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
    string CashierName,
    string PaymentMethod,
    decimal Total,
    string? Notes,
    IReadOnlyCollection<TicketPrintItemDto> Items);
