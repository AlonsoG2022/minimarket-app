package com.minimarket.api.dto;

import java.math.BigDecimal;

public record SaveProductDto(
    String name,
    String shortName,
    String barcode,
    String purchaseBarcode,
    String description,
    BigDecimal price,
    String expirationDate,
    String salesUnitName,
    String purchaseUnitName,
    Integer unitsPerPurchaseUnit,
    Integer stock,
    Integer minimumStock,
    Boolean isActive,
    Integer categoryId
) {
}
