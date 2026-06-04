namespace Minimarket.Api.Models;

public class CashSession
{
    public int Id { get; set; }
    public int UserId { get; set; }
    public User? User { get; set; }
    public DateTime OpenedAt { get; set; }
    public DateTime? ClosedAt { get; set; }
    public decimal OpeningAmount { get; set; }
    public decimal? ClosingExpectedAmount { get; set; }
    public decimal? ClosingCountedAmount { get; set; }
    public decimal? Difference { get; set; }
    public string Status { get; set; } = "abierta";
    public string? Notes { get; set; }
    public ICollection<CashMovement> Movements { get; set; } = new List<CashMovement>();
    public ICollection<Sale> Sales { get; set; } = new List<Sale>();
}
