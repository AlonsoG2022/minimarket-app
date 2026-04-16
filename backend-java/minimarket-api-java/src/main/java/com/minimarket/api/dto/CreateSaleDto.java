package com.minimarket.api.dto;

import java.util.List;

public record CreateSaleDto(Integer userId, String paymentMethod, String notes, List<CreateSaleDetailDto> details) {
}
