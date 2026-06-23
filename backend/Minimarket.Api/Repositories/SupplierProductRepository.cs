using Minimarket.Api.Data;
using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public class SupplierProductRepository(MinimarketDbContext context) : ISupplierProductRepository
{
    public Task AddAsync(SupplierProduct entry) => context.SupplierProducts.AddAsync(entry).AsTask();

    public Task<int> SaveChangesAsync() => context.SaveChangesAsync();
}
