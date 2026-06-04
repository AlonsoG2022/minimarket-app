package com.minimarket.api.dto;

import java.math.BigDecimal;

public record CreateCashMovementDto(Integer userId, String type, BigDecimal amount, String description) {
}
