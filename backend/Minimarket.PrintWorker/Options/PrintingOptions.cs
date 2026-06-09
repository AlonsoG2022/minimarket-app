namespace Minimarket.PrintWorker.Options;

public class PrintingOptions
{
    public int PollSeconds { get; set; } = 5;
    public string? PrinterName { get; set; }
    public string BusinessName { get; set; } = "Minimarket";
    public string LegalName { get; set; } = "Minimarket Casa";
    public string TaxId { get; set; } = "RUC por definir";
    public string AddressLine { get; set; } = "Direccion por definir";
    public string Phone { get; set; } = "Telefono por definir";
    public string CustomerLabel { get; set; } = "Consumidor final";
    public string FooterLine1 { get; set; } = "Gracias por su compra";
    public string FooterLine2 { get; set; } = "Vuelva pronto";
    public int LineWidth { get; set; } = 42;
}
