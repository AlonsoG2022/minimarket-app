namespace Minimarket.Api.Models;

public class Sale
{
    public int Id { get; set; }
    public DateTime SaleDate { get; set; }
    public int UserId { get; set; }
    public User? User { get; set; }
    public decimal Total { get; set; }
    public string PaymentMethod { get; set; } = string.Empty;
    public string? Notes { get; set; }
    public ICollection<SaleDetail> Details { get; set; } = new List<SaleDetail>();
}
