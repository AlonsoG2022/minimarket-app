using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public interface IUserRepository
{
    Task<List<User>> GetAllAsync();
    Task<User?> GetByIdAsync(int id);
    Task<User?> GetByUsernameAsync(string username);
    Task AddAsync(User user);
    Task<int> SaveChangesAsync();
}
