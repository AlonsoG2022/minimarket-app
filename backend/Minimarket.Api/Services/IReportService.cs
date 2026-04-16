using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface IReportService
{
    Task<IReadOnlyCollection<SalesSummaryDto>> GetSalesSummaryAsync(DateTime startDate, DateTime endDate);
    Task<IReadOnlyCollection<TopSellingProductDto>> GetTopSellingProductsAsync(DateTime startDate, DateTime endDate, int limit);
    Task<DashboardDto> GetDashboardAsync();
}
