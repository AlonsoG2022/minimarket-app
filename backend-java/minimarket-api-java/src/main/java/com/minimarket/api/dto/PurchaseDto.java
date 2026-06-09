package com.minimarket.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseDto(
    Integer id,
    LocalDateTime purchaseDate,
    Integer supplierId,
    String supplierName,
    Integer userId,
    String userName,
    String invoiceNumber,
    String notes,
    BigDecimal subTotal,
    BigDecimal igv,
    BigDecimal total,
    List<PurchaseDetailDto> details
) {
}
