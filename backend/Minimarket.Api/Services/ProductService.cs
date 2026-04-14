using Minimarket.Api.DTOs;
using Minimarket.Api.Mapping;
using Minimarket.Api.Models;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

public class ProductService(IProductRepository productRepository, ICategoryRepository categoryRepository) : IProductService
{
    public async Task<IReadOnlyCollection<ProductDto>> GetAllAsync() =>
        (await productRepository.GetAllAsync()).Select(x => x.ToDto()).ToList();

    public async Task<ProductDto?> GetByIdAsync(int id) =>
        (await productRepository.GetByIdAsync(id))?.ToDto();

    public async Task<(bool Success, string? Error, ProductDto? Product)> CreateAsync(SaveProductDto dto)
    {
        if (dto.Stock < 0 || dto.MinimumStock < 0 || dto.Price <= 0)
        {
            return (false, "Los datos del producto no son validos.", null);
        }

        if (await categoryRepository.GetByIdAsync(dto.CategoryId) is null)
        {
            return (false, "La categoria seleccionada no existe.", null);
        }

        if (await productRepository.ExistsBySkuAsync(dto.Sku.Trim().ToUpperInvariant()))
        {
            return (false, "Ya existe un producto con el mismo SKU.", null);
        }

        var product = new Product
        {
            Name = dto.Name.Trim(),
            Sku = dto.Sku.Trim().ToUpperInvariant(),
            Description = dto.Description?.Trim(),
            Price = dto.Price,
            Stock = dto.Stock,
            MinimumStock = dto.MinimumStock,
            IsActive = dto.IsActive,
            CategoryId = dto.CategoryId
        };

        await productRepository.AddAsync(product);
        await productRepository.SaveChangesAsync();

        var created = await productRepository.GetByIdAsync(product.Id);
        return (true, null, created?.ToDto());
    }

    public async Task<(bool Success, string? Error, ProductDto? Product)> UpdateAsync(int id, SaveProductDto dto)
    {
        var product = await productRepository.GetByIdAsync(id);
        if (product is null)
        {
            return (false, "Producto no encontrado.", null);
        }

        if (await categoryRepository.GetByIdAsync(dto.CategoryId) is null)
        {
            return (false, "La categoria seleccionada no existe.", null);
        }

        if (await productRepository.ExistsBySkuAsync(dto.Sku.Trim().ToUpperInvariant(), id))
        {
            return (false, "Ya existe un producto con el mismo SKU.", null);
        }

        product.Name = dto.Name.Trim();
        product.Sku = dto.Sku.Trim().ToUpperInvariant();
        product.Description = dto.Description?.Trim();
        product.Price = dto.Price;
        product.Stock = dto.Stock;
        product.MinimumStock = dto.MinimumStock;
        product.IsActive = dto.IsActive;
        product.CategoryId = dto.CategoryId;

        productRepository.Update(product);
        await productRepository.SaveChangesAsync();

        var updated = await productRepository.GetByIdAsync(product.Id);
        return (true, null, updated?.ToDto());
    }

    public async Task<(bool Success, string? Error)> DeleteAsync(int id)
    {
        var product = await productRepository.GetByIdAsync(id);
        if (product is null)
        {
            return (false, "Producto no encontrado.");
        }

        productRepository.Remove(product);
        await productRepository.SaveChangesAsync();
        return (true, null);
    }
}
