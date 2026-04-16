package com.minimarket.api.dto;

import java.math.BigDecimal;

public record TopSellingProductDto(
    Integer productId,
    String productName,
    String sku,
    Integer totalQuantity,
    BigDecimal totalAmount
) {
}
