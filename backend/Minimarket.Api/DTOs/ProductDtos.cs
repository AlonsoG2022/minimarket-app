namespace Minimarket.Api.DTOs;

public record ProductDto(
    int Id,
    string Name,
    string Sku,
    string? Description,
    decimal Price,
    int Stock,
    int MinimumStock,
    bool IsActive,
    int CategoryId,
    string CategoryName);

public record SaveProductDto(
    string Name,
    string? Description,
    decimal Price,
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
