namespace Minimarket.PrintWorker.Models;

public class PrintJob
{
    public int Id { get; set; }
    public int? SaleId { get; set; }
    public string SourceType { get; set; } = "sale";
    public string DocumentType { get; set; } = "ticket";
    public string Status { get; set; } = "pendiente";
    public int Attempts { get; set; }
    public string? PrinterName { get; set; }
    public DateTime RequestedAt { get; set; }
    public DateTime? StartedAt { get; set; }
    public DateTime? ProcessedAt { get; set; }
    public string? LastError { get; set; }
    public string PayloadJson { get; set; } = string.Empty;
}
