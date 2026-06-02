package com.minimarket.api.dto;

import java.math.BigDecimal;

public record CreatePurchaseDetailDto(
    Integer productId,
    Integer packageQuantity,
    Integer unitsPerPackage,
    BigDecimal packageCost,
    String purchaseUnitName,
    String barcodeSnapshot
) {
}
