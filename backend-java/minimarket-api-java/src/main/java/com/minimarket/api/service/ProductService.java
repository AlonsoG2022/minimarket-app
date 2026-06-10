package com.minimarket.api.service;

import com.minimarket.api.dto.*;
import com.minimarket.api.entity.Product;
import com.minimarket.api.repository.CategoryRepository;
import com.minimarket.api.repository.CompanyRepository;
import com.minimarket.api.repository.ProductRepository;
import com.minimarket.api.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {
    private static final int DEFAULT_MINIMUM_STOCK = 5;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CompanyRepository companyRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository, CompanyRepository companyRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.companyRepository = companyRepository;
    }

    private int getConfiguredMinimumStock() {
        return companyRepository.findById(1)
            .map(com.minimarket.api.entity.Company::getMinimumStock)
            .orElse(DEFAULT_MINIMUM_STOCK);
    }

    public List<ProductDto> getAll() {
        return productRepository.findAllByOrderByNameAsc()
            .stream()
            .map(DtoMapper::toDto)
            .toList();
    }

    public ProductDto getById(Integer id) {
        return productRepository.findWithCategoryById(id)
            .map(DtoMapper::toDto)
            .orElse(null);
    }

    @Transactional
    public ServiceResult<ProductDto> create(SaveProductDto dto) {
        if (dto.stock() < 0 || dto.price().signum() <= 0 || dto.unitsPerPurchaseUnit() == null || dto.unitsPerPurchaseUnit() <= 0) {
            return ServiceResult.failure("Los datos del producto no son validos.");
        }

        var resolvedBarcode = resolveUnifiedBarcode(dto.barcode(), dto.purchaseBarcode());
        if (!resolvedBarcode.success()) {
            return ServiceResult.failure(resolvedBarcode.error());
        }

        var parsedExpirationDate = parseExpirationDate(dto.expirationDate());
        if (!parsedExpirationDate.success()) {
            return ServiceResult.failure(parsedExpirationDate.error());
        }

        var barcodeError = validateBarcodes(dto, null);
        if (barcodeError != null) {
            return ServiceResult.failure(barcodeError);
        }

        var category = categoryRepository.findById(dto.categoryId()).orElse(null);
        if (category == null) {
            return ServiceResult.failure("La categoria seleccionada no existe.");
        }

        var product = new Product();
        product.setName(dto.name().trim());
        product.setSku(generateSku(category.getName()));
        product.setBarcode(resolvedBarcode.data());
        product.setPurchaseBarcode(resolvedBarcode.data());
        product.setDescription(dto.description() != null ? dto.description().trim() : null);
        product.setPrice(dto.price());
        product.setCost(java.math.BigDecimal.ZERO);
        product.setStock(dto.stock());
        product.setMinimumStock(getConfiguredMinimumStock());
        product.setExpirationDate(parsedExpirationDate.data());
        product.setSalesUnitName(normalizeUnitName(dto.salesUnitName()));
        product.setPurchaseUnitName(normalizeUnitName(dto.purchaseUnitName()));
        product.setUnitsPerPurchaseUnit(dto.unitsPerPurchaseUnit());
        product.setIsActive(dto.isActive());
        product.setCategoryId(dto.categoryId());

        var saved = productRepository.save(product);
        var created = productRepository.findWithCategoryById(saved.getId()).orElse(saved);
        return ServiceResult.success(DtoMapper.toDto(created));
    }

    @Transactional
    public ServiceResult<ProductDto> update(Integer id, SaveProductDto dto) {
        var product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ServiceResult.failure("Producto no encontrado.");
        }

        if (dto.stock() < 0 || dto.price().signum() <= 0 || dto.unitsPerPurchaseUnit() == null || dto.unitsPerPurchaseUnit() <= 0) {
            return ServiceResult.failure("Los datos del producto no son validos.");
        }

        var resolvedBarcode = resolveUnifiedBarcode(dto.barcode(), dto.purchaseBarcode());
        if (!resolvedBarcode.success()) {
            return ServiceResult.failure(resolvedBarcode.error());
        }

        var parsedExpirationDate = parseExpirationDate(dto.expirationDate());
        if (!parsedExpirationDate.success()) {
            return ServiceResult.failure(parsedExpirationDate.error());
        }

        var barcodeError = validateBarcodes(dto, id);
        if (barcodeError != null) {
            return ServiceResult.failure(barcodeError);
        }

        if (categoryRepository.findById(dto.categoryId()).isEmpty()) {
            return ServiceResult.failure("La categoria seleccionada no existe.");
        }

        product.setName(dto.name().trim());
        product.setBarcode(resolvedBarcode.data());
        product.setPurchaseBarcode(resolvedBarcode.data());
        product.setDescription(dto.description() != null ? dto.description().trim() : null);
        product.setPrice(dto.price());
        product.setStock(dto.stock());
        product.setMinimumStock(getConfiguredMinimumStock());
        product.setExpirationDate(parsedExpirationDate.data());
        product.setSalesUnitName(normalizeUnitName(dto.salesUnitName()));
        product.setPurchaseUnitName(normalizeUnitName(dto.purchaseUnitName()));
        product.setUnitsPerPurchaseUnit(dto.unitsPerPurchaseUnit());
        product.setIsActive(dto.isActive());
        product.setCategoryId(dto.categoryId());

        var updated = productRepository.save(product);
        var loaded = productRepository.findWithCategoryById(updated.getId()).orElse(updated);
        return ServiceResult.success(DtoMapper.toDto(loaded));
    }

    @Transactional
    public ProductImportResultDto importRows(List<ProductImportRowDto> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ProductImportResultDto(0, List.of(new ProductImportErrorDto(0, "El archivo no contiene filas validas.")));
        }

        var errors = new ArrayList<ProductImportErrorDto>();
        var createdCount = 0;
        var minimumStock = getConfiguredMinimumStock();

        for (var row : rows) {
            var rowNumber = row.rowNumber() != null ? row.rowNumber() : 0;
            var name = row.name() != null ? row.name().trim() : "";
            var categoryName = row.categoryName() != null ? row.categoryName().trim() : "";

            if (name.isBlank()) {
                errors.add(new ProductImportErrorDto(rowNumber, "El nombre del producto es obligatorio."));
                continue;
            }

            if (row.price() == null || row.price().signum() <= 0) {
                errors.add(new ProductImportErrorDto(rowNumber, "El precio debe ser mayor que cero."));
                continue;
            }

            if (categoryName.isBlank()) {
                errors.add(new ProductImportErrorDto(rowNumber, "La categoria es obligatoria."));
                continue;
            }

            var stock = row.stock() != null ? row.stock() : 0;
            if (stock < 0) {
                errors.add(new ProductImportErrorDto(rowNumber, "El stock no puede ser menor que cero."));
                continue;
            }

            var unitsPerPurchaseUnit = row.unitsPerPurchaseUnit() != null ? row.unitsPerPurchaseUnit() : 1;
            if (unitsPerPurchaseUnit <= 0) {
                errors.add(new ProductImportErrorDto(rowNumber, "Las unidades por compra deben ser mayores que cero."));
                continue;
            }

            var category = categoryRepository.findByNameIgnoreCase(categoryName).orElse(null);
            if (category == null) {
                errors.add(new ProductImportErrorDto(rowNumber, "La categoria '%s' no existe.".formatted(categoryName)));
                continue;
            }

            var resolvedBarcode = resolveUnifiedBarcode(row.barcode(), row.barcode());
            if (!resolvedBarcode.success()) {
                errors.add(new ProductImportErrorDto(rowNumber, resolvedBarcode.error()));
                continue;
            }

            if (resolvedBarcode.data() != null) {
                var barcodeExists = productRepository.existsByBarcodeIgnoreCase(resolvedBarcode.data())
                    || productRepository.existsByPurchaseBarcodeIgnoreCase(resolvedBarcode.data());
                if (barcodeExists) {
                    errors.add(new ProductImportErrorDto(rowNumber, "El codigo de barras '%s' ya existe.".formatted(resolvedBarcode.data())));
                    continue;
                }
            }

            var parsedExpirationDate = parseExpirationDate(row.expirationDate());
            if (!parsedExpirationDate.success()) {
                errors.add(new ProductImportErrorDto(rowNumber, parsedExpirationDate.error()));
                continue;
            }

            var product = new Product();
            product.setName(name);
            product.setSku(generateSku(category.getName()));
            product.setBarcode(resolvedBarcode.data());
            product.setPurchaseBarcode(resolvedBarcode.data());
            product.setDescription(normalizeOptional(row.description()));
            product.setPrice(row.price());
            product.setCost(java.math.BigDecimal.ZERO);
            product.setStock(stock);
            product.setMinimumStock(minimumStock);
            product.setExpirationDate(parsedExpirationDate.data());
            product.setSalesUnitName(normalizeUnitName(row.salesUnitName()));
            product.setPurchaseUnitName(normalizeUnitName(row.purchaseUnitName()));
            product.setUnitsPerPurchaseUnit(unitsPerPurchaseUnit);
            product.setIsActive(row.isActive() == null || row.isActive());
            product.setCategoryId(category.getId());

            productRepository.save(product);
            createdCount++;
        }

        return new ProductImportResultDto(createdCount, errors);
    }

    @Transactional
    public ServiceResult<ProductDeleteResultDto> delete(Integer id) {
        var product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ServiceResult.failure("Producto no encontrado.");
        }

        var hasMovements = productRepository.existsSaleDetailsByProductId(id)
            || productRepository.existsPurchaseDetailsByProductId(id);
        if (hasMovements) {
            product.setIsActive(false);
            productRepository.save(product);
            return ServiceResult.success(new ProductDeleteResultDto(
                "El producto tiene compras o ventas registradas. Se desactivo en lugar de eliminarlo.",
                true
            ));
        }

        productRepository.delete(product);
        return ServiceResult.success(new ProductDeleteResultDto("Producto eliminado correctamente.", false));
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

    private String validateBarcodes(SaveProductDto dto, Integer excludingId) {
        var resolvedBarcode = resolveUnifiedBarcode(dto.barcode(), dto.purchaseBarcode());
        if (!resolvedBarcode.success()) {
            return resolvedBarcode.error();
        }

        var barcode = resolvedBarcode.data();

        if (barcode != null) {
            var exists = excludingId == null
                ? productRepository.existsByBarcodeIgnoreCase(barcode)
                : productRepository.existsByBarcodeIgnoreCaseAndIdNot(barcode, excludingId);
            if (exists) {
                return "El codigo de barras ya existe.";
            }

            var existsInPurchaseBarcode = excludingId == null
                ? productRepository.existsByPurchaseBarcodeIgnoreCase(barcode)
                : productRepository.existsByPurchaseBarcodeIgnoreCaseAndIdNot(barcode, excludingId);
            if (existsInPurchaseBarcode) {
                return "El codigo de barras ya existe.";
            }
        }

        return null;
    }

    private ServiceResult<String> resolveUnifiedBarcode(String barcodeValue, String purchaseBarcodeValue) {
        var barcode = normalizeOptional(barcodeValue);
        var purchaseBarcode = normalizeOptional(purchaseBarcodeValue);

        if (barcode != null && purchaseBarcode != null && !barcode.equalsIgnoreCase(purchaseBarcode)) {
            return ServiceResult.failure("Usa un unico codigo de barras para compras y ventas.");
        }

        return ServiceResult.success(barcode != null ? barcode : purchaseBarcode);
    }

    private ServiceResult<LocalDate> parseExpirationDate(String rawValue) {
        var value = normalizeOptional(rawValue);
        if (value == null) {
            return ServiceResult.success(null);
        }

        var patterns = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
        );

        for (var formatter : patterns) {
            try {
                return ServiceResult.success(LocalDate.parse(value, formatter));
            } catch (DateTimeParseException ignored) {
            }
        }

        return ServiceResult.failure("La fecha de caducidad no tiene un formato valido.");
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        var trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalizeUnitName(String value) {
        if (value == null) {
            return "unidad";
        }

        var trimmed = value.trim();
        return trimmed.isBlank() ? "unidad" : trimmed;
    }
}
