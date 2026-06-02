package com.minimarket.api.dto;

import java.math.BigDecimal;

public record ProductDto(
    Integer id,
    String name,
    String sku,
    String barcode,
    String purchaseBarcode,
    String description,
    BigDecimal price,
    BigDecimal cost,
    Integer stock,
    Integer minimumStock,
    String salesUnitName,
    String purchaseUnitName,
    Integer unitsPerPurchaseUnit,
    Boolean isActive,
    Integer categoryId,
    String categoryName
) {
}
