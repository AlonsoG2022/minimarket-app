package com.minimarket.api.dto;

public record ProductImportErrorDto(
    Integer rowNumber,
    String message
) {
}
