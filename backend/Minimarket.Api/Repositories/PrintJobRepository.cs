using Microsoft.EntityFrameworkCore;
using Minimarket.Api.Data;
using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public class PrintJobRepository(MinimarketDbContext context) : IPrintJobRepository
{
    public Task AddAsync(PrintJob job) => context.PrintJobs.AddAsync(job).AsTask();

    public Task<PrintJob?> GetByIdAsync(int id) =>
        context.PrintJobs
            .Include(job => job.Sale)
                .ThenInclude(sale => sale!.User)
            .Include(job => job.Sale)
                .ThenInclude(sale => sale!.Details)
                    .ThenInclude(detail => detail.Product)
            .FirstOrDefaultAsync(job => job.Id == id);

    public Task<List<PrintJob>> GetRecentAsync(int take = 20) =>
        context.PrintJobs
            .OrderByDescending(job => job.RequestedAt)
            .Take(take)
            .ToListAsync();

    public Task<List<PrintJob>> GetPendingAsync(int take = 20) =>
        context.PrintJobs
            .Where(job => job.Status == "pendiente")
            .OrderBy(job => job.RequestedAt)
            .Take(take)
            .ToListAsync();

    public Task<int> SaveChangesAsync() => context.SaveChangesAsync();
}
