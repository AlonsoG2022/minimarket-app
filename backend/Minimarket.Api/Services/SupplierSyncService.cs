using System.Globalization;
using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;
using Minimarket.Api.DTOs;
using Minimarket.Api.Models;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

// Sincroniza el catalogo del proveedor (Coca-Cola / AIC Digital - Arca Continental) hacia Productos.
// Reglas definidas en docs/PROJECT_CONTEXT.md (seccion de sincronizacion de catalogo de proveedor).
//
// IMPORTANTE: los nombres de las propiedades del JSON del proveedor (sku, longDescription, etc.)
// estan tomados de la investigacion documentada. Si la respuesta real difiere, ajustar las
// listas de candidatos en GetString/GetDecimal/GetInt y en FindArray (el parseo es tolerante).
public class SupplierSyncService(
    IHttpClientFactory httpClientFactory,
    IProductRepository productRepository,
    ICategoryRepository categoryRepository,
    ISupplierRepository supplierRepository,
    ISupplierProductRepository supplierProductRepository,
    ICompanyRepository companyRepository) : ISupplierSyncService
{
    private const int DefaultMinimumStock = 5;
    private const string CustomerId = "2898397";

    private const string CategoriesUrl =
        "https://briolightapimgmt.arcacontal.com/product/api/v1/Category/GetHomePageCategories?customerId=" + CustomerId;

    private const string PortfolioUrlFormat =
        "https://briolightapimgmt.arcacontal.com/product/api/v1/Portfolio?businessUnitId=4&CategoryId={0}" +
        "&Phrase=&Type=4&Limit=500&Offset=1&CustomerId=" + CustomerId + "&order=0&searchCriteria=";

    public async Task<(bool Success, string? Error, SupplierSyncResultDto? Result)> SyncAsync(SupplierSyncRequestDto request)
    {
        if (string.IsNullOrWhiteSpace(request.Token))
        {
            return (false, "Debes pegar el token del proveedor.", null);
        }

        if (string.IsNullOrWhiteSpace(request.SupplierDocumentNumber))
        {
            return (false, "Debes seleccionar el proveedor.", null);
        }

        var supplier = await supplierRepository.GetByDocumentNumberAsync(request.SupplierDocumentNumber.Trim());
        if (supplier is null)
        {
            return (false, "No se encontro un proveedor con ese numero de documento.", null);
        }

        var token = request.Token.Trim();
        var warnings = new List<string>();
        var categoriesProcessed = 0;
        var categoriesCreated = 0;
        var productsProcessed = 0;
        var productsCreated = 0;
        var productsUpdated = 0;

        var minimumStock = (await companyRepository.GetAsync())?.MinimumStock ?? DefaultMinimumStock;
        var expiration = DateOnly.FromDateTime(DateTime.Today.AddYears(2));
        var categoryCache = new Dictionary<string, Category>(StringComparer.OrdinalIgnoreCase);

        List<ProviderCategory> categories;
        try
        {
            categories = await FetchCategoriesAsync(token);
        }
        catch (SupplierSyncUnauthorizedException)
        {
            return (false, "El token esta vencido o no es valido. Vuelve a copiarlo del portal del proveedor.", null);
        }
        catch (HttpRequestException ex)
        {
            return (false, $"No se pudo conectar con el proveedor: {ex.Message}", null);
        }

        foreach (var category in categories)
        {
            categoriesProcessed++;
            var categoryName = string.IsNullOrWhiteSpace(category.Name) ? "Sin categoria" : category.Name.Trim();

            if (!categoryCache.TryGetValue(categoryName, out var localCategory))
            {
                localCategory = await categoryRepository.GetByNameAsync(categoryName);
                if (localCategory is null)
                {
                    categoriesCreated++;
                    if (!request.PreviewOnly)
                    {
                        localCategory = new Category { Name = Truncate(categoryName, 100), IsActive = true };
                        await categoryRepository.AddAsync(localCategory);
                        await categoryRepository.SaveChangesAsync();
                    }
                }

                if (localCategory is not null)
                {
                    categoryCache[categoryName] = localCategory;
                }
            }

            List<ProviderProduct> products;
            try
            {
                products = await FetchProductsAsync(token, category.ProviderId);
            }
            catch (SupplierSyncUnauthorizedException)
            {
                return (false, "El token esta vencido o no es valido. Vuelve a copiarlo del portal del proveedor.", null);
            }
            catch (HttpRequestException ex)
            {
                return (false, $"No se pudo conectar con el proveedor: {ex.Message}", null);
            }

            foreach (var providerProduct in products)
            {
                productsProcessed++;

                var units = providerProduct.Units;
                if (units <= 0)
                {
                    units = 1;
                    warnings.Add($"El producto '{Describe(providerProduct)}' no traia 'units' valido; se uso 1.");
                }

                // Costo: lo que paga el negocio al proveedor. NO se redondea a la decima (solo precision de 2 decimales).
                var cost = Math.Round(providerProduct.CustomerPrice / units, 2, MidpointRounding.AwayFromZero);
                // Precio de venta: se redondea a la decima mas cercana (regla de negocio).
                var price = Math.Round(providerProduct.SalePrice / units, 1, MidpointRounding.AwayFromZero);

                var name = Truncate(providerProduct.LongDescription, 150);
                if (string.IsNullOrWhiteSpace(name))
                {
                    name = Truncate(providerProduct.ShortDescription, 150);
                }

                if (string.IsNullOrWhiteSpace(name))
                {
                    warnings.Add($"Se omitio un producto sin nombre (sku '{providerProduct.Sku}').");
                    continue;
                }

                // El shortDescription del proveedor viene largo; armamos un nombre corto a partir del
                // formato del proveedor ("Nombre, empaque volumen, N Unidades").
                var shortName = BuildShortName(name);
                var sku = string.IsNullOrWhiteSpace(providerProduct.Sku) ? null : Truncate(providerProduct.Sku, 30);

                var existing = sku is null ? null : await productRepository.GetBySkuAsync(sku);

                if (existing is null)
                {
                    productsCreated++;
                    if (!request.PreviewOnly && localCategory is not null)
                    {
                        sku ??= await GenerateSkuAsync(categoryName);
                        var product = new Product
                        {
                            Name = name,
                            ShortName = shortName,
                            Sku = sku,
                            Barcode = null,
                            PurchaseBarcode = null,
                            Description = null,
                            Price = price,
                            Cost = cost,
                            Stock = 0,
                            MinimumStock = minimumStock,
                            ExpirationDate = expiration,
                            SalesUnitName = "Unidad",
                            PurchaseUnitName = "Unidad",
                            UnitsPerPurchaseUnit = 1,
                            IsActive = true,
                            CategoryId = localCategory.Id
                        };

                        await productRepository.AddAsync(product);
                        await productRepository.SaveChangesAsync();
                        await AddHistoryAsync(supplier.Id, product.Id, cost);
                    }
                }
                else
                {
                    productsUpdated++;
                    if (!request.PreviewOnly && localCategory is not null)
                    {
                        existing.Name = name;
                        existing.ShortName = shortName;
                        existing.Cost = cost;
                        existing.Price = price;
                        existing.CategoryId = localCategory.Id;
                        productRepository.Update(existing);
                        await productRepository.SaveChangesAsync();
                        await AddHistoryAsync(supplier.Id, existing.Id, cost);
                    }
                }
            }
        }

        var result = new SupplierSyncResultDto(
            request.PreviewOnly,
            supplier.Name,
            categoriesProcessed,
            categoriesCreated,
            productsProcessed,
            productsCreated,
            productsUpdated,
            warnings);

        return (true, null, result);
    }

    private async Task AddHistoryAsync(int supplierId, int productId, decimal cost)
    {
        await supplierProductRepository.AddAsync(new SupplierProduct
        {
            SupplierId = supplierId,
            ProductId = productId,
            LastCost = cost,
            Date = DateTime.Now
        });
        await supplierProductRepository.SaveChangesAsync();
    }

    private async Task<List<ProviderCategory>> FetchCategoriesAsync(string token)
    {
        var root = await GetJsonAsync(CategoriesUrl, token);
        var array = FindArray(root);
        var result = new List<ProviderCategory>();

        if (array.ValueKind != JsonValueKind.Array)
        {
            return result;
        }

        foreach (var element in array.EnumerateArray())
        {
            if (element.ValueKind != JsonValueKind.Object)
            {
                continue;
            }

            var id = GetInt(element, "id", "categoryId", "categoryID", "Id");
            var name = GetString(element, "name", "categoryName", "description", "title");
            if (id is not null)
            {
                result.Add(new ProviderCategory(id.Value, name ?? string.Empty));
            }
        }

        return result;
    }

    private async Task<List<ProviderProduct>> FetchProductsAsync(string token, int categoryId)
    {
        var url = string.Format(CultureInfo.InvariantCulture, PortfolioUrlFormat, categoryId);
        var root = await GetJsonAsync(url, token);
        var array = FindArray(root);
        var result = new List<ProviderProduct>();

        if (array.ValueKind != JsonValueKind.Array)
        {
            return result;
        }

        foreach (var element in array.EnumerateArray())
        {
            if (element.ValueKind != JsonValueKind.Object)
            {
                continue;
            }

            result.Add(new ProviderProduct(
                GetString(element, "sku", "SKU", "code") ?? string.Empty,
                GetString(element, "longDescription", "name", "description") ?? string.Empty,
                GetString(element, "shortDescription", "shortName") ?? string.Empty,
                GetDecimal(element, "salePrice", "price") ?? 0m,
                GetDecimal(element, "customerPrice", "cost") ?? 0m,
                GetDecimal(element, "units", "unit", "quantity") ?? 1m,
                GetInt(element, "categoryId", "categoryID", "category") ?? categoryId));
        }

        return result;
    }

    private async Task<JsonElement> GetJsonAsync(string url, string token)
    {
        var client = httpClientFactory.CreateClient();
        client.Timeout = TimeSpan.FromSeconds(90);

        using var requestMessage = new HttpRequestMessage(HttpMethod.Get, url);
        requestMessage.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        requestMessage.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));

        using var response = await client.SendAsync(requestMessage);
        if (response.StatusCode is HttpStatusCode.Unauthorized or HttpStatusCode.Forbidden)
        {
            throw new SupplierSyncUnauthorizedException();
        }

        response.EnsureSuccessStatusCode();

        await using var stream = await response.Content.ReadAsStreamAsync();
        using var document = await JsonDocument.ParseAsync(stream);
        return document.RootElement.Clone();
    }

    private async Task<string> GenerateSkuAsync(string categoryName)
    {
        var prefix = BuildCategoryPrefix(categoryName);
        var existing = await productRepository.GetSkusByPrefixAsync($"{prefix}-");
        var next = existing
            .Select(ParseSkuSequence)
            .DefaultIfEmpty(0)
            .Max() + 1;

        return $"{prefix}-{next:000000}";
    }

    private static int ParseSkuSequence(string sku)
    {
        var parts = sku.Split('-', 2);
        return parts.Length == 2 && int.TryParse(parts[1], out var value) ? value : 0;
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

    private static string Truncate(string? value, int maxLength)
    {
        var trimmed = value?.Trim() ?? string.Empty;
        return trimmed.Length <= maxLength ? trimmed : trimmed[..maxLength];
    }

    private static readonly Regex VolumeRegex =
        new(@"\d+([.,]\d+)?\s*(ml|lt|l|kg|g)\b", RegexOptions.IgnoreCase | RegexOptions.Compiled);

    // Nombre corto a partir del formato del proveedor: toma el texto antes de la primera coma
    // (el nombre del producto) y le agrega el volumen (ml/L/g/kg) si no estaba ya incluido.
    // Asi se descartan el empaque ("PET", "Lata", "Vidrio...") y la cola "N Unidades".
    private static string BuildShortName(string name)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            return string.Empty;
        }

        var firstSegment = Regex.Replace(name.Split(',')[0].Trim(), @"\s+", " ");
        var match = VolumeRegex.Match(name);
        var volume = match.Success ? Regex.Replace(match.Value, @"\s+", string.Empty) : string.Empty;

        var result = firstSegment;
        if (volume.Length > 0 &&
            !firstSegment.Replace(" ", string.Empty).Contains(volume, StringComparison.OrdinalIgnoreCase))
        {
            result = $"{firstSegment} {volume}";
        }

        result = Regex.Replace(result, @"\s+", " ").Trim();
        return result.Length > 60 ? result[..60] : result;
    }

    private static string Describe(ProviderProduct product) =>
        !string.IsNullOrWhiteSpace(product.LongDescription) ? product.LongDescription
        : !string.IsNullOrWhiteSpace(product.Sku) ? product.Sku
        : "(sin nombre)";

    // Busca el array de datos dentro de la respuesta, sin importar el nombre del envoltorio.
    private static JsonElement FindArray(JsonElement root)
    {
        if (root.ValueKind == JsonValueKind.Array)
        {
            return root;
        }

        if (root.ValueKind != JsonValueKind.Object)
        {
            return default;
        }

        foreach (var key in new[] { "data", "items", "products", "result", "results", "portfolio", "categories", "value", "content" })
        {
            if (TryGetProperty(root, key, out var value) && value.ValueKind == JsonValueKind.Array)
            {
                return value;
            }
        }

        foreach (var property in root.EnumerateObject())
        {
            if (property.Value.ValueKind == JsonValueKind.Array)
            {
                return property.Value;
            }
        }

        foreach (var property in root.EnumerateObject())
        {
            if (property.Value.ValueKind == JsonValueKind.Object)
            {
                var nested = FindArray(property.Value);
                if (nested.ValueKind == JsonValueKind.Array)
                {
                    return nested;
                }
            }
        }

        return default;
    }

    private static bool TryGetProperty(JsonElement element, string name, out JsonElement value)
    {
        foreach (var property in element.EnumerateObject())
        {
            if (string.Equals(property.Name, name, StringComparison.OrdinalIgnoreCase))
            {
                value = property.Value;
                return true;
            }
        }

        value = default;
        return false;
    }

    private static string? GetString(JsonElement element, params string[] names)
    {
        foreach (var name in names)
        {
            if (TryGetProperty(element, name, out var value))
            {
                if (value.ValueKind == JsonValueKind.String)
                {
                    return value.GetString();
                }

                if (value.ValueKind is JsonValueKind.Number or JsonValueKind.True or JsonValueKind.False)
                {
                    return value.ToString();
                }
            }
        }

        return null;
    }

    private static decimal? GetDecimal(JsonElement element, params string[] names)
    {
        foreach (var name in names)
        {
            if (TryGetProperty(element, name, out var value))
            {
                if (value.ValueKind == JsonValueKind.Number && value.TryGetDecimal(out var number))
                {
                    return number;
                }

                if (value.ValueKind == JsonValueKind.String &&
                    decimal.TryParse(value.GetString(), NumberStyles.Any, CultureInfo.InvariantCulture, out var parsed))
                {
                    return parsed;
                }
            }
        }

        return null;
    }

    private static int? GetInt(JsonElement element, params string[] names)
    {
        var value = GetDecimal(element, names);
        return value is null ? null : (int)value.Value;
    }

    private sealed record ProviderCategory(int ProviderId, string Name);

    private sealed record ProviderProduct(
        string Sku,
        string LongDescription,
        string ShortDescription,
        decimal SalePrice,
        decimal CustomerPrice,
        decimal Units,
        int CategoryId);

    private sealed class SupplierSyncUnauthorizedException : Exception;
}
