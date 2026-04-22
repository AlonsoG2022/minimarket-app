using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public interface IProductRepository
{
    Task<List<Product>> GetAllAsync();
    Task<Product?> GetByIdAsync(int id);
    Task<bool> ExistsBySkuAsync(string sku, int? excludingId = null);
    Task<List<string>> GetSkusByPrefixAsync(string prefix);
    Task AddAsync(Product product);
    void Update(Product product);
    void Remove(Product product);
    Task<int> SaveChangesAsync();
    Task<int> CountAsync();
    Task<int> CountLowStockAsync();
}
