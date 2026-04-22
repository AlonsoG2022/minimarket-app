package com.minimarket.api.service;

import com.minimarket.api.dto.CategoryDto;
import com.minimarket.api.dto.SaveCategoryDto;
import com.minimarket.api.entity.Category;
import com.minimarket.api.repository.CategoryRepository;
import com.minimarket.api.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryDto> getAll() {
        return categoryRepository.findByIsActiveTrueOrderByNameAsc()
            .stream()
            .map(DtoMapper::toDto)
            .toList();
    }

    @Transactional
    public ServiceResult<CategoryDto> create(SaveCategoryDto dto) {
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

        var saved = categoryRepository.save(category);
        return ServiceResult.success(DtoMapper.toDto(saved));
    }
}
