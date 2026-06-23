package com.minimarket.api.dto;

import java.util.List;

// Resultado/resumen de la sincronizacion.
public record SupplierSyncResultDto(
    boolean previewOnly,
    String supplierName,
    int categoriesProcessed,
    int categoriesCreated,
    int productsProcessed,
    int productsCreated,
    int productsUpdated,
    List<String> warnings) {
}
