using System.Globalization;
using System.Text;
using Minimarket.PrintWorker.DTOs;
using Minimarket.PrintWorker.Options;

namespace Minimarket.PrintWorker.Services;

public class TicketTextRenderer
{
    public string Build(TicketPrintPayload payload, PrintingOptions options)
    {
        var width = Math.Max(32, options.LineWidth);
        var lines = new List<string>
        {
            Center(options.BusinessName, width),
            Center("TICKET DE VENTA", width),
            new string('-', width),
            $"Venta: #{payload.SaleId}",
            $"Fecha: {payload.SaleDate:dd/MM/yyyy HH:mm}",
            $"Cajero: {payload.CashierName}",
            $"Pago: {payload.PaymentMethod}",
            new string('-', width)
        };

        foreach (var item in payload.Items)
        {
            foreach (var wrapped in Wrap(item.ProductName, width))
            {
                lines.Add(wrapped);
            }

            var detail = $"{item.Quantity} x {FormatMoney(item.UnitPrice)}";
            lines.Add(PadRight(detail, Math.Max(0, width - FormatMoney(item.Subtotal).Length)) + FormatMoney(item.Subtotal));
        }

        lines.Add(new string('-', width));
        lines.Add(PadRight("Items", Math.Max(0, width - payload.Items.Count.ToString(CultureInfo.InvariantCulture).Length)) + payload.Items.Count.ToString(CultureInfo.InvariantCulture));
        var totalUnits = payload.Items.Sum(item => item.Quantity);
        lines.Add(PadRight("Unidades", Math.Max(0, width - totalUnits.ToString(CultureInfo.InvariantCulture).Length)) + totalUnits.ToString(CultureInfo.InvariantCulture));

        if (!string.IsNullOrWhiteSpace(payload.Notes))
        {
            lines.Add(new string('-', width));
            lines.Add("Notas:");
            lines.AddRange(Wrap(payload.Notes, width));
        }

        lines.Add(new string('-', width));
        lines.Add(PadRight("TOTAL", Math.Max(0, width - FormatMoney(payload.Total).Length)) + FormatMoney(payload.Total));

        var builder = new StringBuilder();
        foreach (var line in lines)
        {
            builder.AppendLine(line);
        }

        builder.AppendLine();
        builder.AppendLine(Center("Gracias por su compra", width));
        builder.AppendLine();
        builder.AppendLine();
        return builder.ToString();
    }

    private static string FormatMoney(decimal value) =>
        $"S/ {value:0.00}";

    private static string Center(string value, int width)
    {
        if (value.Length >= width)
        {
            return value;
        }

        var padding = (width - value.Length) / 2;
        return new string(' ', padding) + value;
    }

    private static string PadRight(string value, int width) =>
        value.PadRight(Math.Max(value.Length, width));

    private static IReadOnlyCollection<string> Wrap(string? value, int width)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return Array.Empty<string>();
        }

        var words = value.Split(' ', StringSplitOptions.RemoveEmptyEntries);
        var lines = new List<string>();
        var current = string.Empty;

        foreach (var word in words)
        {
            var candidate = string.IsNullOrEmpty(current) ? word : $"{current} {word}";
            if (candidate.Length <= width)
            {
                current = candidate;
                continue;
            }

            if (!string.IsNullOrEmpty(current))
            {
                lines.Add(current);
            }

            current = word;
        }

        if (!string.IsNullOrEmpty(current))
        {
            lines.Add(current);
        }

        return lines;
    }
}
