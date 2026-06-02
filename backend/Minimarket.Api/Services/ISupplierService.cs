using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface ISupplierService
{
    Task<IReadOnlyCollection<SupplierDto>> GetAllAsync();
    Task<SupplierDto?> GetByIdAsync(int id);
    Task<(bool Success, string? Error, SupplierDto? Supplier)> CreateAsync(SaveSupplierDto dto);
    Task<(bool Success, string? Error, SupplierDto? Supplier)> UpdateAsync(int id, SaveSupplierDto dto);
    Task<(bool Success, string? Error)> DeleteAsync(int id);
}
