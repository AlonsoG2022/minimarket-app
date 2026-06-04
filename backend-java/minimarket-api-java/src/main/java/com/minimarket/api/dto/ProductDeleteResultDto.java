package com.minimarket.api.dto;

public record ProductDeleteResultDto(
    String message,
    boolean deactivated
) {
}
