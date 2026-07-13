package com.minimarket.api.dto;

import java.util.List;

public record ReceiptConfirmRequestDto(
    Integer supplierId,
    String newSupplierName,
    String newSupplierRuc,
    Integer userId,
    String invoiceNumber,
    List<ReceiptConfirmItemDto> items) {
}
