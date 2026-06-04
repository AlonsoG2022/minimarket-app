namespace Minimarket.Api.Models;

public class CashMovement
{
    public int Id { get; set; }
    public int CashSessionId { get; set; }
    public CashSession? CashSession { get; set; }
    public DateTime MovementDate { get; set; }
    public string Type { get; set; } = string.Empty;
    public decimal Amount { get; set; }
    public string? Description { get; set; }
    public string? ReferenceType { get; set; }
    public int? ReferenceId { get; set; }
}
