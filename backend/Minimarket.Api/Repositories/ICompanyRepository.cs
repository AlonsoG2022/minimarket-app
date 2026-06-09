using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public interface ICompanyRepository
{
    Task<Company?> GetAsync();
    void Update(Company company);
    Task<int> SaveChangesAsync();
}
