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

        // Los datos vienen del snapshot (igual que la vista previa). Si un ticket viejo no los trae,
        // se usan como respaldo las opciones locales del worker.
        var businessName = Fallback(payload.BusinessName, options.BusinessName);
        var legalName = Fallback(payload.LegalName, options.LegalName);
        var taxId = Fallback(payload.TaxId, options.TaxId);
        var addressLine = Fallback(payload.AddressLine, options.AddressLine);
        var phone = Fallback(payload.Phone, options.Phone);
        var tagline = payload.Tagline?.Trim() ?? string.Empty;
        var documentTitle = Fallback(payload.DocumentTitle, "Ticket de venta");
        var customerLabel = Fallback(payload.CustomerLabel, options.CustomerLabel);
        var footerLine1 = Fallback(payload.FooterLine1, options.FooterLine1);
        var footerLine2 = Fallback(payload.FooterLine2, options.FooterLine2);

        // Totales del snapshot; si un ticket viejo no trae el desglose, se deriva del total (IGV 18% incluido).
        var total = payload.Total;
        var subTotal = payload.SubTotal > 0 ? payload.SubTotal : decimal.Round(total / 1.18m, 2, MidpointRounding.AwayFromZero);
        var igv = payload.Igv > 0 ? payload.Igv : decimal.Round(total - subTotal, 2, MidpointRounding.AwayFromZero);

        var lines = new List<string> { Center(businessName, width) };
        if (!string.IsNullOrWhiteSpace(tagline))
        {
            lines.Add(Center(tagline, width));
        }

        lines.Add(new string('-', width));
        lines.Add(Center(legalName, width));
        lines.Add(Center($"RUC: {taxId}", width));
        lines.Add(Center(addressLine, width));
        lines.Add(Center($"Telefono: {phone}", width));
        lines.Add(new string('-', width));
        lines.Add(Center(documentTitle.ToUpperInvariant(), width));
        lines.Add(Center($"#{payload.SaleId} - {payload.SaleDate:dd/MM/yyyy HH:mm}", width));
        lines.Add(new string('-', width));
        lines.Add($"Fecha: {payload.SaleDate:dd/MM/yyyy HH:mm}");
        lines.Add($"Cajero: {payload.CashierName}");
        lines.Add($"Cliente: {customerLabel}");
        lines.Add($"Pago: {payload.PaymentMethod}");
        lines.Add(new string('-', width));
        lines.Add(TicketHeader(width));
        lines.Add(new string('-', width));

        foreach (var item in payload.Items)
        {
            foreach (var wrapped in Wrap(item.ProductName, width - 2))
            {
                lines.Add(wrapped);
            }

            var detail = $"{item.Quantity} x {FormatMoney(item.UnitPrice)}";
            lines.Add(PadRight(detail, Math.Max(0, width - FormatMoney(item.Subtotal).Length)) + FormatMoney(item.Subtotal));
        }

        lines.Add(new string('-', width));
        lines.Add(Row("Items", payload.Items.Count.ToString(CultureInfo.InvariantCulture), width));
        var totalUnits = payload.Items.Sum(item => item.Quantity);
        lines.Add(Row("Unidades", totalUnits.ToString(CultureInfo.InvariantCulture), width));
        lines.Add(Row("Subtotal", FormatMoney(subTotal), width));
        lines.Add(Row("IGV (18%)", FormatMoney(igv), width));

        if (!string.IsNullOrWhiteSpace(payload.Notes))
        {
            lines.Add(new string('-', width));
            lines.Add("Notas:");
            lines.AddRange(Wrap(payload.Notes, width));
        }

        lines.Add(new string('-', width));
        lines.Add(Row("TOTAL", FormatMoney(total), width));

        var builder = new StringBuilder();
        foreach (var line in lines)
        {
            builder.AppendLine(line);
        }

        builder.AppendLine();
        builder.AppendLine(Center(footerLine1, width));
        builder.AppendLine(Center(footerLine2, width));
        builder.AppendLine();
        builder.AppendLine();
        return builder.ToString();
    }

    private static string Fallback(string? value, string fallback) =>
        string.IsNullOrWhiteSpace(value) ? fallback : value.Trim();

    private static string Row(string label, string value, int width) =>
        PadRight(label, Math.Max(0, width - value.Length)) + value;

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

    private static string TicketHeader(int width)
    {
        const string header = "CANT PRODUCTO P.UNIT IMPORTE";
        return header.Length <= width ? header : header[..width];
    }
}
