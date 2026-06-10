using Minimarket.Api.DTOs;
using Minimarket.Api.Mapping;
using Minimarket.Api.Models;
using Minimarket.Api.Repositories;
using System.Globalization;
using System.Text;

namespace Minimarket.Api.Services;

public class ProductService(IProductRepository productRepository, ICategoryRepository categoryRepository, ICompanyRepository companyRepository) : IProductService
{
    private const int DefaultMinimumStock = 5;

    private async Task<int> GetConfiguredMinimumStockAsync()
    {
        var company = await companyRepository.GetAsync();
        return company?.MinimumStock ?? DefaultMinimumStock;
    }

    public async Task<IReadOnlyCollection<ProductDto>> GetAllAsync() =>
        (await productRepository.GetAllAsync()).Select(x => x.ToDto()).ToList();

    public async Task<ProductDto?> GetByIdAsync(int id) =>
        (await productRepository.GetByIdAsync(id))?.ToDto();

    public async Task<(bool Success, string? Error, ProductDto? Product)> CreateAsync(SaveProductDto dto)
    {
        if (dto.Stock < 0 || dto.Price <= 0 || dto.UnitsPerPurchaseUnit <= 0)
        {
            return (false, "Los datos del producto no son validos.", null);
        }

        if (!TryResolveBarcode(dto.Barcode, dto.PurchaseBarcode, out var unifiedBarcode, out var barcodeError))
        {
            return (false, barcodeError, null);
        }

        if (!TryParseExpirationDate(dto.ExpirationDate, out var expirationDate, out var expirationError))
        {
            return (false, expirationError, null);
        }

        var validationError = await ValidateBarcodesAsync(dto, null);
        if (validationError is not null)
        {
            return (false, validationError, null);
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
            Barcode = unifiedBarcode,
            PurchaseBarcode = unifiedBarcode,
            Description = dto.Description?.Trim(),
            Price = dto.Price,
            Cost = 0m,
            Stock = dto.Stock,
            MinimumStock = await GetConfiguredMinimumStockAsync(),
            ExpirationDate = expirationDate,
            SalesUnitName = NormalizeUnitName(dto.SalesUnitName),
            PurchaseUnitName = NormalizeUnitName(dto.PurchaseUnitName),
            UnitsPerPurchaseUnit = dto.UnitsPerPurchaseUnit,
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

        if (dto.Stock < 0 || dto.Price <= 0 || dto.UnitsPerPurchaseUnit <= 0)
        {
            return (false, "Los datos del producto no son validos.", null);
        }

        if (!TryResolveBarcode(dto.Barcode, dto.PurchaseBarcode, out var unifiedBarcode, out var barcodeError))
        {
            return (false, barcodeError, null);
        }

        if (!TryParseExpirationDate(dto.ExpirationDate, out var expirationDate, out var expirationError))
        {
            return (false, expirationError, null);
        }

        var validationError = await ValidateBarcodesAsync(dto, id);
        if (validationError is not null)
        {
            return (false, validationError, null);
        }

        if (await categoryRepository.GetByIdAsync(dto.CategoryId) is null)
        {
            return (false, "La categoria seleccionada no existe.", null);
        }

        product.Name = dto.Name.Trim();
        product.Barcode = unifiedBarcode;
        product.PurchaseBarcode = unifiedBarcode;
        product.Description = dto.Description?.Trim();
        product.Price = dto.Price;
        product.Stock = dto.Stock;
        product.MinimumStock = await GetConfiguredMinimumStockAsync();
        product.ExpirationDate = expirationDate;
        product.SalesUnitName = NormalizeUnitName(dto.SalesUnitName);
        product.PurchaseUnitName = NormalizeUnitName(dto.PurchaseUnitName);
        product.UnitsPerPurchaseUnit = dto.UnitsPerPurchaseUnit;
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
        var minimumStock = await GetConfiguredMinimumStockAsync();

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

            if (row.UnitsPerPurchaseUnit is <= 0)
            {
                errors.Add(new ProductImportErrorDto(row.RowNumber, "Las unidades por compra deben ser mayores que cero."));
                continue;
            }

            var category = await categoryRepository.GetByNameAsync(categoryName);
            if (category is null)
            {
                errors.Add(new ProductImportErrorDto(row.RowNumber, $"La categoria '{categoryName}' no existe."));
                continue;
            }

            if (!TryResolveBarcode(row.Barcode, row.Barcode, out var unifiedBarcode, out var barcodeError))
            {
                errors.Add(new ProductImportErrorDto(row.RowNumber, barcodeError!));
                continue;
            }

            if (unifiedBarcode is not null && await BarcodeExistsForAnotherProductAsync(unifiedBarcode))
            {
                errors.Add(new ProductImportErrorDto(row.RowNumber, $"El codigo de barras '{unifiedBarcode}' ya existe."));
                continue;
            }

            if (!TryParseExpirationDate(row.ExpirationDate, out var expirationDate, out var expirationError))
            {
                errors.Add(new ProductImportErrorDto(row.RowNumber, expirationError!));
                continue;
            }

            var product = new Product
            {
                Name = name,
                Sku = await GenerateSkuAsync(category.Name),
                Barcode = unifiedBarcode,
                PurchaseBarcode = unifiedBarcode,
                Description = NormalizeOptional(row.Description),
                Price = row.Price,
                Cost = 0m,
                Stock = row.Stock,
                MinimumStock = minimumStock,
                ExpirationDate = expirationDate,
                SalesUnitName = NormalizeUnitName(row.SalesUnitName),
                PurchaseUnitName = NormalizeUnitName(row.PurchaseUnitName),
                UnitsPerPurchaseUnit = row.UnitsPerPurchaseUnit ?? 1,
                IsActive = row.IsActive ?? true,
                CategoryId = category.Id
            };

            await productRepository.AddAsync(product);
            await productRepository.SaveChangesAsync();
            createdCount++;
        }

        return new ProductImportResultDto(createdCount, errors);
    }

    public async Task<(bool Success, string? Error, DeleteProductResultDto? Result)> DeleteAsync(int id)
    {
        var product = await productRepository.GetByIdAsync(id);
        if (product is null)
        {
            return (false, "Producto no encontrado.", null);
        }

        var hasMovements = await productRepository.HasSaleDetailsAsync(id) || await productRepository.HasPurchaseDetailsAsync(id);
        if (hasMovements)
        {
            product.IsActive = false;
            productRepository.Update(product);
            await productRepository.SaveChangesAsync();
            return (true, null, new DeleteProductResultDto("El producto tiene compras o ventas registradas. Se desactivo en lugar de eliminarlo.", true));
        }

        productRepository.Remove(product);
        await productRepository.SaveChangesAsync();
        return (true, null, new DeleteProductResultDto("Producto eliminado correctamente.", false));
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

    private async Task<string?> ValidateBarcodesAsync(SaveProductDto dto, int? excludingId)
    {
        if (!TryResolveBarcode(dto.Barcode, dto.PurchaseBarcode, out var barcode, out var barcodeError))
        {
            return barcodeError;
        }

        if (barcode is not null && await productRepository.ExistsByBarcodeAsync(barcode, excludingId))
        {
            return "El codigo de barras ya existe.";
        }

        if (barcode is not null && await productRepository.ExistsByPurchaseBarcodeAsync(barcode, excludingId))
        {
            return "El codigo de barras ya existe.";
        }

        return null;
    }

    private async Task<bool> BarcodeExistsForAnotherProductAsync(string barcode) =>
        await productRepository.ExistsByBarcodeAsync(barcode) || await productRepository.ExistsByPurchaseBarcodeAsync(barcode);

    private static bool TryResolveBarcode(string? barcodeValue, string? purchaseBarcodeValue, out string? unifiedBarcode, out string? error)
    {
        var barcode = NormalizeOptional(barcodeValue);
        var purchaseBarcode = NormalizeOptional(purchaseBarcodeValue);

        if (barcode is not null && purchaseBarcode is not null && !string.Equals(barcode, purchaseBarcode, StringComparison.OrdinalIgnoreCase))
        {
            unifiedBarcode = null;
            error = "Usa un unico codigo de barras para compras y ventas.";
            return false;
        }

        unifiedBarcode = barcode ?? purchaseBarcode;
        error = null;
        return true;
    }

    private static bool TryParseExpirationDate(string? rawValue, out DateOnly? expirationDate, out string? error)
    {
        var value = NormalizeOptional(rawValue);
        if (value is null)
        {
            expirationDate = null;
            error = null;
            return true;
        }

        if (DateOnly.TryParseExact(value, ["yyyy-MM-dd", "dd/MM/yyyy", "d/M/yyyy"], CultureInfo.InvariantCulture, DateTimeStyles.None, out var parsed))
        {
            expirationDate = parsed;
            error = null;
            return true;
        }

        expirationDate = null;
        error = "La fecha de caducidad no tiene un formato valido.";
        return false;
    }

    private static string? NormalizeOptional(string? value)
    {
        var trimmed = value?.Trim();
        return string.IsNullOrWhiteSpace(trimmed) ? null : trimmed;
    }

    private static string NormalizeUnitName(string? value)
    {
        var trimmed = value?.Trim();
        return string.IsNullOrWhiteSpace(trimmed) ? "unidad" : trimmed;
    }
}
