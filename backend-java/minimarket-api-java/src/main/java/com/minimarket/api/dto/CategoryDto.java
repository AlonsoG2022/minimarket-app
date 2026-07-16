package com.minimarket.api.dto;

import java.math.BigDecimal;

public record CategoryDto(Integer id, String name, String description, Boolean isActive, BigDecimal priceAdjustmentPercentage) {
}
