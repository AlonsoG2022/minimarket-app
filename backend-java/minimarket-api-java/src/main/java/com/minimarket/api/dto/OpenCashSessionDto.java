package com.minimarket.api.dto;

import java.math.BigDecimal;

public record OpenCashSessionDto(Integer userId, BigDecimal openingAmount, String notes) {
}
