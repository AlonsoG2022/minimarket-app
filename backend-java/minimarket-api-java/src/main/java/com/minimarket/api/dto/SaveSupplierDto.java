package com.minimarket.api.dto;

public record SaveSupplierDto(
    String name,
    String documentNumber,
    String contactName,
    String phone,
    String email,
    String address,
    String notes,
    Boolean isActive
) {
}
