namespace Minimarket.PrintWorker.DTOs;

public record TicketPrintItem(
    string ProductName,
    int Quantity,
    decimal UnitPrice,
    decimal Subtotal);

public record TicketPrintPayload(
    int SaleId,
    DateTime SaleDate,
    string? BusinessName,
    string? LegalName,
    string? TaxId,
    string? AddressLine,
    string? Phone,
    string? Tagline,
    string? DocumentTitle,
    string? CustomerLabel,
    string CashierName,
    string PaymentMethod,
    decimal SubTotal,
    decimal Igv,
    decimal Total,
    string? FooterLine1,
    string? FooterLine2,
    string? Notes,
    IReadOnlyCollection<TicketPrintItem> Items);
