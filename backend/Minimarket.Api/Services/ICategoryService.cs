using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface ICategoryService
{
    Task<IReadOnlyCollection<CategoryDto>> GetAllAsync(bool includeInactive);
    Task<(bool Success, string? Error, CategoryDto? Category)> CreateAsync(SaveCategoryDto dto);
    Task<(bool Success, string? Error, CategoryDto? Category)> UpdateAsync(int id, SaveCategoryDto dto);
}
