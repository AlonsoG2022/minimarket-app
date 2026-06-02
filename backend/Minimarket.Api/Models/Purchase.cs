namespace Minimarket.Api.Models;

public class Purchase
{
    public int Id { get; set; }
    public DateTime PurchaseDate { get; set; }
    public int SupplierId { get; set; }
    public Supplier? Supplier { get; set; }
    public int UserId { get; set; }
    public User? User { get; set; }
    public string? InvoiceNumber { get; set; }
    public string? Notes { get; set; }
    public decimal Total { get; set; }
    public ICollection<PurchaseDetail> Details { get; set; } = new List<PurchaseDetail>();
}
