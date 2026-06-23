namespace Minimarket.Api.Models;

// Historico del costo de compra de un producto por proveedor.
// Se alimenta desde la sincronizacion del catalogo del proveedor (ej. Coca-Cola / AIC Digital).
public class SupplierProduct
{
    public int Id { get; set; }
    public int SupplierId { get; set; }
    public int ProductId { get; set; }
    public decimal LastCost { get; set; }
    public DateTime Date { get; set; }
    public Supplier? Supplier { get; set; }
    public Product? Product { get; set; }
}
