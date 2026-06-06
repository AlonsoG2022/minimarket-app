package com.minimarket.api.dto;

import java.time.LocalDateTime;

public record PrintJobDto(
    Integer id,
    Integer saleId,
    String sourceType,
    String documentType,
    String status,
    Integer attempts,
    String printerName,
    LocalDateTime requestedAt,
    LocalDateTime startedAt,
    LocalDateTime processedAt,
    String lastError
) {
}
