package com.minimarket.api.dto;

import java.math.BigDecimal;

public record SaveProductDto(
    String name,
    String sku,
    String description,
    BigDecimal price,
    Integer stock,
    Integer minimumStock,
    Boolean isActive,
    Integer categoryId
) {
}
