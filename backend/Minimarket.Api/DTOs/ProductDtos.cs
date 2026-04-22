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
