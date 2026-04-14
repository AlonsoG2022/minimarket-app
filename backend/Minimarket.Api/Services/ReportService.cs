using Minimarket.Api.DTOs;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

public class ReportService(ISaleRepository saleRepository, IProductRepository productRepository) : IReportService
{
    public async Task<IReadOnlyCollection<SalesSummaryDto>> GetSalesSummaryAsync(DateTime startDate, DateTime endDate) =>
        await saleRepository.GetSalesSummaryAsync(startDate, endDate);

    public async Task<DashboardDto> GetDashboardAsync()
    {
        var now = DateTime.Now;
        var dayStart = now.Date;
        var dayEnd = dayStart.AddDays(1).AddTicks(-1);

        return new DashboardDto(
            await saleRepository.GetTodaySalesTotalAsync(dayStart, dayEnd),
            await saleRepository.GetTodayTransactionsAsync(dayStart, dayEnd),
            await productRepository.CountAsync(),
            await productRepository.CountLowStockAsync());
    }
}
