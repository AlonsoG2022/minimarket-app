package com.minimarket.api.tenant;

public record TenantRecord(
    Integer id,
    String code,
    String name,
    String slug,
    String databaseName,
    boolean active
) {
}
