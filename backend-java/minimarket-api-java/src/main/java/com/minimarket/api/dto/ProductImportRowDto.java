package com.minimarket.api.dto;

import java.math.BigDecimal;

public record ProductImportRowDto(
    Integer rowNumber,
    String name,
    BigDecimal price,
    String categoryName,
    Integer stock
) {
}
