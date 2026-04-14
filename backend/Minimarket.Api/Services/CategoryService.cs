using Minimarket.Api.DTOs;
using Minimarket.Api.Mapping;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

public class CategoryService(ICategoryRepository categoryRepository) : ICategoryService
{
    public async Task<IReadOnlyCollection<CategoryDto>> GetAllAsync() =>
        (await categoryRepository.GetAllAsync()).Select(x => x.ToDto()).ToList();
}


