package com.minimarket.api.dto;

import java.math.BigDecimal;

// Item confirmado por el usuario en la pantalla de revision.
public record ReceiptConfirmItemDto(
    String action,        // "match" | "create" | "skip"
    Integer productId,
    String name,
    String shortName,
    String categoryName,
    Integer quantity,
    Integer packUnits,
    BigDecimal price,
    BigDecimal cost) {
}
