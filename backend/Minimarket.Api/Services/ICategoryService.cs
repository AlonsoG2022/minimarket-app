using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface ICategoryService
{
    Task<IReadOnlyCollection<CategoryDto>> GetAllAsync();
}
