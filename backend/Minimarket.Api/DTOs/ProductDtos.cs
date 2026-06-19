namespace Minimarket.Api.DTOs;

public record ProductDto(
    int Id,
    string Name,
    string ShortName,
    string Sku,
    string? Barcode,
    string? PurchaseBarcode,
    string? Description,
    decimal Price,
    decimal Cost,
    int Stock,
    int MinimumStock,
    string? ExpirationDate,
    string SalesUnitName,
    string PurchaseUnitName,
    int UnitsPerPurchaseUnit,
    bool IsActive,
    int CategoryId,
    string CategoryName);

public record SaveProductDto(
    string Name,
    string? ShortName,
    string? Barcode,
    string? PurchaseBarcode,
    string? Description,
    decimal Price,
    string? ExpirationDate,
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
    string? ShortName,
    decimal Price,
    string CategoryName,
    string? Barcode,
    string? Description,
    string? SalesUnitName,
    string? PurchaseUnitName,
    int? UnitsPerPurchaseUnit,
    int Stock,
    string? ExpirationDate,
    bool? IsActive);

public record ProductImportErrorDto(
    int RowNumber,
    string Message);

public record ProductImportResultDto(
    int CreatedCount,
    IReadOnlyCollection<ProductImportErrorDto> Errors);

public record DeleteProductResultDto(
    string Message,
    bool Deactivated);
