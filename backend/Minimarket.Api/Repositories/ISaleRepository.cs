using Minimarket.Api.DTOs;
using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public interface ISaleRepository
{
    Task<List<Sale>> GetAllAsync();
    Task<Sale?> GetByIdAsync(int id);
    Task AddAsync(Sale sale);
    Task<int> SaveChangesAsync();
    Task<List<SalesSummaryDto>> GetSalesSummaryAsync(DateTime startDate, DateTime endDate);
    Task<decimal> GetTodaySalesTotalAsync(DateTime dayStart, DateTime dayEnd);
    Task<int> GetTodayTransactionsAsync(DateTime dayStart, DateTime dayEnd);
}
