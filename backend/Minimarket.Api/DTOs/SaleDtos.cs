namespace Minimarket.Api.DTOs;

public record CreateSaleDetailDto(int ProductId, int Quantity);

public record CreateSaleDto(int UserId, string PaymentMethod, string? Notes, List<CreateSaleDetailDto> Details);

public record SaleDetailDto(
    int Id,
    int ProductId,
    string ProductName,
    int Quantity,
    decimal UnitPrice,
    decimal Subtotal);

public record SaleDto(
    int Id,
    DateTime SaleDate,
    int UserId,
    string UserName,
    string PaymentMethod,
    decimal Total,
    string? Notes,
    IReadOnlyCollection<SaleDetailDto> Details);

public record SalesSummaryDto(DateOnly Date, decimal TotalAmount, int SaleCount);

public record DashboardDto(decimal TodaySales, int TodayTransactions, int ProductCount, int LowStockProducts);
