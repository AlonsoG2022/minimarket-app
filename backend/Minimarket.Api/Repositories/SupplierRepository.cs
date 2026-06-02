using Microsoft.EntityFrameworkCore;
using Minimarket.Api.Data;
using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public class SupplierRepository(MinimarketDbContext context) : ISupplierRepository
{
    public Task<List<Supplier>> GetAllAsync() =>
        context.Suppliers
            .AsNoTracking()
            .OrderBy(x => x.Name)
            .ToListAsync();

    public Task<Supplier?> GetByIdAsync(int id) =>
        context.Suppliers.FirstOrDefaultAsync(x => x.Id == id);

    public Task AddAsync(Supplier supplier) => context.Suppliers.AddAsync(supplier).AsTask();

    public void Update(Supplier supplier) => context.Suppliers.Update(supplier);

    public void Remove(Supplier supplier) => context.Suppliers.Remove(supplier);

    public Task<int> SaveChangesAsync() => context.SaveChangesAsync();
}
