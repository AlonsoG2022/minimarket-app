using Minimarket.Api.DTOs;
using Minimarket.Api.Mapping;
using Minimarket.Api.Models;
using Minimarket.Api.Repositories;
using System.Globalization;
using System.Text;

namespace Minimarket.Api.Services;

public class ProductService(IProductRepository productRepository, ICategoryRepository categoryRepository) : IProductService
{
    private const int FixedMinimumStock = 5;

    public async Task<IReadOnlyCollection<ProductDto>> GetAllAsync() =>
        (await productRepository.GetAllAsync()).Select(x => x.ToDto()).ToList();

    public async Task<ProductDto?> GetByIdAsync(int id) =>
        (await productRepository.GetByIdAsync(id))?.ToDto();

    public async Task<(bool Success, string? Error, ProductDto? Product)> CreateAsync(SaveProductDto dto)
    {
        if (dto.Stock < 0 || dto.Price <= 0)
        {
            return (false, "Los datos del producto no son validos.", null);
        }

        var category = await categoryRepository.GetByIdAsync(dto.CategoryId);
        if (category is null)
        {
            return (false, "La categoria seleccionada no existe.", null);
        }

        var product = new Product
        {
            Name = dto.Name.Trim(),
            Sku = await GenerateSkuAsync(category.Name),
            Description = dto.Description?.Trim(),
            Price = dto.Price,
            Stock = dto.Stock,
            MinimumStock = FixedMinimumStock,
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

        product.Name = dto.Name.Trim();
        product.Description = dto.Description?.Trim();
        product.Price = dto.Price;
        product.Stock = dto.Stock;
        product.MinimumStock = FixedMinimumStock;
        product.IsActive = dto.IsActive;
        product.CategoryId = dto.CategoryId;

        productRepository.Update(product);
        await productRepository.SaveChangesAsync();

        var updated = await productRepository.GetByIdAsync(product.Id);
        return (true, null, updated?.ToDto());
    }

    public async Task<ProductImportResultDto> ImportAsync(IReadOnlyCollection<ProductImportRowDto> rows)
    {
        if (rows.Count == 0)
        {
            return new ProductImportResultDto(0, [new ProductImportErrorDto(0, "El archivo no contiene filas validas.")]);
        }

        var errors = new List<ProductImportErrorDto>();
        var createdCount = 0;

        foreach (var row in rows)
        {
            var name = row.Name?.Trim() ?? string.Empty;
            var categoryName = row.CategoryName?.Trim() ?? string.Empty;

            if (string.IsNullOrWhiteSpace(name))
            {
                errors.Add(new ProductImportErrorDto(row.RowNumber, "El nombre del producto es obligatorio."));
                continue;
            }

            if (row.Price <= 0)
            {
                errors.Add(new ProductImportErrorDto(row.RowNumber, "El precio debe ser mayor que cero."));
                continue;
            }

            if (string.IsNullOrWhiteSpace(categoryName))
            {
                errors.Add(new ProductImportErrorDto(row.RowNumber, "La categoria es obligatoria."));
                continue;
            }

            if (row.Stock < 0)
            {
                errors.Add(new ProductImportErrorDto(row.RowNumber, "El stock no puede ser menor que cero."));
                continue;
            }

            var category = await categoryRepository.GetByNameAsync(categoryName);
            if (category is null)
            {
                errors.Add(new ProductImportErrorDto(row.RowNumber, $"La categoria '{categoryName}' no existe."));
                continue;
            }

            var product = new Product
            {
                Name = name,
                Sku = await GenerateSkuAsync(category.Name),
                Description = null,
                Price = row.Price,
                Stock = row.Stock,
                MinimumStock = FixedMinimumStock,
                IsActive = true,
                CategoryId = category.Id
            };

            await productRepository.AddAsync(product);
            await productRepository.SaveChangesAsync();
            createdCount++;
        }

        return new ProductImportResultDto(createdCount, errors);
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

    private async Task<string> GenerateSkuAsync(string categoryName)
    {
        var prefix = BuildCategoryPrefix(categoryName);
        var existingSkus = await productRepository.GetSkusByPrefixAsync($"{prefix}-");
        var nextNumber = existingSkus
            .Select(ParseSkuSequence)
            .DefaultIfEmpty(0)
            .Max() + 1;

        return $"{prefix}-{nextNumber:000000}";
    }

    private static int ParseSkuSequence(string sku)
    {
        var parts = sku.Split('-', 2);
        if (parts.Length != 2)
        {
            return 0;
        }

        return int.TryParse(parts[1], out var value) ? value : 0;
    }

    private static string BuildCategoryPrefix(string categoryName)
    {
        var normalized = categoryName.Normalize(NormalizationForm.FormD);
        var builder = new StringBuilder();

        foreach (var character in normalized)
        {
            if (CharUnicodeInfo.GetUnicodeCategory(character) == UnicodeCategory.NonSpacingMark)
            {
                continue;
            }

            if (char.IsLetterOrDigit(character))
            {
                builder.Append(char.ToUpperInvariant(character));
            }

            if (builder.Length == 3)
            {
                break;
            }
        }

        if (builder.Length == 0)
        {
            builder.Append("CAT");
        }

        while (builder.Length < 3)
        {
            builder.Append('X');
        }

        return builder.ToString();
    }
}
