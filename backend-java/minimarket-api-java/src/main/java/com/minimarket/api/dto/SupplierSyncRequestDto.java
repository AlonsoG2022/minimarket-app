package com.minimarket.api.dto;

// Peticion para sincronizar el catalogo de un proveedor (token pegado + proveedor elegido).
// previewOnly = true solo calcula cuantos productos se crearian/actualizarian, sin escribir en la BD.
public record SupplierSyncRequestDto(
    String token,
    String supplierDocumentNumber,
    Boolean previewOnly) {
}
