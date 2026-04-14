using Microsoft.EntityFrameworkCore;
using Minimarket.Api.Data;
using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public class CategoryRepository(MinimarketDbContext context) : ICategoryRepository
{
    public Task<List<Category>> GetAllAsync() =>
        context.Categories
            .AsNoTracking()
            .Where(x => x.IsActive)
            .OrderBy(x => x.Name)
            .ToListAsync();

    public Task<Category?> GetByIdAsync(int id) =>
        context.Categories
            .AsNoTracking()
            .FirstOrDefaultAsync(x => x.Id == id);
}
