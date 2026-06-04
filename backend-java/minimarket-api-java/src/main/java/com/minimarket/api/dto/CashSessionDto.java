package com.minimarket.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CashSessionDto(
    Integer id,
    Integer userId,
    String userName,
    LocalDateTime openedAt,
    LocalDateTime closedAt,
    BigDecimal openingAmount,
    BigDecimal closingExpectedAmount,
    BigDecimal closingCountedAmount,
    BigDecimal difference,
    String status,
    String notes,
    BigDecimal currentAmount,
    List<CashMovementDto> movements
) {
}
