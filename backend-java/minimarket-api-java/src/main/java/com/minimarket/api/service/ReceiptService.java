package com.minimarket.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.api.dto.*;
import com.minimarket.api.entity.CashMovement;
import com.minimarket.api.entity.Category;
import com.minimarket.api.entity.Product;
import com.minimarket.api.entity.Purchase;
import com.minimarket.api.entity.PurchaseDetail;
import com.minimarket.api.entity.Supplier;
import com.minimarket.api.entity.SupplierProduct;
import com.minimarket.api.repository.CashSessionRepository;
import com.minimarket.api.repository.CategoryRepository;
import com.minimarket.api.repository.CompanyRepository;
import com.minimarket.api.repository.ProductRepository;
import com.minimarket.api.repository.PurchaseRepository;
import com.minimarket.api.repository.SupplierProductRepository;
import com.minimarket.api.repository.SupplierRepository;
import com.minimarket.api.repository.UserRepository;
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
        "{\"proveedor\":{\"nombre\":\"\",\"ruc\":\"\"},\"numeroBoleta\":\"\",\"items\":[{\"descripcion\":\"\",\"cantidad\":1," +
        "\"total\":0,\"unidadesPorPaquete\":1,\"categoria\":\"\"}]}. " +
        "numeroBoleta es el numero del comprobante. cantidad es el numero de la columna Cantidad de esa linea. " +
        "total es el monto de la columna Total de esa linea (lo que se pago por esa linea, ya con IGV si aplica). " +
        "unidadesPorPaquete: pon 1 por defecto. NO lo deduzcas del nombre del producto: textos como '20X200GR', " +
        "'12X270GR' o '30X24UND' son solo el formato/presentacion, NO la cantidad comprada. Solo pon un numero mayor a 1 " +
        "si la unidad de compra es claramente un paquete que el minimarket abre para vender por unidad. " +
        "categoria: clasifica el producto (Abarrotes, Bebidas, Gaseosas, Aguas, Jugos, Cuidado personal, Limpieza, " +
        "Snacks, Golosinas, Galletas, Licores, Cervezas, Lacteos). Si un dato no esta usa 0 o cadena vacia. " +
        "No inventes items que no esten en la boleta.";

    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final PurchaseRepository purchaseRepository;
    private final CashSessionRepository cashSessionRepository;
    private final UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ReceiptService(CompanyRepository companyRepository,
                          ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          SupplierRepository supplierRepository,
                          SupplierProductRepository supplierProductRepository,
                          PurchaseRepository purchaseRepository,
                          CashSessionRepository cashSessionRepository,
                          UserRepository userRepository) {
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.supplierProductRepository = supplierProductRepository;
        this.purchaseRepository = purchaseRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.userRepository = userRepository;
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
        var invoiceNumber = getString(parsed, "numeroBoleta");
        var invoiceAlreadyRegistered = invoiceNumber != null && !invoiceNumber.isBlank()
            && purchaseRepository.existsByInvoiceNumber(invoiceNumber.trim());
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

                var lineTotal = orZero(getDecimal(item, "total"));
                var packUnits = Math.max(1, getInt(item, "unidadesPorPaquete"));
                var quantity = getDecimal(item, "cantidad");
                if (quantity == null) {
                    quantity = BigDecimal.ONE;
                }
                var category = orEmpty(getString(item, "categoria")).trim();
                if (category.isBlank()) {
                    category = "Abarrotes";
                }

                // Costo por unidad = lo pagado en esa linea (columna Total) entre las unidades que entran.
                var totalUnitsForCost = quantity.multiply(BigDecimal.valueOf(packUnits));
                var unitCost = totalUnitsForCost.signum() > 0
                    ? lineTotal.divide(totalUnitsForCost, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                var suggestedName = truncate(description, 150);
                var suggestedShort = truncate(ShortNameGenerator.generate(suggestedName), 60);
                var suggestedPrice = unitCost.multiply(marginFactor(category)).setScale(1, RoundingMode.HALF_UP);

                var match = bestMatch(suggestedName, activeProducts);

                lines.add(new ReceiptScanLineDto(
                    description, quantity, packUnits, unitCost, lineTotal,
                    suggestedName, suggestedShort, category, suggestedPrice,
                    match.id(), match.name(), match.score()));
            }
        }

        if (lines.isEmpty()) {
            warnings.add("No se detectaron productos en la boleta.");
        }

        return ServiceResult.success(new ReceiptScanResultDto(supplierName, supplierRuc, supplierId, invoiceNumber, invoiceAlreadyRegistered, lines, warnings));
    }

    public ServiceResult<ReceiptConfirmResultDto> confirm(ReceiptConfirmRequestDto request) {
        if (request.items() == null || request.items().isEmpty()) {
            return ServiceResult.failure("No hay items para guardar.");
        }

        if (request.userId() == null) {
            return ServiceResult.failure("El usuario no es valido.");
        }
        var user = userRepository.findById(request.userId()).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            return ServiceResult.failure("El usuario no es valido.");
        }

        // Anti-duplicado: si esa boleta ya se cargo, no se registra de nuevo (evita duplicar stock).
        var invoice = request.invoiceNumber() == null || request.invoiceNumber().isBlank()
            ? null : truncate(request.invoiceNumber().trim(), 50);
        if (invoice != null && purchaseRepository.existsByInvoiceNumber(invoice)) {
            return ServiceResult.failure("La boleta '" + invoice + "' ya fue cargada antes. No se registro de nuevo.");
        }

        // Proveedor: usar el existente o crear uno nuevo desde la boleta.
        Supplier supplier;
        if (request.supplierId() != null && request.supplierId() > 0) {
            supplier = supplierRepository.findById(request.supplierId()).orElse(null);
            if (supplier == null) {
                return ServiceResult.failure("El proveedor seleccionado no existe.");
            }
        } else {
            var newName = request.newSupplierName() == null ? "" : request.newSupplierName().trim();
            if (newName.isBlank()) {
                return ServiceResult.failure("Falta el nombre del proveedor nuevo.");
            }
            var ruc = request.newSupplierRuc() == null ? "" : request.newSupplierRuc().trim();
            supplier = ruc.isBlank() ? null : supplierRepository.findByDocumentNumber(ruc).orElse(null);
            if (supplier == null) {
                var nuevo = new Supplier();
                nuevo.setName(truncate(newName, 150));
                nuevo.setDocumentNumber(ruc.isBlank() ? null : truncate(ruc, 30));
                nuevo.setIsActive(true);
                supplier = supplierRepository.save(nuevo);
            }
        }

        var minimumStock = companyRepository.findById(1)
            .map(com.minimarket.api.entity.Company::getMinimumStock)
            .orElse(DEFAULT_MINIMUM_STOCK);
        var expiration = LocalDate.now().plusYears(2);
        var warnings = new ArrayList<String>();
        var gastos = new ArrayList<Gasto>();   // flete/reparto -> gasto en la caja del dia
        var created = 0;
        var updated = 0;

        var purchase = new Purchase();
        purchase.setPurchaseDate(LocalDateTime.now());
        purchase.setSupplierId(supplier.getId());
        purchase.setUserId(user.getId());
        purchase.setInvoiceNumber(invoice);
        purchase.setNotes("Cargada desde foto de boleta");
        purchase.setSubTotal(BigDecimal.ZERO);
        purchase.setIgv(BigDecimal.ZERO);
        purchase.setTotal(BigDecimal.ZERO);

        for (var item : request.items()) {
            var action = item.action() == null ? "" : item.action().trim().toLowerCase();
            if (action.equals("skip")) {
                continue;
            }

            var quantity = Math.max(1, item.quantity() == null ? 1 : item.quantity());
            var packUnits = Math.max(1, item.packUnits() == null ? 1 : item.packUnits());
            var totalUnits = quantity * packUnits;
            var unitCost = orZero(item.cost());
            var packageCost = unitCost.multiply(BigDecimal.valueOf(packUnits)).setScale(2, RoundingMode.HALF_UP);
            var subtotal = unitCost.multiply(BigDecimal.valueOf(totalUnits)).setScale(2, RoundingMode.HALF_UP);

            // Flete / reparto: no es producto, se registra como gasto en la caja abierta.
            if (action.equals("gasto")) {
                if (subtotal.signum() > 0) {
                    var nombre = item.name() == null || item.name().isBlank() ? "Flete/reparto" : item.name().trim();
                    gastos.add(new Gasto(nombre, subtotal));
                }
                continue;
            }

            Product product;
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
                // Regalo nuevo (sin precio): se crea OCULTO para no venderlo en 0 hasta ponerle precio.
                var salePrice = item.price() != null && item.price().signum() > 0 ? item.price() : unitCost;
                var isActive = salePrice.signum() > 0;
                var nuevo = new Product();
                nuevo.setName(name);
                nuevo.setShortName(truncate(item.shortName() == null || item.shortName().isBlank()
                    ? ShortNameGenerator.generate(name) : item.shortName(), 60));
                nuevo.setSku(generateSku(category.getName()));
                nuevo.setBarcode(null);
                nuevo.setPurchaseBarcode(null);
                nuevo.setDescription(null);
                nuevo.setPrice(salePrice);
                nuevo.setCost(unitCost);
                nuevo.setStock(totalUnits);
                nuevo.setMinimumStock(minimumStock);
                nuevo.setExpirationDate(expiration);
                nuevo.setSalesUnitName("Unidad");
                nuevo.setPurchaseUnitName("Unidad");
                nuevo.setUnitsPerPurchaseUnit(1);
                nuevo.setIsActive(isActive);
                nuevo.setCategoryId(category.getId());
                product = productRepository.save(nuevo);
                created++;
                if (!isActive) {
                    warnings.add("'" + name + "' se creo OCULTO (sin precio) porque llego de regalo. Ponle precio en Productos y activalo para venderlo.");
                }
            } else if (action.equals("match") && item.productId() != null) {
                var existing = productRepository.findById(item.productId()).orElse(null);
                if (existing == null) {
                    warnings.add("No se encontro el producto a actualizar (id " + item.productId() + ").");
                    continue;
                }
                existing.setStock((existing.getStock() == null ? 0 : existing.getStock()) + totalUnits);
                if (unitCost.signum() > 0) {   // regalos (costo 0) solo suman stock
                    existing.setCost(unitCost);
                }
                product = productRepository.save(existing);
                updated++;
            } else {
                continue;
            }

            var detail = new PurchaseDetail();
            detail.setPurchase(purchase);
            detail.setProductId(product.getId());
            detail.setPackageQuantity(quantity);
            detail.setUnitsPerPackage(packUnits);
            detail.setTotalUnits(totalUnits);
            detail.setPackageCost(packageCost);
            detail.setUnitCost(unitCost.setScale(2, RoundingMode.HALF_UP));
            detail.setSubtotal(subtotal);
            detail.setPurchaseUnitName("Unidad");
            detail.setBarcodeSnapshot(null);
            purchase.getDetails().add(detail);

            // Los regalos (costo 0) no se registran en el comparador de precios por proveedor.
            if (unitCost.signum() > 0) {
                recordCost(supplier.getId(), product.getId(), unitCost);
            }
        }

        if (purchase.getDetails().isEmpty()) {
            return ServiceResult.failure("No hay items validos para registrar la compra.");
        }

        var total = purchase.getDetails().stream().map(PurchaseDetail::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        var subTotal = total.divide(IGV, 2, RoundingMode.HALF_UP);
        purchase.setTotal(total);
        purchase.setSubTotal(subTotal);
        purchase.setIgv(total.subtract(subTotal).setScale(2, RoundingMode.HALF_UP));

        var savedPurchase = purchaseRepository.save(purchase);

        // Flete/reparto -> gasto en la caja abierta del usuario (si no hay caja, se avisa).
        if (!gastos.isEmpty()) {
            var openSession = cashSessionRepository
                .findFirstByUserIdAndStatusOrderByOpenedAtDesc(user.getId(), "abierta").orElse(null);
            if (openSession == null) {
                var totalGasto = gastos.stream().map(Gasto::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
                warnings.add("No hay una caja abierta: el gasto de flete/reparto (S/ " + totalGasto.setScale(2, RoundingMode.HALF_UP)
                    + ") no se registro. Abre la caja y registralo como gasto manualmente.");
            } else {
                for (var g : gastos) {
                    var movement = new CashMovement();
                    movement.setCashSessionId(openSession.getId());
                    movement.setCashSession(openSession);
                    movement.setMovementDate(LocalDateTime.now());
                    movement.setType("gasto");
                    movement.setAmount(g.amount());
                    movement.setDescription(invoice == null || invoice.isBlank()
                        ? g.name() : g.name() + " (boleta " + invoice + ")");
                    movement.setReferenceType("compra");
                    movement.setReferenceId(savedPurchase.getId());
                    openSession.getMovements().add(movement);
                }
                cashSessionRepository.save(openSession);
            }
        }

        return ServiceResult.success(new ReceiptConfirmResultDto(created, updated, savedPurchase.getId(), warnings));
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

    private record Gasto(String name, BigDecimal amount) {
    }

    private static final class UnauthorizedException extends RuntimeException {
    }
}
