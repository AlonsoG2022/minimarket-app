package com.minimarket.api.dto;

import java.math.BigDecimal;

public record SaleDetailDto(
    Integer id,
    Integer productId,
    String productName,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal
) {
}
