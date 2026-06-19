namespace Minimarket.Api.DTOs;

public record CreateSaleDetailDto(int ProductId, int Quantity);

public record CreateSaleDto(int UserId, int? CashSessionId, string PaymentMethod, string? Notes, List<CreateSaleDetailDto> Details);

public record SaleDetailDto(
    int Id,
    int ProductId,
    string ProductName,
    string ProductShortName,
    int Quantity,
    decimal UnitPrice,
    decimal Subtotal);

public record SaleDto(
    int Id,
    DateTime SaleDate,
    int UserId,
    string UserName,
    int? CashSessionId,
    string? PrintStatus,
    int? LastPrintJobId,
    string PaymentMethod,
    decimal SubTotal,
    decimal Igv,
    decimal Total,
    string? Notes,
    IReadOnlyCollection<SaleDetailDto> Details);

public record SalesSummaryDto(DateOnly Date, decimal TotalAmount, int SaleCount);

public record TopSellingProductDto(
    int ProductId,
    string ProductName,
    string Sku,
    int TotalQuantity,
    decimal TotalAmount);

public record DashboardDto(decimal TodaySales, int TodayTransactions, int ProductCount, int LowStockProducts);
