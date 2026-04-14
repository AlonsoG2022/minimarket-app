using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface IProductService
{
    Task<IReadOnlyCollection<ProductDto>> GetAllAsync();
    Task<ProductDto?> GetByIdAsync(int id);
    Task<(bool Success, string? Error, ProductDto? Product)> CreateAsync(SaveProductDto dto);
    Task<(bool Success, string? Error, ProductDto? Product)> UpdateAsync(int id, SaveProductDto dto);
    Task<(bool Success, string? Error)> DeleteAsync(int id);
}
