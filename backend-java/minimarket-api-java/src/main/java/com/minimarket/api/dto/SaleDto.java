package com.minimarket.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleDto(
    Integer id,
    LocalDateTime saleDate,
    Integer userId,
    String userName,
    String paymentMethod,
    BigDecimal total,
    String notes,
    List<SaleDetailDto> details
) {
}
