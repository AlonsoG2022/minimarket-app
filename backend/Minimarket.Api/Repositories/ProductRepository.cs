using Microsoft.EntityFrameworkCore;
using Minimarket.Api.Data;
using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public class ProductRepository(MinimarketDbContext context) : IProductRepository
{
    public Task<List<Product>> GetAllAsync() =>
        context.Products
            .AsNoTracking()
            .Include(x => x.Category)
            .OrderBy(x => x.Name)
            .ToListAsync();

    public Task<Product?> GetByIdAsync(int id) =>
        context.Products
            .Include(x => x.Category)
            .FirstOrDefaultAsync(x => x.Id == id);

    public Task<bool> ExistsBySkuAsync(string sku, int? excludingId = null) =>
        context.Products.AnyAsync(x => x.Sku == sku && (!excludingId.HasValue || x.Id != excludingId.Value));

    public Task AddAsync(Product product) => context.Products.AddAsync(product).AsTask();

    public void Update(Product product) => context.Products.Update(product);

    public void Remove(Product product) => context.Products.Remove(product);

    public Task<int> SaveChangesAsync() => context.SaveChangesAsync();

    public Task<int> CountAsync() => context.Products.CountAsync();

    public Task<int> CountLowStockAsync() => context.Products.CountAsync(x => x.Stock <= x.MinimumStock);
}
