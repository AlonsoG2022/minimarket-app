package com.minimarket.api.service;

import com.minimarket.api.dto.*;
import com.minimarket.api.entity.Product;
import com.minimarket.api.repository.CategoryRepository;
import com.minimarket.api.repository.ProductRepository;
import com.minimarket.api.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {
    private static final int FIXED_MINIMUM_STOCK = 5;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
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
        product.setBarcode(normalizeOptional(dto.barcode()));
        product.setPurchaseBarcode(normalizeOptional(dto.purchaseBarcode()));
        product.setDescription(dto.description() != null ? dto.description().trim() : null);
        product.setPrice(dto.price());
        product.setCost(java.math.BigDecimal.ZERO);
        product.setStock(dto.stock());
        product.setMinimumStock(FIXED_MINIMUM_STOCK);
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

        var barcodeError = validateBarcodes(dto, id);
        if (barcodeError != null) {
            return ServiceResult.failure(barcodeError);
        }

        if (categoryRepository.findById(dto.categoryId()).isEmpty()) {
            return ServiceResult.failure("La categoria seleccionada no existe.");
        }

        product.setName(dto.name().trim());
        product.setBarcode(normalizeOptional(dto.barcode()));
        product.setPurchaseBarcode(normalizeOptional(dto.purchaseBarcode()));
        product.setDescription(dto.description() != null ? dto.description().trim() : null);
        product.setPrice(dto.price());
        product.setStock(dto.stock());
        product.setMinimumStock(FIXED_MINIMUM_STOCK);
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

            var category = categoryRepository.findByNameIgnoreCase(categoryName).orElse(null);
            if (category == null) {
                errors.add(new ProductImportErrorDto(rowNumber, "La categoria '%s' no existe.".formatted(categoryName)));
                continue;
            }

            var product = new Product();
            product.setName(name);
            product.setSku(generateSku(category.getName()));
            product.setBarcode(null);
            product.setPurchaseBarcode(null);
            product.setDescription(null);
            product.setPrice(row.price());
            product.setCost(java.math.BigDecimal.ZERO);
            product.setStock(stock);
            product.setMinimumStock(FIXED_MINIMUM_STOCK);
            product.setSalesUnitName("unidad");
            product.setPurchaseUnitName("unidad");
            product.setUnitsPerPurchaseUnit(1);
            product.setIsActive(true);
            product.setCategoryId(category.getId());

            productRepository.save(product);
            createdCount++;
        }

        return new ProductImportResultDto(createdCount, errors);
    }

    @Transactional
    public ServiceResult<Void> delete(Integer id) {
        var product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ServiceResult.failure("Producto no encontrado.");
        }

        productRepository.delete(product);
        return ServiceResult.success(null);
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
        var barcode = normalizeOptional(dto.barcode());
        var purchaseBarcode = normalizeOptional(dto.purchaseBarcode());

        if (barcode != null) {
            var exists = excludingId == null
                ? productRepository.existsByBarcodeIgnoreCase(barcode)
                : productRepository.existsByBarcodeIgnoreCaseAndIdNot(barcode, excludingId);
            if (exists) {
                return "El codigo de barras de venta ya existe.";
            }

            var existingPurchaseBarcode = productRepository.findWithCategoryByPurchaseBarcode(barcode).orElse(null);
            if (existingPurchaseBarcode != null
                && !existingPurchaseBarcode.getId().equals(excludingId)
                && (existingPurchaseBarcode.getBarcode() == null || !existingPurchaseBarcode.getBarcode().equalsIgnoreCase(barcode))) {
                return "El codigo de barras de venta ya esta siendo usado como codigo de compra.";
            }
        }

        if (purchaseBarcode != null) {
            var exists = excludingId == null
                ? productRepository.existsByPurchaseBarcodeIgnoreCase(purchaseBarcode)
                : productRepository.existsByPurchaseBarcodeIgnoreCaseAndIdNot(purchaseBarcode, excludingId);
            if (exists) {
                return "El codigo de barras de compra ya existe.";
            }

            var existingBarcode = productRepository.findWithCategoryByBarcode(purchaseBarcode).orElse(null);
            if (existingBarcode != null
                && !existingBarcode.getId().equals(excludingId)
                && (existingBarcode.getPurchaseBarcode() == null || !existingBarcode.getPurchaseBarcode().equalsIgnoreCase(purchaseBarcode))) {
                return "El codigo de barras de compra ya esta siendo usado como codigo de venta.";
            }
        }

        return null;
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
