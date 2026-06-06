using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public interface IPrintJobRepository
{
    Task AddAsync(PrintJob job);
    Task<PrintJob?> GetByIdAsync(int id);
    Task<List<PrintJob>> GetRecentAsync(int take = 20);
    Task<List<PrintJob>> GetPendingAsync(int take = 20);
    Task<int> SaveChangesAsync();
}
