package com.minimarket.printworker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TicketPrintPayloadDto(
    Integer saleId,
    LocalDateTime saleDate,
    String businessName,
    String legalName,
    String taxId,
    String addressLine,
    String phone,
    String tagline,
    String documentTitle,
    String customerLabel,
    String cashierName,
    String paymentMethod,
    BigDecimal subTotal,
    BigDecimal igv,
    BigDecimal total,
    String footerLine1,
    String footerLine2,
    String notes,
    List<TicketPrintItemDto> items
) {
}
