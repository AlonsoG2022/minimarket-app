package com.minimarket.api.service;

import com.minimarket.api.dto.CategoryDto;
import com.minimarket.api.repository.CategoryRepository;
import com.minimarket.api.util.DtoMapper;
import org.springframework.stereotype.Service;

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
}
