using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public interface IProductRepository
{
    Task<List<Product>> GetAllAsync();
    Task<Product?> GetByIdAsync(int id);
    Task<Product?> GetByBarcodeAsync(string barcode);
    Task<Product?> GetByPurchaseBarcodeAsync(string barcode);
    Task<bool> ExistsBySkuAsync(string sku, int? excludingId = null);
    Task<bool> ExistsByBarcodeAsync(string barcode, int? excludingId = null);
    Task<bool> ExistsByPurchaseBarcodeAsync(string barcode, int? excludingId = null);
    Task<bool> HasSaleDetailsAsync(int productId);
    Task<bool> HasPurchaseDetailsAsync(int productId);
    Task<List<string>> GetSkusByPrefixAsync(string prefix);
    Task AddAsync(Product product);
    void Update(Product product);
    void Remove(Product product);
    Task<int> SaveChangesAsync();
    Task<int> CountAsync();
    Task<int> CountLowStockAsync();
}
