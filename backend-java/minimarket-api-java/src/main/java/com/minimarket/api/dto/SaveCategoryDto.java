package com.minimarket.api.dto;

import java.math.BigDecimal;

public record SaveCategoryDto(
    String name,
    String description,
    Boolean isActive,
    BigDecimal priceAdjustmentPercentage
) {
}
