using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public interface IPurchaseRepository
{
    Task<List<Purchase>> GetAllAsync();
    Task<Purchase?> GetByIdAsync(int id);
    Task<bool> ExistsByInvoiceNumberAsync(string invoiceNumber);
    Task AddAsync(Purchase purchase);
    Task<int> SaveChangesAsync();
}
