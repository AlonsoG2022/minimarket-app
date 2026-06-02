using Microsoft.EntityFrameworkCore;
using Minimarket.Api.Data;
using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public class PurchaseRepository(MinimarketDbContext context) : IPurchaseRepository
{
    public Task<List<Purchase>> GetAllAsync() =>
        context.Purchases
            .Include(x => x.Supplier)
            .Include(x => x.User)
            .Include(x => x.Details)
                .ThenInclude(x => x.Product)
            .OrderByDescending(x => x.PurchaseDate)
            .ToListAsync();

    public Task<Purchase?> GetByIdAsync(int id) =>
        context.Purchases
            .Include(x => x.Supplier)
            .Include(x => x.User)
            .Include(x => x.Details)
                .ThenInclude(x => x.Product)
            .FirstOrDefaultAsync(x => x.Id == id);

    public Task AddAsync(Purchase purchase) => context.Purchases.AddAsync(purchase).AsTask();

    public Task<int> SaveChangesAsync() => context.SaveChangesAsync();
}
