namespace Minimarket.Api.DTOs;

// Peticion para sincronizar el catalogo de un proveedor (token pegado + proveedor elegido).
// PreviewOnly = true solo calcula cuantos productos se crearian/actualizarian, sin escribir en la BD.
public record SupplierSyncRequestDto(
    string Token,
    string SupplierDocumentNumber,
    bool PreviewOnly);

// Resultado/resumen de la sincronizacion.
public record SupplierSyncResultDto(
    bool PreviewOnly,
    string SupplierName,
    int CategoriesProcessed,
    int CategoriesCreated,
    int ProductsProcessed,
    int ProductsCreated,
    int ProductsUpdated,
    IReadOnlyCollection<string> Warnings);
