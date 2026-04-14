using Microsoft.EntityFrameworkCore;
using Minimarket.Api.Data;
using Minimarket.Api.DTOs;
using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public class SaleRepository(MinimarketDbContext context) : ISaleRepository
{
    public Task<List<Sale>> GetAllAsync() =>
        context.Sales
            .Include(x => x.User)
            .Include(x => x.Details)
                .ThenInclude(x => x.Product)
            .OrderByDescending(x => x.SaleDate)
            .ToListAsync();

    public Task<Sale?> GetByIdAsync(int id) =>
        context.Sales
            .Include(x => x.User)
            .Include(x => x.Details)
                .ThenInclude(x => x.Product)
            .FirstOrDefaultAsync(x => x.Id == id);

    public Task AddAsync(Sale sale) => context.Sales.AddAsync(sale).AsTask();

    public Task<int> SaveChangesAsync() => context.SaveChangesAsync();

    public async Task<List<SalesSummaryDto>> GetSalesSummaryAsync(DateTime startDate, DateTime endDate)
    {
        return await context.Sales
            .Where(x => x.SaleDate >= startDate && x.SaleDate <= endDate)
            .GroupBy(x => x.SaleDate.Date)
            .Select(group => new SalesSummaryDto(
                DateOnly.FromDateTime(group.Key),
                group.Sum(x => x.Total),
                group.Count()))
            .OrderBy(x => x.Date)
            .ToListAsync();
    }

    public async Task<decimal> GetTodaySalesTotalAsync(DateTime dayStart, DateTime dayEnd)
    {
        return await context.Sales
            .Where(x => x.SaleDate >= dayStart && x.SaleDate <= dayEnd)
            .SumAsync(x => (decimal?)x.Total) ?? 0m;
    }

    public Task<int> GetTodayTransactionsAsync(DateTime dayStart, DateTime dayEnd) =>
        context.Sales.CountAsync(x => x.SaleDate >= dayStart && x.SaleDate <= dayEnd);
}
