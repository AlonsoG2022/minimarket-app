package com.minimarket.api.dto;

import java.math.BigDecimal;

public record TicketPrintItemDto(
    String productName,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal
) {
}
