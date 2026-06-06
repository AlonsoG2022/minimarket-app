package com.minimarket.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TicketPrintPayloadDto(
    Integer saleId,
    LocalDateTime saleDate,
    String cashierName,
    String paymentMethod,
    BigDecimal total,
    String notes,
    List<TicketPrintItemDto> items
) {
}
