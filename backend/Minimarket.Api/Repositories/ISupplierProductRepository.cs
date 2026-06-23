using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public interface ISupplierProductRepository
{
    Task AddAsync(SupplierProduct entry);
    Task<int> SaveChangesAsync();
}
