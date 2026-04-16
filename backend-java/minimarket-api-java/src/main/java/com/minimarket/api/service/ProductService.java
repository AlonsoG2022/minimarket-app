package com.minimarket.api.service;

import com.minimarket.api.dto.ProductDto;
import com.minimarket.api.dto.SaveProductDto;
import com.minimarket.api.entity.Product;
import com.minimarket.api.repository.CategoryRepository;
import com.minimarket.api.repository.ProductRepository;
import com.minimarket.api.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

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
        if (dto.stock() < 0 || dto.minimumStock() < 0 || dto.price().signum() <= 0) {
            return ServiceResult.failure("Los datos del producto no son validos.");
        }

        if (categoryRepository.findById(dto.categoryId()).isEmpty()) {
            return ServiceResult.failure("La categoria seleccionada no existe.");
        }

        var normalizedSku = dto.sku().trim().toUpperCase();
        if (productRepository.existsBySkuIgnoreCase(normalizedSku)) {
            return ServiceResult.failure("Ya existe un producto con el mismo SKU.");
        }

        var product = new Product();
        product.setName(dto.name().trim());
        product.setSku(normalizedSku);
        product.setDescription(dto.description() != null ? dto.description().trim() : null);
        product.setPrice(dto.price());
        product.setStock(dto.stock());
        product.setMinimumStock(dto.minimumStock());
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

        if (categoryRepository.findById(dto.categoryId()).isEmpty()) {
            return ServiceResult.failure("La categoria seleccionada no existe.");
        }

        var normalizedSku = dto.sku().trim().toUpperCase();
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(normalizedSku, id)) {
            return ServiceResult.failure("Ya existe un producto con el mismo SKU.");
        }

        product.setName(dto.name().trim());
        product.setSku(normalizedSku);
        product.setDescription(dto.description() != null ? dto.description().trim() : null);
        product.setPrice(dto.price());
        product.setStock(dto.stock());
        product.setMinimumStock(dto.minimumStock());
        product.setIsActive(dto.isActive());
        product.setCategoryId(dto.categoryId());

        var updated = productRepository.save(product);
        var loaded = productRepository.findWithCategoryById(updated.getId()).orElse(updated);
        return ServiceResult.success(DtoMapper.toDto(loaded));
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
}
