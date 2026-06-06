namespace Minimarket.PrintWorker.DTOs;

public record TicketPrintItem(
    string ProductName,
    int Quantity,
    decimal UnitPrice,
    decimal Subtotal);

public record TicketPrintPayload(
    int SaleId,
    DateTime SaleDate,
    string CashierName,
    string PaymentMethod,
    decimal Total,
    string? Notes,
    IReadOnlyCollection<TicketPrintItem> Items);
