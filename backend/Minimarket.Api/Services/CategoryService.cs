using Minimarket.Api.DTOs;
using Minimarket.Api.Mapping;
using Minimarket.Api.Models;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

public class CategoryService(ICategoryRepository categoryRepository) : ICategoryService
{
    public async Task<IReadOnlyCollection<CategoryDto>> GetAllAsync(bool includeInactive) =>
        (await categoryRepository.GetAllAsync(includeInactive)).Select(x => x.ToDto()).ToList();

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

    public async Task<(bool Success, string? Error, CategoryDto? Category)> UpdateAsync(int id, SaveCategoryDto dto)
    {
        var category = await categoryRepository.GetByIdAsync(id);
        if (category is null)
        {
            return (false, "Categoria no encontrada.", null);
        }

        var normalizedName = dto.Name.Trim();
        if (string.IsNullOrWhiteSpace(normalizedName))
        {
            return (false, "El nombre de la categoria es obligatorio.", null);
        }

        var byName = await categoryRepository.GetByNameAsync(normalizedName);
        if (byName is not null && byName.Id != id)
        {
            return (false, "Ya existe una categoria con el mismo nombre.", null);
        }

        // Si el nombre no cambio, byName es la misma fila ya rastreada; la reutilizamos para evitar
        // conflictos de seguimiento. Si cambio a un nombre nuevo, usamos la instancia cargada por Id.
        var toUpdate = byName is not null && byName.Id == id ? byName : category;
        toUpdate.Name = normalizedName;
        toUpdate.Description = dto.Description?.Trim();
        toUpdate.IsActive = dto.IsActive;

        categoryRepository.Update(toUpdate);
        await categoryRepository.SaveChangesAsync();

        return (true, null, toUpdate.ToDto());
    }
}

