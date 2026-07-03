using System.Globalization;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;
using Minimarket.Api.DTOs;
using Minimarket.Api.Helpers;
using Minimarket.Api.Models;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

// Lee la foto de una boleta de compra con IA (Claude vision), propone productos/costos,
// y al confirmar guarda el costo por proveedor (ProveedorProducto) y crea/actualiza productos.
public class ReceiptService(
    IHttpClientFactory httpClientFactory,
    ICompanyRepository companyRepository,
    IProductRepository productRepository,
    ICategoryRepository categoryRepository,
    ISupplierRepository supplierRepository,
    ISupplierProductRepository supplierProductRepository) : IReceiptService
{
    private const string ClaudeUrl = "https://api.anthropic.com/v1/messages";
    private const string ClaudeModel = "claude-sonnet-5";
    private const decimal IgvFactor = 1.18m;
    private const int DefaultMinimumStock = 5;

    private static readonly HashSet<string> StopWords = new(StringComparer.OrdinalIgnoreCase)
    {
        "pet","lata","latas","vidrio","retornable","no","botella","unidad","unidades","uni",
        "con","gas","sin","de","del","la","el","los","las","x","pack","caja","bolsa","gr","g","ml"
    };

    private const string Prompt =
        "Eres un lector de boletas de compra de un minimarket en Peru. Lee la imagen y devuelve SOLO un JSON " +
        "valido (sin texto adicional, sin markdown) con esta forma exacta: " +
        "{\"proveedor\":{\"nombre\":\"\",\"ruc\":\"\"},\"items\":[{\"descripcion\":\"\",\"cantidad\":1," +
        "\"precioUnitario\":0,\"unidadesPorPaquete\":1,\"categoria\":\"\"}]}. " +
        "Reglas: precioUnitario es el precio unitario SIN IGV tal como aparece en la columna Precio. " +
        "unidadesPorPaquete: si el item viene en tira/pack (ej. x6, x12, TRAx12UND, PCKx6UND) pon ese numero; " +
        "si es unidad suelta pon 1. categoria: clasifica el producto (Abarrotes, Bebidas, Gaseosas, Aguas, Jugos, " +
        "Cuidado personal, Limpieza, Snacks, Golosinas, Galletas, Licores, Cervezas, Lacteos). Si un dato no esta usa " +
        "0 o cadena vacia. No inventes items que no esten en la boleta.";

    public async Task<(bool Success, string? Error, ReceiptScanResultDto? Result)> ScanAsync(ReceiptScanRequestDto request)
    {
        if (string.IsNullOrWhiteSpace(request.ImageBase64))
        {
            return (false, "No se recibio la imagen de la boleta.", null);
        }

        var company = await companyRepository.GetAsync();
        if (company is null || string.IsNullOrWhiteSpace(company.AiReceiptKey))
        {
            return (false, "Falta configurar la clave de IA en Configuracion para leer boletas.", null);
        }

        string apiKey;
        try
        {
            apiKey = CryptoHelper.Decrypt(company.AiReceiptKey);
        }
        catch
        {
            return (false, "La clave de IA guardada no es valida. Vuelve a guardarla en Configuracion.", null);
        }

        JsonElement parsed;
        try
        {
            var text = await CallClaudeAsync(apiKey, request.ImageBase64, request.MediaType ?? "image/jpeg");
            parsed = ParseJsonFromText(text);
        }
        catch (ClaudeAuthException)
        {
            return (false, "La clave de IA fue rechazada. Verifica que sea correcta y tenga saldo.", null);
        }
        catch (HttpRequestException ex)
        {
            return (false, $"No se pudo contactar la IA: {ex.Message}", null);
        }
        catch (JsonException)
        {
            return (false, "La IA no devolvio un resultado legible. Intenta con una foto mas clara.", null);
        }

        var warnings = new List<string>();
        var supplierName = GetString(parsed.TryGetProperty("proveedor", out var prov) ? prov : default, "nombre") ?? "";
        var supplierRuc = GetString(prov, "ruc");
        int? supplierId = null;
        if (!string.IsNullOrWhiteSpace(supplierRuc))
        {
            supplierId = (await supplierRepository.GetByDocumentNumberAsync(supplierRuc.Trim()))?.Id;
        }

        var products = await productRepository.GetAllAsync();
        var activeProducts = products.Where(p => p.IsActive).ToList();

        var lines = new List<ReceiptScanLineDto>();
        if (parsed.TryGetProperty("items", out var items) && items.ValueKind == JsonValueKind.Array)
        {
            foreach (var item in items.EnumerateArray())
            {
                var description = (GetString(item, "descripcion") ?? "").Trim();
                if (string.IsNullOrWhiteSpace(description))
                {
                    continue;
                }

                var priceNet = GetDecimal(item, "precioUnitario") ?? 0m;
                var packUnits = Math.Max(1, GetInt(item, "unidadesPorPaquete") ?? 1);
                var quantity = GetDecimal(item, "cantidad") ?? 1m;
                var category = (GetString(item, "categoria") ?? "Abarrotes").Trim();
                if (category.Length == 0)
                {
                    category = "Abarrotes";
                }

                var unitCost = Math.Round(priceNet * IgvFactor / packUnits, 2, MidpointRounding.AwayFromZero);
                var suggestedName = Truncate(description, 150);
                var suggestedShort = Truncate(ShortNameGenerator.Generate(suggestedName), 60);
                var suggestedPrice = RoundFriendly(unitCost * MarginFactor(category));

                var (matchId, matchName, score) = BestMatch(suggestedName, activeProducts);

                lines.Add(new ReceiptScanLineDto(
                    description, quantity, packUnits, unitCost,
                    suggestedName, suggestedShort, category, suggestedPrice,
                    matchId, matchName, score));
            }
        }

        if (lines.Count == 0)
        {
            warnings.Add("No se detectaron productos en la boleta.");
        }

        var result = new ReceiptScanResultDto(supplierName, supplierRuc, supplierId, lines, warnings);
        return (true, null, result);
    }

    public async Task<(bool Success, string? Error, ReceiptConfirmResultDto? Result)> ConfirmAsync(ReceiptConfirmRequestDto request)
    {
        var supplier = await supplierRepository.GetByIdAsync(request.SupplierId);
        if (supplier is null)
        {
            return (false, "El proveedor seleccionado no existe.", null);
        }

        if (request.Items is null || request.Items.Count == 0)
        {
            return (false, "No hay items para guardar.", null);
        }

        var minimumStock = (await companyRepository.GetAsync())?.MinimumStock ?? DefaultMinimumStock;
        var expiration = DateOnly.FromDateTime(DateTime.Today.AddYears(2));
        var warnings = new List<string>();
        var created = 0;
        var updated = 0;
        var costsRecorded = 0;

        foreach (var item in request.Items)
        {
            var action = (item.Action ?? "").Trim().ToLowerInvariant();
            if (action == "skip")
            {
                continue;
            }

            if (action == "create")
            {
                var categoryName = string.IsNullOrWhiteSpace(item.CategoryName) ? "Abarrotes" : item.CategoryName.Trim();
                var category = await categoryRepository.GetByNameAsync(categoryName);
                if (category is null)
                {
                    category = new Category { Name = Truncate(categoryName, 100), IsActive = true };
                    await categoryRepository.AddAsync(category);
                    await categoryRepository.SaveChangesAsync();
                }

                var name = Truncate(string.IsNullOrWhiteSpace(item.Name) ? "Producto" : item.Name, 150);
                var product = new Product
                {
                    Name = name,
                    ShortName = Truncate(string.IsNullOrWhiteSpace(item.ShortName) ? ShortNameGenerator.Generate(name) : item.ShortName, 60),
                    Sku = await GenerateSkuAsync(category.Name),
                    Barcode = null,
                    PurchaseBarcode = null,
                    Description = null,
                    Price = item.Price > 0 ? item.Price : item.Cost,
                    Cost = item.Cost,
                    Stock = 0,
                    MinimumStock = minimumStock,
                    ExpirationDate = expiration,
                    SalesUnitName = "Unidad",
                    PurchaseUnitName = "Unidad",
                    UnitsPerPurchaseUnit = 1,
                    IsActive = true,
                    CategoryId = category.Id
                };

                await productRepository.AddAsync(product);
                await productRepository.SaveChangesAsync();
                await RecordCostAsync(supplier.Id, product.Id, item.Cost);
                created++;
                costsRecorded++;
            }
            else if (action == "match" && item.ProductId is int productId)
            {
                var product = await productRepository.GetByIdAsync(productId);
                if (product is null)
                {
                    warnings.Add($"No se encontro el producto a actualizar (id {productId}).");
                    continue;
                }

                product.Cost = item.Cost;
                productRepository.Update(product);
                await productRepository.SaveChangesAsync();
                await RecordCostAsync(supplier.Id, product.Id, item.Cost);
                updated++;
                costsRecorded++;
            }
        }

        return (true, null, new ReceiptConfirmResultDto(created, updated, costsRecorded, warnings));
    }

    private async Task RecordCostAsync(int supplierId, int productId, decimal cost)
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

    private async Task<string> CallClaudeAsync(string apiKey, string imageBase64, string mediaType)
    {
        var payload = new
        {
            model = ClaudeModel,
            max_tokens = 4000,
            messages = new[]
            {
                new
                {
                    role = "user",
                    content = new object[]
                    {
                        new { type = "image", source = new { type = "base64", media_type = mediaType, data = imageBase64 } },
                        new { type = "text", text = Prompt }
                    }
                }
            }
        };

        var client = httpClientFactory.CreateClient();
        client.Timeout = TimeSpan.FromSeconds(120);

        using var requestMessage = new HttpRequestMessage(HttpMethod.Post, ClaudeUrl);
        requestMessage.Headers.TryAddWithoutValidation("x-api-key", apiKey);
        requestMessage.Headers.TryAddWithoutValidation("anthropic-version", "2023-06-01");
        requestMessage.Content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8);
        requestMessage.Content.Headers.ContentType = new MediaTypeHeaderValue("application/json");

        using var response = await client.SendAsync(requestMessage);
        if (response.StatusCode is System.Net.HttpStatusCode.Unauthorized or System.Net.HttpStatusCode.Forbidden)
        {
            throw new ClaudeAuthException();
        }

        response.EnsureSuccessStatusCode();

        await using var stream = await response.Content.ReadAsStreamAsync();
        using var doc = await JsonDocument.ParseAsync(stream);
        var sb = new StringBuilder();
        if (doc.RootElement.TryGetProperty("content", out var content) && content.ValueKind == JsonValueKind.Array)
        {
            foreach (var block in content.EnumerateArray())
            {
                if (block.TryGetProperty("type", out var t) && t.GetString() == "text"
                    && block.TryGetProperty("text", out var txt))
                {
                    sb.Append(txt.GetString());
                }
            }
        }

        return sb.ToString();
    }

    private static JsonElement ParseJsonFromText(string text)
    {
        var trimmed = (text ?? "").Trim();
        var start = trimmed.IndexOf('{');
        var end = trimmed.LastIndexOf('}');
        if (start >= 0 && end > start)
        {
            trimmed = trimmed.Substring(start, end - start + 1);
        }

        using var doc = JsonDocument.Parse(trimmed);
        return doc.RootElement.Clone();
    }

    private (int? Id, string? Name, double Score) BestMatch(string name, List<Product> products)
    {
        var target = Tokenize(name);
        int? bestId = null;
        string? bestName = null;
        double best = 0;

        foreach (var product in products)
        {
            var score = Jaccard(target, Tokenize(product.Name));
            if (score > best)
            {
                best = score;
                bestId = product.Id;
                bestName = product.Name;
            }
        }

        return best >= 0.5 ? (bestId, bestName, best) : (null, null, best);
    }

    private static HashSet<string> Tokenize(string value)
    {
        var normalized = StripAccents(value).ToLowerInvariant();
        normalized = Regex.Replace(normalized, @"[^a-z0-9]+", " ");
        return normalized.Split(' ', StringSplitOptions.RemoveEmptyEntries)
            .Where(t => t.Length >= 2 && !StopWords.Contains(t) && !Regex.IsMatch(t, @"^\d+$"))
            .ToHashSet();
    }

    private static double Jaccard(HashSet<string> a, HashSet<string> b)
    {
        if (a.Count == 0 || b.Count == 0)
        {
            return 0;
        }

        var inter = a.Count(b.Contains);
        var union = a.Count + b.Count - inter;
        return union == 0 ? 0 : (double)inter / union;
    }

    private static decimal MarginFactor(string category)
    {
        var c = StripAccents(category).ToLowerInvariant();
        if (c.Contains("gaseosa") || c.Contains("agua") || c.Contains("jugo") || c.Contains("isoton") || c.Contains("energiz") || c.Contains("bebida"))
            return 1.35m;
        if (c.Contains("snack") || c.Contains("golosina") || c.Contains("galleta") || c.Contains("chocolate") || c.Contains("piqueo"))
            return 1.40m;
        if (c.Contains("cerveza"))
            return 1.25m;
        if (c.Contains("licor"))
            return 1.22m;
        if (c.Contains("cuidado") || c.Contains("limpieza") || c.Contains("higiene") || c.Contains("personal") || c.Contains("hogar"))
            return 1.30m;
        return 1.18m;
    }

    private static decimal RoundFriendly(decimal value) => Math.Round(value, 1, MidpointRounding.AwayFromZero);

    private async Task<string> GenerateSkuAsync(string categoryName)
    {
        var prefix = BuildCategoryPrefix(categoryName);
        var existing = await productRepository.GetSkusByPrefixAsync($"{prefix}-");
        var next = existing.Select(ParseSkuSequence).DefaultIfEmpty(0).Max() + 1;
        return $"{prefix}-{next:000000}";
    }

    private static int ParseSkuSequence(string sku)
    {
        var parts = sku.Split('-', 2);
        return parts.Length == 2 && int.TryParse(parts[1], out var value) ? value : 0;
    }

    private static string BuildCategoryPrefix(string categoryName)
    {
        var normalized = StripAccents(categoryName);
        var builder = new StringBuilder();
        foreach (var c in normalized)
        {
            if (char.IsLetterOrDigit(c))
            {
                builder.Append(char.ToUpperInvariant(c));
            }
            if (builder.Length == 3)
            {
                break;
            }
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

    private static string StripAccents(string value)
    {
        var normalized = value.Normalize(NormalizationForm.FormD);
        var builder = new StringBuilder();
        foreach (var c in normalized)
        {
            if (CharUnicodeInfo.GetUnicodeCategory(c) != UnicodeCategory.NonSpacingMark)
            {
                builder.Append(c);
            }
        }
        return builder.ToString().Normalize(NormalizationForm.FormC);
    }

    private static string? GetString(JsonElement element, string name)
    {
        if (element.ValueKind == JsonValueKind.Object && element.TryGetProperty(name, out var value))
        {
            return value.ValueKind == JsonValueKind.String ? value.GetString()
                : value.ValueKind is JsonValueKind.Number or JsonValueKind.True or JsonValueKind.False ? value.ToString()
                : null;
        }
        return null;
    }

    private static decimal? GetDecimal(JsonElement element, string name)
    {
        if (element.ValueKind == JsonValueKind.Object && element.TryGetProperty(name, out var value))
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
        return null;
    }

    private static int? GetInt(JsonElement element, string name)
    {
        var value = GetDecimal(element, name);
        return value is null ? null : (int)value.Value;
    }

    private sealed class ClaudeAuthException : Exception;
}
