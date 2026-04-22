using Minimarket.Api.DTOs;
using Minimarket.Api.Mapping;
using Minimarket.Api.Models;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

public class CategoryService(ICategoryRepository categoryRepository) : ICategoryService
{
    public async Task<IReadOnlyCollection<CategoryDto>> GetAllAsync() =>
        (await categoryRepository.GetAllAsync()).Select(x => x.ToDto()).ToList();

    public async Task<(bool Success, string? Error, CategoryDto? Category)> CreateAsync(SaveCategoryDto dto)
    {
        var normalizedName = dto.Name.Trim();
        if (string.IsNullOrWhiteSpace(normalizedName))
        {
            return (false, "El nombre de la categoria es obligatorio.", null);
        }

        if (await categoryRepository.GetByNameAsync(normalizedName) is not null)
        {
            return (false, "Ya existe una categoria con el mismo nombre.", null);
        }

        var category = new Category
        {
            Name = normalizedName,
            Description = dto.Description?.Trim(),
            IsActive = dto.IsActive
        };

        await categoryRepository.AddAsync(category);
        await categoryRepository.SaveChangesAsync();

        return (true, null, category.ToDto());
    }
}

