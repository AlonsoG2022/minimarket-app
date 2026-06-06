package com.minimarket.printworker.dto;

import java.math.BigDecimal;

public record TicketPrintItemDto(
    String productName,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal
) {
}
