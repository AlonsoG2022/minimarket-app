namespace Minimarket.Api.Models;

public class PurchaseDetail
{
    public int Id { get; set; }
    public int PurchaseId { get; set; }
    public Purchase? Purchase { get; set; }
    public int ProductId { get; set; }
    public Product? Product { get; set; }
    public int PackageQuantity { get; set; }
    public int UnitsPerPackage { get; set; }
    public int TotalUnits { get; set; }
    public decimal PackageCost { get; set; }
    public decimal UnitCost { get; set; }
    public decimal Subtotal { get; set; }
    public string PurchaseUnitName { get; set; } = "unidad";
    public string? BarcodeSnapshot { get; set; }
}
