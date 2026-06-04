package com.minimarket.api.dto;

import java.math.BigDecimal;

public record CloseCashSessionDto(Integer userId, BigDecimal countedAmount, String notes) {
}
