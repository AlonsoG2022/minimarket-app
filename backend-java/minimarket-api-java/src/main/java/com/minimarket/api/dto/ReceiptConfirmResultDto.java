package com.minimarket.api.dto;

import java.util.List;

public record ReceiptConfirmResultDto(
    int created,
    int updated,
    int purchaseId,
    List<String> warnings) {
}
