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
        var rows = await context.Sales
            .AsNoTracking()
            .Where(x => x.SaleDate >= startDate && x.SaleDate <= endDate)
            .GroupBy(x => x.SaleDate.Date)
            .Select(group => new
            {
                Date = group.Key,
                TotalAmount = group.Sum(x => x.Total),
                SaleCount = group.Count()
            })
            .OrderBy(x => x.Date)
            .ToListAsync();

        return rows
            .Select(row => new SalesSummaryDto(
                DateOnly.FromDateTime(row.Date),
                row.TotalAmount,
                row.SaleCount))
            .ToList();
    }

    public Task<List<TopSellingProductDto>> GetTopSellingProductsAsync(DateTime startDate, DateTime endDate, int limit) =>
        context.SaleDetails
            .AsNoTracking()
            .Where(x => x.Sale != null && x.Sale.SaleDate >= startDate && x.Sale.SaleDate <= endDate)
            .GroupBy(x => new
            {
                x.ProductId,
                ProductName = x.Product != null ? x.Product.Name : string.Empty,
                Sku = x.Product != null ? x.Product.Sku : string.Empty
            })
            .Select(group => new TopSellingProductDto(
                group.Key.ProductId,
                group.Key.ProductName,
                group.Key.Sku,
                group.Sum(x => x.Quantity),
                group.Sum(x => x.Subtotal)))
            .OrderByDescending(x => x.TotalQuantity)
            .ThenByDescending(x => x.TotalAmount)
            .ThenBy(x => x.ProductName)
            .Take(limit)
            .ToListAsync();

    public async Task<decimal> GetTodaySalesTotalAsync(DateTime dayStart, DateTime dayEnd)
    {
        return await context.Sales
            .Where(x => x.SaleDate >= dayStart && x.SaleDate <= dayEnd)
            .SumAsync(x => (decimal?)x.Total) ?? 0m;
    }

    public Task<int> GetTodayTransactionsAsync(DateTime dayStart, DateTime dayEnd) =>
        context.Sales.CountAsync(x => x.SaleDate >= dayStart && x.SaleDate <= dayEnd);
}
