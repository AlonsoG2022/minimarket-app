namespace Minimarket.Api.DTOs;

public record SupplierDto(
    int Id,
    string Name,
    string? DocumentNumber,
    string? ContactName,
    string? Phone,
    string? Email,
    string? Address,
    string? Notes,
    bool IsActive);

public record SaveSupplierDto(
    string Name,
    string? DocumentNumber,
    string? ContactName,
    string? Phone,
    string? Email,
    string? Address,
    string? Notes,
    bool IsActive);
