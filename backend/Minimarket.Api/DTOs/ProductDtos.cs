namespace Minimarket.Api.DTOs;

public record ProductDto(
    int Id,
    string Name,
    string Sku,
    string? Barcode,
    string? PurchaseBarcode,
    string? Description,
    decimal Price,
    decimal Cost,
    int Stock,
    int MinimumStock,
    string SalesUnitName,
    string PurchaseUnitName,
    int UnitsPerPurchaseUnit,
    bool IsActive,
    int CategoryId,
    string CategoryName);

public record SaveProductDto(
    string Name,
    string? Barcode,
    string? PurchaseBarcode,
    string? Description,
    decimal Price,
    string SalesUnitName,
    string PurchaseUnitName,
    int UnitsPerPurchaseUnit,
    int Stock,
    int MinimumStock,
    bool IsActive,
    int CategoryId);

public record ProductImportRowDto(
    int RowNumber,
    string Name,
    decimal Price,
    string CategoryName,
    int Stock);

public record ProductImportErrorDto(
    int RowNumber,
    string Message);

public record ProductImportResultDto(
    int CreatedCount,
    IReadOnlyCollection<ProductImportErrorDto> Errors);
