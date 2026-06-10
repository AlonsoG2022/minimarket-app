namespace Minimarket.Api.DTOs;

public record PrintJobDto(
    int Id,
    int? SaleId,
    string SourceType,
    string DocumentType,
    string Status,
    int Attempts,
    string? PrinterName,
    DateTime RequestedAt,
    DateTime? StartedAt,
    DateTime? ProcessedAt,
    string? LastError);

public record TicketPrintItemDto(
    string ProductName,
    int Quantity,
    decimal UnitPrice,
    decimal Subtotal);

public record TicketPrintPayloadDto(
    int SaleId,
    DateTime SaleDate,
    string BusinessName,
    string LegalName,
    string TaxId,
    string AddressLine,
    string Phone,
    string Tagline,
    string DocumentTitle,
    string CustomerLabel,
    string CashierName,
    string PaymentMethod,
    decimal SubTotal,
    decimal Igv,
    decimal Total,
    string FooterLine1,
    string FooterLine2,
    string? Notes,
    IReadOnlyCollection<TicketPrintItemDto> Items);
