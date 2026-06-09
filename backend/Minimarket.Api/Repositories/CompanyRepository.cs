using Microsoft.EntityFrameworkCore;
using Minimarket.Api.Data;
using Minimarket.Api.Models;

namespace Minimarket.Api.Repositories;

public class CompanyRepository(MinimarketDbContext context) : ICompanyRepository
{
    public Task<Company?> GetAsync() =>
        context.Companies.FirstOrDefaultAsync();

    public void Update(Company company) =>
        context.Companies.Update(company);

    public Task<int> SaveChangesAsync() =>
        context.SaveChangesAsync();
}
