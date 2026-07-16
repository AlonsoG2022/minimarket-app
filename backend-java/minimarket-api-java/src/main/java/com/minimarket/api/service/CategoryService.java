package com.minimarket.api.service;

import com.minimarket.api.dto.CategoryDto;
import com.minimarket.api.dto.SaveCategoryDto;
import com.minimarket.api.entity.Category;
import com.minimarket.api.repository.CategoryRepository;
import com.minimarket.api.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryDto> getAll(boolean includeInactive) {
        var categories = includeInactive
            ? categoryRepository.findAllByOrderByNameAsc()
            : categoryRepository.findByIsActiveTrueOrderByNameAsc();
        return categories.stream().map(DtoMapper::toDto).toList();
    }

    private BigDecimal normalizePriceAdjustment(BigDecimal value) {
        var normalized = value != null ? value : BigDecimal.ZERO;
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El ajuste de precio no puede ser negativo.");
        }

        return normalized.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public ServiceResult<CategoryDto> create(SaveCategoryDto dto) {
        final BigDecimal normalizedAdjustment;
        try {
            normalizedAdjustment = normalizePriceAdjustment(dto.priceAdjustmentPercentage());
        } catch (IllegalArgumentException ex) {
            return ServiceResult.failure(ex.getMessage());
        }

        var normalizedName = dto.name() != null ? dto.name().trim() : "";
        if (normalizedName.isBlank()) {
            return ServiceResult.failure("El nombre de la categoria es obligatorio.");
        }

        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            return ServiceResult.failure("Ya existe una categoria con el mismo nombre.");
        }

        var category = new Category();
        category.setName(normalizedName);
        category.setDescription(dto.description() != null ? dto.description().trim() : null);
        category.setIsActive(Boolean.TRUE.equals(dto.isActive()));
        category.setPriceAdjustmentPercentage(normalizedAdjustment);

        var saved = categoryRepository.save(category);
        return ServiceResult.success(DtoMapper.toDto(saved));
    }

    @Transactional
    public ServiceResult<CategoryDto> update(Integer id, SaveCategoryDto dto) {
        final BigDecimal normalizedAdjustment;
        try {
            normalizedAdjustment = normalizePriceAdjustment(dto.priceAdjustmentPercentage());
        } catch (IllegalArgumentException ex) {
            return ServiceResult.failure(ex.getMessage());
        }

        var category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            return ServiceResult.failure("Categoria no encontrada.");
        }

        var normalizedName = dto.name() != null ? dto.name().trim() : "";
        if (normalizedName.isBlank()) {
            return ServiceResult.failure("El nombre de la categoria es obligatorio.");
        }

        var byName = categoryRepository.findByNameIgnoreCase(normalizedName).orElse(null);
        if (byName != null && !byName.getId().equals(id)) {
            return ServiceResult.failure("Ya existe una categoria con el mismo nombre.");
        }

        category.setName(normalizedName);
        category.setDescription(dto.description() != null ? dto.description().trim() : null);
        category.setIsActive(Boolean.TRUE.equals(dto.isActive()));
        category.setPriceAdjustmentPercentage(normalizedAdjustment);

        var saved = categoryRepository.save(category);
        return ServiceResult.success(DtoMapper.toDto(saved));
    }
}
