using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface IReportService
{
    Task<IReadOnlyCollection<SalesSummaryDto>> GetSalesSummaryAsync(DateTime startDate, DateTime endDate);
    Task<DashboardDto> GetDashboardAsync();
}
