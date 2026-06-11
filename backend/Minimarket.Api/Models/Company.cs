namespace Minimarket.Api.Models;

public class Company
{
    public int Id { get; set; }
    public string BusinessName { get; set; } = string.Empty;
    public string LegalName { get; set; } = string.Empty;
    public string TaxId { get; set; } = string.Empty;
    public string AddressLine { get; set; } = string.Empty;
    public string Phone { get; set; } = string.Empty;
    public string Tagline { get; set; } = string.Empty;
    public string DocumentTitle { get; set; } = string.Empty;
    public string CustomerLabel { get; set; } = string.Empty;
    public string FooterLine1 { get; set; } = string.Empty;
    public string FooterLine2 { get; set; } = string.Empty;
    public bool ShowTicketPreview { get; set; } = true;
    public int MinimumStock { get; set; } = 5;
    public string Theme { get; set; } = "orange";
}
