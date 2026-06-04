using Microsoft.EntityFrameworkCore;
using Minimarket.Api.Data;
using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public class CashSessionRepository(MinimarketDbContext context) : ICashSessionRepository
{
    public Task<CashSession?> GetCurrentOpenAsync(int userId) =>
        context.CashSessions
            .Include(session => session.User)
            .Include(session => session.Movements)
            .FirstOrDefaultAsync(session => session.UserId == userId && session.Status == "abierta");

    public Task<List<CashSession>> GetRecentByUserAsync(int userId, int take = 10) =>
        context.CashSessions
            .Include(session => session.User)
            .Include(session => session.Movements)
            .Where(session => session.UserId == userId)
            .OrderByDescending(session => session.OpenedAt)
            .Take(take)
            .ToListAsync();

    public Task<CashSession?> GetByIdAsync(int id) =>
        context.CashSessions
            .Include(session => session.User)
            .Include(session => session.Movements)
            .FirstOrDefaultAsync(session => session.Id == id);

    public Task AddAsync(CashSession session) => context.CashSessions.AddAsync(session).AsTask();

    public Task<int> SaveChangesAsync() => context.SaveChangesAsync();
}
