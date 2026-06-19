namespace Minimarket.Api.Models;

public class Product
{
    public int Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public string ShortName { get; set; } = string.Empty;
    public string Sku { get; set; } = string.Empty;
    public string? Barcode { get; set; }
    public string? PurchaseBarcode { get; set; }
    public string? Description { get; set; }
    public decimal Price { get; set; }
    public decimal Cost { get; set; }
    public int Stock { get; set; }
    public int MinimumStock { get; set; }
    public DateOnly? ExpirationDate { get; set; }
    public string SalesUnitName { get; set; } = "unidad";
    public string PurchaseUnitName { get; set; } = "unidad";
    public int UnitsPerPurchaseUnit { get; set; } = 1;
    public bool IsActive { get; set; } = true;
    public int CategoryId { get; set; }
    public Category? Category { get; set; }
    public ICollection<SaleDetail> SaleDetails { get; set; } = new List<SaleDetail>();
    public ICollection<PurchaseDetail> PurchaseDetails { get; set; } = new List<PurchaseDetail>();
}
