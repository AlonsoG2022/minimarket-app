using System.Drawing;
using System.Drawing.Printing;
using Minimarket.PrintWorker.DTOs;
using Minimarket.PrintWorker.Options;
using Microsoft.Extensions.Options;

namespace Minimarket.PrintWorker.Services;

public class WindowsPrinterDispatcher(
    TicketTextRenderer renderer,
    IOptions<PrintingOptions> options) : IPrinterDispatcher
{
    public Task<string> PrintTicketAsync(string? printerName, TicketPrintPayload payload, CancellationToken cancellationToken)
    {
        var printingOptions = options.Value;
        var resolvedPrinterName = string.IsNullOrWhiteSpace(printerName)
            ? printingOptions.PrinterName
            : printerName;

        var renderedTicket = renderer.Build(payload, printingOptions);
        var lines = renderedTicket
            .Replace("\r\n", "\n", StringComparison.Ordinal)
            .Split('\n', StringSplitOptions.None);

        using var document = new PrintDocument();
        document.PrintController = new StandardPrintController();

        if (!string.IsNullOrWhiteSpace(resolvedPrinterName))
        {
            document.PrinterSettings.PrinterName = resolvedPrinterName;
        }

        if (!document.PrinterSettings.IsValid)
        {
            throw new InvalidOperationException("No se encontro una impresora valida para el worker de impresion.");
        }

        var currentLine = 0;
        using var font = new Font("Consolas", 9f, FontStyle.Regular, GraphicsUnit.Point);
        using var brush = new SolidBrush(Color.Black);
        var lineHeight = font.GetHeight() + 2;

        document.PrintPage += (_, eventArgs) =>
        {
            var y = eventArgs.MarginBounds.Top;

            while (currentLine < lines.Length)
            {
                cancellationToken.ThrowIfCancellationRequested();
                eventArgs.Graphics.DrawString(lines[currentLine], font, brush, eventArgs.MarginBounds.Left, y);
                currentLine++;
                y += (int)lineHeight;

                if (y + lineHeight > eventArgs.MarginBounds.Bottom)
                {
                    eventArgs.HasMorePages = true;
                    return;
                }
            }

            eventArgs.HasMorePages = false;
        };

        document.Print();
        return Task.FromResult(document.PrinterSettings.PrinterName);
    }
}
