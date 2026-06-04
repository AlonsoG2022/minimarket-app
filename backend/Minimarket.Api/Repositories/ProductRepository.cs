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

    public Task<Product?> GetByBarcodeAsync(string barcode) =>
        context.Products
            .Include(x => x.Category)
            .FirstOrDefaultAsync(x => x.Barcode == barcode);

    public Task<Product?> GetByPurchaseBarcodeAsync(string barcode) =>
        context.Products
            .Include(x => x.Category)
            .FirstOrDefaultAsync(x => x.PurchaseBarcode == barcode);

    public Task<bool> ExistsBySkuAsync(string sku, int? excludingId = null) =>
        context.Products.AnyAsync(x => x.Sku == sku && (!excludingId.HasValue || x.Id != excludingId.Value));

    public Task<bool> ExistsByBarcodeAsync(string barcode, int? excludingId = null) =>
        context.Products.AnyAsync(x => x.Barcode == barcode && (!excludingId.HasValue || x.Id != excludingId.Value));

    public Task<bool> ExistsByPurchaseBarcodeAsync(string barcode, int? excludingId = null) =>
        context.Products.AnyAsync(x => x.PurchaseBarcode == barcode && (!excludingId.HasValue || x.Id != excludingId.Value));

    public Task<bool> HasSaleDetailsAsync(int productId) =>
        context.SaleDetails.AnyAsync(x => x.ProductId == productId);

    public Task<bool> HasPurchaseDetailsAsync(int productId) =>
        context.PurchaseDetails.AnyAsync(x => x.ProductId == productId);

    public Task<List<string>> GetSkusByPrefixAsync(string prefix) =>
        context.Products
            .AsNoTracking()
            .Where(x => x.Sku.StartsWith(prefix))
            .Select(x => x.Sku)
            .ToListAsync();

    public Task AddAsync(Product product) => context.Products.AddAsync(product).AsTask();

    public void Update(Product product) => context.Products.Update(product);

    public void Remove(Product product) => context.Products.Remove(product);

    public Task<int> SaveChangesAsync() => context.SaveChangesAsync();

    public Task<int> CountAsync() => context.Products.CountAsync();

    public Task<int> CountLowStockAsync() => context.Products.CountAsync(x => x.Stock <= x.MinimumStock);
}
