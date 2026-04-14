using Microsoft.EntityFrameworkCore;
using Minimarket.Api.Data;
using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public class UserRepository(MinimarketDbContext context) : IUserRepository
{
    public Task<List<User>> GetAllAsync() =>
        context.Users
            .AsNoTracking()
            .OrderBy(x => x.FullName)
            .ToListAsync();

    public Task<User?> GetByIdAsync(int id) =>
        context.Users
            .AsNoTracking()
            .FirstOrDefaultAsync(x => x.Id == id);

    public Task<User?> GetByUsernameAsync(string username) =>
        context.Users
            .AsNoTracking()
            .FirstOrDefaultAsync(x => x.Username == username);

    public Task AddAsync(User user) => context.Users.AddAsync(user).AsTask();

    public Task<int> SaveChangesAsync() => context.SaveChangesAsync();
}
