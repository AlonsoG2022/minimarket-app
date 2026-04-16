package com.minimarket.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesSummaryDto(LocalDate date, BigDecimal totalAmount, Integer saleCount) {
}
