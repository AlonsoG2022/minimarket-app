namespace Minimarket.Api.DTOs;

public record CashMovementDto(
    int Id,
    DateTime MovementDate,
    string Type,
    decimal Amount,
    string? Description,
    string? ReferenceType,
    int? ReferenceId);

public record CashSessionDto(
    int Id,
    int UserId,
    string UserName,
    DateTime OpenedAt,
    DateTime? ClosedAt,
    decimal OpeningAmount,
    decimal? ClosingExpectedAmount,
    decimal? ClosingCountedAmount,
    decimal? Difference,
    string Status,
    string? Notes,
    decimal CurrentAmount,
    IReadOnlyCollection<CashMovementDto> Movements);

public record OpenCashSessionDto(int UserId, decimal OpeningAmount, string? Notes);

public record CreateCashMovementDto(int UserId, string Type, decimal Amount, string? Description);

public record CloseCashSessionDto(int UserId, decimal CountedAmount, string? Notes);
