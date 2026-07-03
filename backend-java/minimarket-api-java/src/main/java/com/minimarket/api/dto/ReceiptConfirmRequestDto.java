package com.minimarket.api.dto;

import java.util.List;

public record ReceiptConfirmRequestDto(
    Integer supplierId,
    List<ReceiptConfirmItemDto> items) {
}
