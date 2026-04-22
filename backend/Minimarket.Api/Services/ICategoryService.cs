using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface ICategoryService
{
    Task<IReadOnlyCollection<CategoryDto>> GetAllAsync();
    Task<(bool Success, string? Error, CategoryDto? Category)> CreateAsync(SaveCategoryDto dto);
}
