package com.minimarket.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.api.dto.*;
import com.minimarket.api.entity.Category;
import com.minimarket.api.entity.Product;
import com.minimarket.api.entity.SupplierProduct;
import com.minimarket.api.repository.CategoryRepository;
import com.minimarket.api.repository.CompanyRepository;
import com.minimarket.api.repository.ProductRepository;
import com.minimarket.api.repository.SupplierProductRepository;
import com.minimarket.api.repository.SupplierRepository;
import com.minimarket.api.util.CryptoHelper;
import com.minimarket.api.util.ShortNameGenerator;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Lee la foto de una boleta de compra con IA (Claude vision), propone productos/costos,
// y al confirmar guarda el costo por proveedor (ProveedorProducto) y crea/actualiza productos.
@Service
public class ReceiptService {

    private static final String CLAUDE_URL = "https://api.anthropic.com/v1/messages";
    private static final String CLAUDE_MODEL = "claude-sonnet-5";
    private static final BigDecimal IGV = new BigDecimal("1.18");
    private static final int DEFAULT_MINIMUM_STOCK = 5;

    private static final Set<String> STOP_WORDS = Set.of(
        "pet","lata","latas","vidrio","retornable","no","botella","unidad","unidades","uni",
        "con","gas","sin","de","del","la","el","los","las","x","pack","caja","bolsa","gr","g","ml");

    private static final String PROMPT =
        "Eres un lector de boletas de compra de un minimarket en Peru. Lee la imagen y devuelve SOLO un JSON " +
        "valido (sin texto adicional, sin markdown) con esta forma exacta: " +
        "{\"proveedor\":{\"nombre\":\"\",\"ruc\":\"\"},\"items\":[{\"descripcion\":\"\",\"cantidad\":1," +
        "\"precioUnitario\":0,\"unidadesPorPaquete\":1,\"categoria\":\"\"}]}. " +
        "Reglas: precioUnitario es el precio unitario SIN IGV tal como aparece en la columna Precio. " +
        "unidadesPorPaquete: si el item viene en tira/pack (ej. x6, x12, TRAx12UND, PCKx6UND) pon ese numero; " +
        "si es unidad suelta pon 1. categoria: clasifica el producto (Abarrotes, Bebidas, Gaseosas, Aguas, Jugos, " +
        "Cuidado personal, Limpieza, Snacks, Golosinas, Galletas, Licores, Cervezas, Lacteos). Si un dato no esta usa " +
        "0 o cadena vacia. No inventes items que no esten en la boleta.";

    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ReceiptService(CompanyRepository companyRepository,
                          ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          SupplierRepository supplierRepository,
                          SupplierProductRepository supplierProductRepository) {
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.supplierProductRepository = supplierProductRepository;
    }

    public ServiceResult<ReceiptScanResultDto> scan(ReceiptScanRequestDto request) {
        if (request.imageBase64() == null || request.imageBase64().isBlank()) {
            return ServiceResult.failure("No se recibio la imagen de la boleta.");
        }

        var company = companyRepository.findById(1).orElse(null);
        if (company == null || company.getAiReceiptKey() == null || company.getAiReceiptKey().isBlank()) {
            return ServiceResult.failure("Falta configurar la clave de IA en Configuracion para leer boletas.");
        }

        String apiKey;
        try {
            apiKey = CryptoHelper.decrypt(company.getAiReceiptKey());
        } catch (RuntimeException ex) {
            return ServiceResult.failure("La clave de IA guardada no es valida. Vuelve a guardarla en Configuracion.");
        }

        JsonNode parsed;
        try {
            var text = callClaude(apiKey, request.imageBase64(), request.mediaType() == null ? "image/jpeg" : request.mediaType());
            parsed = parseJsonFromText(text);
        } catch (UnauthorizedException ex) {
            return ServiceResult.failure("La clave de IA fue rechazada. Verifica que sea correcta y tenga saldo.");
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ServiceResult.failure("No se pudo contactar la IA: " + ex.getMessage());
        }

        if (parsed == null) {
            return ServiceResult.failure("La IA no devolvio un resultado legible. Intenta con una foto mas clara.");
        }

        var warnings = new ArrayList<String>();
        var prov = parsed.get("proveedor");
        var supplierName = orEmpty(getString(prov, "nombre"));
        var supplierRuc = getString(prov, "ruc");
        Integer supplierId = null;
        if (supplierRuc != null && !supplierRuc.isBlank()) {
            supplierId = supplierRepository.findByDocumentNumber(supplierRuc.trim()).map(s -> s.getId()).orElse(null);
        }

        var activeProducts = productRepository.findAllByOrderByNameAsc().stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsActive())).toList();

        var lines = new ArrayList<ReceiptScanLineDto>();
        var items = parsed.get("items");
        if (items != null && items.isArray()) {
            for (var item : items) {
                var description = orEmpty(getString(item, "descripcion")).trim();
                if (description.isBlank()) {
                    continue;
                }

                var priceNet = orZero(getDecimal(item, "precioUnitario"));
                var packUnits = Math.max(1, getInt(item, "unidadesPorPaquete"));
                var quantity = getDecimal(item, "cantidad");
                if (quantity == null) {
                    quantity = BigDecimal.ONE;
                }
                var category = orEmpty(getString(item, "categoria")).trim();
                if (category.isBlank()) {
                    category = "Abarrotes";
                }

                var unitCost = priceNet.multiply(IGV).divide(BigDecimal.valueOf(packUnits), 2, RoundingMode.HALF_UP);
                var suggestedName = truncate(description, 150);
                var suggestedShort = truncate(ShortNameGenerator.generate(suggestedName), 60);
                var suggestedPrice = unitCost.multiply(marginFactor(category)).setScale(1, RoundingMode.HALF_UP);

                var match = bestMatch(suggestedName, activeProducts);

                lines.add(new ReceiptScanLineDto(
                    description, quantity, packUnits, unitCost,
                    suggestedName, suggestedShort, category, suggestedPrice,
                    match.id(), match.name(), match.score()));
            }
        }

        if (lines.isEmpty()) {
            warnings.add("No se detectaron productos en la boleta.");
        }

        return ServiceResult.success(new ReceiptScanResultDto(supplierName, supplierRuc, supplierId, lines, warnings));
    }

    public ServiceResult<ReceiptConfirmResultDto> confirm(ReceiptConfirmRequestDto request) {
        if (request.supplierId() == null) {
            return ServiceResult.failure("El proveedor seleccionado no existe.");
        }
        var supplier = supplierRepository.findById(request.supplierId()).orElse(null);
        if (supplier == null) {
            return ServiceResult.failure("El proveedor seleccionado no existe.");
        }
        if (request.items() == null || request.items().isEmpty()) {
            return ServiceResult.failure("No hay items para guardar.");
        }

        var minimumStock = companyRepository.findById(1)
            .map(com.minimarket.api.entity.Company::getMinimumStock)
            .orElse(DEFAULT_MINIMUM_STOCK);
        var expiration = LocalDate.now().plusYears(2);
        var warnings = new ArrayList<String>();
        var created = 0;
        var updated = 0;
        var costsRecorded = 0;

        for (var item : request.items()) {
            var action = item.action() == null ? "" : item.action().trim().toLowerCase();
            if (action.equals("skip")) {
                continue;
            }

            var cost = orZero(item.cost());

            if (action.equals("create")) {
                var categoryName = item.categoryName() == null || item.categoryName().isBlank() ? "Abarrotes" : item.categoryName().trim();
                var category = categoryRepository.findByNameIgnoreCase(categoryName).orElse(null);
                if (category == null) {
                    var nueva = new Category();
                    nueva.setName(truncate(categoryName, 100));
                    nueva.setIsActive(true);
                    category = categoryRepository.save(nueva);
                }

                var name = truncate(item.name() == null || item.name().isBlank() ? "Producto" : item.name(), 150);
                var product = new Product();
                product.setName(name);
                product.setShortName(truncate(item.shortName() == null || item.shortName().isBlank()
                    ? ShortNameGenerator.generate(name) : item.shortName(), 60));
                product.setSku(generateSku(category.getName()));
                product.setBarcode(null);
                product.setPurchaseBarcode(null);
                product.setDescription(null);
                product.setPrice(item.price() != null && item.price().signum() > 0 ? item.price() : cost);
                product.setCost(cost);
                product.setStock(0);
                product.setMinimumStock(minimumStock);
                product.setExpirationDate(expiration);
                product.setSalesUnitName("Unidad");
                product.setPurchaseUnitName("Unidad");
                product.setUnitsPerPurchaseUnit(1);
                product.setIsActive(true);
                product.setCategoryId(category.getId());

                var saved = productRepository.save(product);
                recordCost(supplier.getId(), saved.getId(), cost);
                created++;
                costsRecorded++;
            } else if (action.equals("match") && item.productId() != null) {
                var product = productRepository.findById(item.productId()).orElse(null);
                if (product == null) {
                    warnings.add("No se encontro el producto a actualizar (id " + item.productId() + ").");
                    continue;
                }
                product.setCost(cost);
                productRepository.save(product);
                recordCost(supplier.getId(), product.getId(), cost);
                updated++;
                costsRecorded++;
            }
        }

        return ServiceResult.success(new ReceiptConfirmResultDto(created, updated, costsRecorded, warnings));
    }

    private void recordCost(Integer supplierId, Integer productId, BigDecimal cost) {
        var entry = new SupplierProduct();
        entry.setSupplierId(supplierId);
        entry.setProductId(productId);
        entry.setLastCost(cost);
        entry.setDate(LocalDateTime.now());
        supplierProductRepository.save(entry);
    }

    private String callClaude(String apiKey, String imageBase64, String mediaType) throws IOException, InterruptedException {
        var image = Map.of("type", "image", "source",
            Map.of("type", "base64", "media_type", mediaType, "data", imageBase64));
        var textBlock = Map.of("type", "text", "text", PROMPT);
        var message = Map.of("role", "user", "content", List.of(image, textBlock));
        var body = Map.of("model", CLAUDE_MODEL, "max_tokens", 4000, "messages", List.of(message));
        var json = objectMapper.writeValueAsString(body);

        var httpRequest = HttpRequest.newBuilder(URI.create(CLAUDE_URL))
            .timeout(Duration.ofSeconds(120))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        var status = response.statusCode();
        if (status == 401 || status == 403) {
            throw new UnauthorizedException();
        }
        if (status >= 400) {
            throw new IOException("HTTP " + status);
        }

        var root = objectMapper.readTree(response.body());
        var sb = new StringBuilder();
        var content = root.get("content");
        if (content != null && content.isArray()) {
            for (var block : content) {
                var type = block.get("type");
                if (type != null && "text".equals(type.asText()) && block.get("text") != null) {
                    sb.append(block.get("text").asText());
                }
            }
        }
        return sb.toString();
    }

    private JsonNode parseJsonFromText(String text) throws IOException {
        var trimmed = text == null ? "" : text.trim();
        var start = trimmed.indexOf('{');
        var end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            trimmed = trimmed.substring(start, end + 1);
        }
        if (trimmed.isBlank()) {
            return null;
        }
        return objectMapper.readTree(trimmed);
    }

    private Match bestMatch(String name, List<Product> products) {
        var target = tokenize(name);
        Integer bestId = null;
        String bestName = null;
        double best = 0;
        for (var product : products) {
            var score = jaccard(target, tokenize(product.getName()));
            if (score > best) {
                best = score;
                bestId = product.getId();
                bestName = product.getName();
            }
        }
        return best >= 0.5 ? new Match(bestId, bestName, best) : new Match(null, null, best);
    }

    private static Set<String> tokenize(String value) {
        var normalized = stripAccents(value).toLowerCase().replaceAll("[^a-z0-9]+", " ");
        var result = new HashSet<String>();
        for (var token : normalized.split(" ")) {
            if (token.length() >= 2 && !STOP_WORDS.contains(token) && !token.matches("\\d+")) {
                result.add(token);
            }
        }
        return result;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int inter = 0;
        for (var t : a) {
            if (b.contains(t)) {
                inter++;
            }
        }
        int union = a.size() + b.size() - inter;
        return union == 0 ? 0 : (double) inter / union;
    }

    private static BigDecimal marginFactor(String category) {
        var c = stripAccents(category).toLowerCase();
        if (c.contains("gaseosa") || c.contains("agua") || c.contains("jugo") || c.contains("isoton") || c.contains("energiz") || c.contains("bebida"))
            return new BigDecimal("1.35");
        if (c.contains("snack") || c.contains("golosina") || c.contains("galleta") || c.contains("chocolate") || c.contains("piqueo"))
            return new BigDecimal("1.40");
        if (c.contains("cerveza"))
            return new BigDecimal("1.25");
        if (c.contains("licor"))
            return new BigDecimal("1.22");
        if (c.contains("cuidado") || c.contains("limpieza") || c.contains("higiene") || c.contains("personal") || c.contains("hogar"))
            return new BigDecimal("1.30");
        return new BigDecimal("1.18");
    }

    private String generateSku(String categoryName) {
        var prefix = buildCategoryPrefix(categoryName);
        var next = productRepository.findBySkuStartingWith(prefix + "-").stream()
            .map(Product::getSku).mapToInt(this::parseSkuSequence).max().orElse(0) + 1;
        return "%s-%06d".formatted(prefix, next);
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

    private static String buildCategoryPrefix(String categoryName) {
        var normalized = stripAccents(categoryName).replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (normalized.isBlank()) {
            return "CAT";
        }
        var prefix = normalized.length() >= 3 ? normalized.substring(0, 3) : normalized;
        return (prefix + "XXX").substring(0, 3);
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

    private static String stripAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static String getString(JsonNode node, String name) {
        if (node == null || !node.isObject()) {
            return null;
        }
        var value = node.get(name);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static BigDecimal getDecimal(JsonNode node, String name) {
        if (node == null || !node.isObject()) {
            return null;
        }
        var value = node.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        try {
            return new BigDecimal(value.asText().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static int getInt(JsonNode node, String name) {
        var value = getDecimal(node, name);
        return value == null ? 1 : value.intValue();
    }

    private record Match(Integer id, String name, Double score) {
    }

    private static final class UnauthorizedException extends RuntimeException {
    }
}
