package com.minimarket.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.api.dto.SupplierSyncRequestDto;
import com.minimarket.api.dto.SupplierSyncResultDto;
import com.minimarket.api.entity.Category;
import com.minimarket.api.entity.Product;
import com.minimarket.api.entity.Supplier;
import com.minimarket.api.entity.SupplierProduct;
import com.minimarket.api.repository.CategoryRepository;
import com.minimarket.api.repository.CompanyRepository;
import com.minimarket.api.repository.ProductRepository;
import com.minimarket.api.repository.SupplierProductRepository;
import com.minimarket.api.repository.SupplierRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Sincroniza el catalogo del proveedor (Coca-Cola / AIC Digital - Arca Continental) hacia Productos.
// Reglas definidas en docs/PROJECT_CONTEXT.md (seccion de sincronizacion de catalogo de proveedor).
//
// IMPORTANTE: los nombres de las propiedades del JSON del proveedor (sku, longDescription, etc.)
// estan tomados de la investigacion documentada. Si la respuesta real difiere, ajustar las
// listas de candidatos en getString/getDecimal/getInt y en findArray (el parseo es tolerante).
@Service
public class SupplierSyncService {

    private static final int DEFAULT_MINIMUM_STOCK = 5;
    private static final String CUSTOMER_ID = "2898397";

    private static final String CATEGORIES_URL =
        "https://briolightapimgmt.arcacontal.com/product/api/v1/Category/GetHomePageCategories?customerId=" + CUSTOMER_ID;

    private static final String PORTFOLIO_URL_FORMAT =
        "https://briolightapimgmt.arcacontal.com/product/api/v1/Portfolio?businessUnitId=4&CategoryId=%d"
        + "&Phrase=&Type=4&Limit=500&Offset=1&CustomerId=" + CUSTOMER_ID + "&order=0&searchCriteria=";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final CompanyRepository companyRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public SupplierSyncService(ProductRepository productRepository,
                               CategoryRepository categoryRepository,
                               SupplierRepository supplierRepository,
                               SupplierProductRepository supplierProductRepository,
                               CompanyRepository companyRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.supplierProductRepository = supplierProductRepository;
        this.companyRepository = companyRepository;
    }

    public ServiceResult<SupplierSyncResultDto> sync(SupplierSyncRequestDto request) {
        if (request.token() == null || request.token().isBlank()) {
            return ServiceResult.failure("Debes pegar el token del proveedor.");
        }

        if (request.supplierDocumentNumber() == null || request.supplierDocumentNumber().isBlank()) {
            return ServiceResult.failure("Debes seleccionar el proveedor.");
        }

        var supplier = supplierRepository.findByDocumentNumber(request.supplierDocumentNumber().trim()).orElse(null);
        if (supplier == null) {
            return ServiceResult.failure("No se encontro un proveedor con ese numero de documento.");
        }

        var previewOnly = Boolean.TRUE.equals(request.previewOnly());
        var token = request.token().trim();
        var warnings = new ArrayList<String>();
        var counters = new Counters();

        var minimumStock = companyRepository.findById(1)
            .map(com.minimarket.api.entity.Company::getMinimumStock)
            .orElse(DEFAULT_MINIMUM_STOCK);
        var expiration = LocalDate.now().plusYears(2);
        var categoryCache = new HashMap<String, Category>();

        List<ProviderCategory> categories;
        try {
            categories = fetchCategories(token);
        } catch (UnauthorizedException ex) {
            return ServiceResult.failure("El token esta vencido o no es valido. Vuelve a copiarlo del portal del proveedor.");
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ServiceResult.failure("No se pudo conectar con el proveedor: " + ex.getMessage());
        }

        for (var category : categories) {
            counters.categoriesProcessed++;
            var categoryName = category.name() == null || category.name().isBlank() ? "Sin categoria" : category.name().trim();

            var localCategory = categoryCache.get(categoryName.toLowerCase());
            if (localCategory == null) {
                localCategory = categoryRepository.findByNameIgnoreCase(categoryName).orElse(null);
                if (localCategory == null) {
                    counters.categoriesCreated++;
                    if (!previewOnly) {
                        var nueva = new Category();
                        nueva.setName(truncate(categoryName, 100));
                        nueva.setIsActive(true);
                        localCategory = categoryRepository.save(nueva);
                    }
                }
                if (localCategory != null) {
                    categoryCache.put(categoryName.toLowerCase(), localCategory);
                }
            }

            List<ProviderProduct> products;
            try {
                products = fetchProducts(token, category.providerId());
            } catch (UnauthorizedException ex) {
                return ServiceResult.failure("El token esta vencido o no es valido. Vuelve a copiarlo del portal del proveedor.");
            } catch (IOException | InterruptedException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                return ServiceResult.failure("No se pudo conectar con el proveedor: " + ex.getMessage());
            }

            for (var providerProduct : products) {
                counters.productsProcessed++;

                var units = providerProduct.units();
                if (units == null || units.signum() <= 0) {
                    units = BigDecimal.ONE;
                    warnings.add("El producto '" + describe(providerProduct) + "' no traia 'units' valido; se uso 1.");
                }

                // Costo: lo que paga el negocio al proveedor. NO se redondea a la decima (solo 2 decimales de precision).
                var cost = providerProduct.customerPrice().divide(units, 2, RoundingMode.HALF_UP);
                // Precio de venta: se redondea a la decima mas cercana (regla de negocio).
                var price = providerProduct.salePrice().divide(units, 1, RoundingMode.HALF_UP);

                var name = truncate(providerProduct.longDescription(), 150);
                if (name.isBlank()) {
                    name = truncate(providerProduct.shortDescription(), 150);
                }

                if (name.isBlank()) {
                    warnings.add("Se omitio un producto sin nombre (sku '" + providerProduct.sku() + "').");
                    continue;
                }

                var shortName = truncate(providerProduct.shortDescription(), 60);
                var sku = providerProduct.sku() == null || providerProduct.sku().isBlank()
                    ? null
                    : truncate(providerProduct.sku(), 30);

                var existing = sku == null ? null : productRepository.findWithCategoryBySku(sku).orElse(null);

                if (existing == null) {
                    counters.productsCreated++;
                    if (!previewOnly && localCategory != null) {
                        if (sku == null) {
                            sku = generateSku(categoryName);
                        }
                        var product = new Product();
                        product.setName(name);
                        product.setShortName(shortName);
                        product.setSku(sku);
                        product.setBarcode(null);
                        product.setPurchaseBarcode(null);
                        product.setDescription(null);
                        product.setPrice(price);
                        product.setCost(cost);
                        product.setStock(0);
                        product.setMinimumStock(minimumStock);
                        product.setExpirationDate(expiration);
                        product.setSalesUnitName("Unidad");
                        product.setPurchaseUnitName("Unidad");
                        product.setUnitsPerPurchaseUnit(1);
                        product.setIsActive(true);
                        product.setCategoryId(localCategory.getId());

                        var saved = productRepository.save(product);
                        addHistory(supplier.getId(), saved.getId(), cost);
                    }
                } else {
                    counters.productsUpdated++;
                    if (!previewOnly && localCategory != null) {
                        existing.setName(name);
                        existing.setShortName(shortName);
                        existing.setCost(cost);
                        existing.setPrice(price);
                        existing.setCategoryId(localCategory.getId());
                        productRepository.save(existing);
                        addHistory(supplier.getId(), existing.getId(), cost);
                    }
                }
            }
        }

        var result = new SupplierSyncResultDto(
            previewOnly,
            supplier.getName(),
            counters.categoriesProcessed,
            counters.categoriesCreated,
            counters.productsProcessed,
            counters.productsCreated,
            counters.productsUpdated,
            warnings);

        return ServiceResult.success(result);
    }

    private void addHistory(Integer supplierId, Integer productId, BigDecimal cost) {
        var entry = new SupplierProduct();
        entry.setSupplierId(supplierId);
        entry.setProductId(productId);
        entry.setLastCost(cost);
        entry.setDate(LocalDateTime.now());
        supplierProductRepository.save(entry);
    }

    private List<ProviderCategory> fetchCategories(String token) throws IOException, InterruptedException {
        var root = getJson(CATEGORIES_URL, token);
        var array = findArray(root);
        var result = new ArrayList<ProviderCategory>();
        if (array == null || !array.isArray()) {
            return result;
        }

        for (var element : array) {
            if (!element.isObject()) {
                continue;
            }
            var id = getInt(element, "id", "categoryId", "categoryID", "Id");
            var name = getString(element, "name", "categoryName", "description", "title");
            if (id != null) {
                result.add(new ProviderCategory(id, name == null ? "" : name));
            }
        }

        return result;
    }

    private List<ProviderProduct> fetchProducts(String token, int categoryId) throws IOException, InterruptedException {
        var url = PORTFOLIO_URL_FORMAT.formatted(categoryId);
        var root = getJson(url, token);
        var array = findArray(root);
        var result = new ArrayList<ProviderProduct>();
        if (array == null || !array.isArray()) {
            return result;
        }

        for (var element : array) {
            if (!element.isObject()) {
                continue;
            }
            result.add(new ProviderProduct(
                orEmpty(getString(element, "sku", "SKU", "code")),
                orEmpty(getString(element, "longDescription", "name", "description")),
                orEmpty(getString(element, "shortDescription", "shortName")),
                orZero(getDecimal(element, "salePrice", "price")),
                orZero(getDecimal(element, "customerPrice", "cost")),
                orOne(getDecimal(element, "units", "unit", "quantity")),
                getInt(element, "categoryId", "categoryID", "category") == null
                    ? categoryId
                    : getInt(element, "categoryId", "categoryID", "category")));
        }

        return result;
    }

    private JsonNode getJson(String url, String token) throws IOException, InterruptedException {
        var httpRequest = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(90))
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .GET()
            .build();

        var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        var status = response.statusCode();
        if (status == 401 || status == 403) {
            throw new UnauthorizedException();
        }
        if (status >= 400) {
            throw new IOException("HTTP " + status);
        }

        return objectMapper.readTree(response.body());
    }

    private String generateSku(String categoryName) {
        var prefix = buildCategoryPrefix(categoryName);
        var nextNumber = productRepository.findBySkuStartingWith(prefix + "-")
            .stream()
            .map(Product::getSku)
            .mapToInt(this::parseSkuSequence)
            .max()
            .orElse(0) + 1;

        return "%s-%06d".formatted(prefix, nextNumber);
    }

    private int parseSkuSequence(String sku) {
        var parts = sku.split("-", 2);
        if (parts.length != 2) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String buildCategoryPrefix(String categoryName) {
        var normalized = Normalizer.normalize(categoryName, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("[^A-Za-z0-9]", "")
            .toUpperCase();

        if (normalized.isBlank()) {
            return "CAT";
        }

        var prefix = normalized.length() >= 3 ? normalized.substring(0, 3) : normalized;
        return (prefix + "XXX").substring(0, 3);
    }

    private static String describe(ProviderProduct product) {
        if (product.longDescription() != null && !product.longDescription().isBlank()) {
            return product.longDescription();
        }
        if (product.sku() != null && !product.sku().isBlank()) {
            return product.sku();
        }
        return "(sin nombre)";
    }

    private static String truncate(String value, int maxLength) {
        var trimmed = value == null ? "" : value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal orOne(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }

    // Busca el array de datos dentro de la respuesta, sin importar el nombre del envoltorio.
    private static JsonNode findArray(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (!root.isObject()) {
            return null;
        }

        for (var key : List.of("data", "items", "products", "result", "results", "portfolio", "categories", "value", "content")) {
            var value = field(root, key);
            if (value != null && value.isArray()) {
                return value;
            }
        }

        Iterator<JsonNode> elements = root.elements();
        while (elements.hasNext()) {
            var value = elements.next();
            if (value.isArray()) {
                return value;
            }
        }

        elements = root.elements();
        while (elements.hasNext()) {
            var value = elements.next();
            if (value.isObject()) {
                var nested = findArray(value);
                if (nested != null && nested.isArray()) {
                    return nested;
                }
            }
        }

        return null;
    }

    private static JsonNode field(JsonNode node, String name) {
        if (node == null || !node.isObject()) {
            return null;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String getString(JsonNode element, String... names) {
        for (var name : names) {
            var value = field(element, name);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    private static BigDecimal getDecimal(JsonNode element, String... names) {
        for (var name : names) {
            var value = field(element, name);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isNumber()) {
                return value.decimalValue();
            }
            if (value.isTextual()) {
                try {
                    return new BigDecimal(value.asText().trim());
                } catch (NumberFormatException ignored) {
                    // se ignora y se prueba el siguiente candidato
                }
            }
        }
        return null;
    }

    private static Integer getInt(JsonNode element, String... names) {
        var value = getDecimal(element, names);
        return value == null ? null : value.intValue();
    }

    private static final class Counters {
        int categoriesProcessed;
        int categoriesCreated;
        int productsProcessed;
        int productsCreated;
        int productsUpdated;
    }

    private record ProviderCategory(int providerId, String name) {
    }

    private record ProviderProduct(
        String sku,
        String longDescription,
        String shortDescription,
        BigDecimal salePrice,
        BigDecimal customerPrice,
        BigDecimal units,
        Integer categoryId) {
    }

    private static final class UnauthorizedException extends RuntimeException {
    }
}
