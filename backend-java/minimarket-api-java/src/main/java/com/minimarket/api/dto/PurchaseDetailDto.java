package com.minimarket.api.dto;

import java.math.BigDecimal;

public record PurchaseDetailDto(
    Integer id,
    Integer productId,
    String productName,
    Integer packageQuantity,
    Integer unitsPerPackage,
    Integer totalUnits,
    BigDecimal packageCost,
    BigDecimal unitCost,
    BigDecimal subtotal,
    String purchaseUnitName,
    String barcodeSnapshot
) {
}
