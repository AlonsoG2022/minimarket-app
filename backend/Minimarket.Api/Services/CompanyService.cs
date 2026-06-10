using Minimarket.Api.DTOs;
using Minimarket.Api.Mapping;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

public class CompanyService(ICompanyRepository companyRepository, IProductRepository productRepository) : ICompanyService
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

        if (dto.MinimumStock < 0)
            return (false, "El stock minimo no puede ser negativo.", null);

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
        company.MinimumStock = dto.MinimumStock;

        companyRepository.Update(company);
        await companyRepository.SaveChangesAsync();

        // El stock minimo es global: sincroniza todos los productos con el nuevo valor.
        await productRepository.UpdateAllMinimumStockAsync(dto.MinimumStock);

        return (true, null, company.ToDto());
    }
}
