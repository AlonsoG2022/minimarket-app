using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public interface ISupplierRepository
{
    Task<List<Supplier>> GetAllAsync();
    Task<Supplier?> GetByIdAsync(int id);
    Task AddAsync(Supplier supplier);
    void Update(Supplier supplier);
    void Remove(Supplier supplier);
    Task<int> SaveChangesAsync();
}
