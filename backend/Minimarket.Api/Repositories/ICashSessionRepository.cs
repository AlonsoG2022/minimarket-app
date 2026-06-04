using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public interface ICashSessionRepository
{
    Task<CashSession?> GetCurrentOpenAsync(int userId);
    Task<List<CashSession>> GetRecentByUserAsync(int userId, int take = 10);
    Task<CashSession?> GetByIdAsync(int id);
    Task AddAsync(CashSession session);
    Task<int> SaveChangesAsync();
}
