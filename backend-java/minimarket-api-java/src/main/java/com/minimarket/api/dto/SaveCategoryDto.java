package com.minimarket.api.dto;

public record SaveCategoryDto(
    String name,
    String description,
    Boolean isActive
) {
}
