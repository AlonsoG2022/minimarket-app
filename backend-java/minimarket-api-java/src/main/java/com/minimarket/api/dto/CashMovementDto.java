package com.minimarket.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CashMovementDto(
    Integer id,
    LocalDateTime movementDate,
    String type,
    BigDecimal amount,
    String description,
    String referenceType,
    Integer referenceId
) {
}
