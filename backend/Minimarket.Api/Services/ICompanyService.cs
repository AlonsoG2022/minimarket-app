using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface ICompanyService
{
    Task<CompanyDto?> GetAsync();
    Task<(bool Success, string? Error, CompanyDto? Company)> UpdateAsync(SaveCompanyDto dto);
}
