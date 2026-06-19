package com.minimarket.api.dto;

import java.math.BigDecimal;

public record ProductImportRowDto(
    Integer rowNumber,
    String name,
    String shortName,
    BigDecimal price,
    String categoryName,
    String barcode,
    String description,
    String salesUnitName,
    String purchaseUnitName,
    Integer unitsPerPurchaseUnit,
    Integer stock,
    String expirationDate,
    Boolean isActive
) {
}
