using Minimarket.Api.DTOs;
using Minimarket.Api.Mapping;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

public class CompanyService(ICompanyRepository companyRepository) : ICompanyService
{
    public async Task<CompanyDto?> GetAsync()
    {
        var company = await companyRepository.GetAsync();
        return company?.ToDto();
    }

    public async Task<(bool Success, string? Error, CompanyDto? Company)> UpdateAsync(SaveCompanyDto dto)
    {
        if (string.IsNullOrWhiteSpace(dto.BusinessName))
            return (false, "El nombre comercial es obligatorio.", null);

        if (string.IsNullOrWhiteSpace(dto.TaxId))
            return (false, "El RUC es obligatorio.", null);

        var company = await companyRepository.GetAsync();
        if (company is null)
            return (false, "La configuracion de empresa no existe.", null);

        company.BusinessName = dto.BusinessName.Trim();
        company.LegalName = dto.LegalName.Trim();
        company.TaxId = dto.TaxId.Trim();
        company.AddressLine = dto.AddressLine.Trim();
        company.Phone = dto.Phone.Trim();
        company.Tagline = dto.Tagline.Trim();
        company.DocumentTitle = dto.DocumentTitle.Trim();
        company.CustomerLabel = dto.CustomerLabel.Trim();
        company.FooterLine1 = dto.FooterLine1.Trim();
        company.FooterLine2 = dto.FooterLine2.Trim();
        company.ShowTicketPreview = dto.ShowTicketPreview;

        companyRepository.Update(company);
        await companyRepository.SaveChangesAsync();

        return (true, null, company.ToDto());
    }
}
