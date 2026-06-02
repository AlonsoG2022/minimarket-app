package com.minimarket.api.dto;

public record SupplierDto(
    Integer id,
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
