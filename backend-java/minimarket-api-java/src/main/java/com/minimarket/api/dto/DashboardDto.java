package com.minimarket.api.dto;

import java.math.BigDecimal;

public record DashboardDto(BigDecimal todaySales, Integer todayTransactions, Integer productCount, Integer lowStockProducts) {
}
