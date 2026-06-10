namespace Minimarket.Api.DTOs;

public record CompanyDto(
    int Id,
    string BusinessName,
    string LegalName,
    string TaxId,
    string AddressLine,
    string Phone,
    string Tagline,
    string DocumentTitle,
    string CustomerLabel,
    string FooterLine1,
    string FooterLine2,
    bool ShowTicketPreview,
    int MinimumStock);

public record SaveCompanyDto(
    string BusinessName,
    string LegalName,
    string TaxId,
    string AddressLine,
    string Phone,
    string Tagline,
    string DocumentTitle,
    string CustomerLabel,
    string FooterLine1,
    string FooterLine2,
    bool ShowTicketPreview,
    int MinimumStock);
