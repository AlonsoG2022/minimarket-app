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
    int MinimumStock,
    string Theme,
    bool HasAiReceiptKey);

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
    int MinimumStock,
    string Theme,
    // null o vacio = no cambiar la clave; con valor = guardar/reemplazar la clave de la IA.
    string? AiReceiptKey);
