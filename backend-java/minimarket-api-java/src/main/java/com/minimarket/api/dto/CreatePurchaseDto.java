package com.minimarket.api.dto;

import java.util.List;

public record CreatePurchaseDto(
    Integer supplierId,
    Integer userId,
    String invoiceNumber,
    String notes,
    List<CreatePurchaseDetailDto> details
) {
}
