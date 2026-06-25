using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public interface ICategoryRepository
{
    Task<List<Category>> GetAllAsync(bool includeInactive);
    Task<Category?> GetByIdAsync(int id);
    Task<Category?> GetByNameAsync(string name);
    Task AddAsync(Category category);
    void Update(Category category);
    Task<int> SaveChangesAsync();
}
