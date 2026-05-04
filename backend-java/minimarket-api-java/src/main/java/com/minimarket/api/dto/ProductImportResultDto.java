package com.minimarket.api.dto;

import java.util.List;

public record ProductImportResultDto(
    Integer createdCount,
    List<ProductImportErrorDto> errors
) {
}
