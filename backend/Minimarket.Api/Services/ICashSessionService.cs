using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface ICashSessionService
{
    Task<CashSessionDto?> GetCurrentAsync(int userId);
    Task<IReadOnlyCollection<CashSessionDto>> GetRecentByUserAsync(int userId);
    Task<(bool Success, string? Error, CashSessionDto? Session)> OpenAsync(OpenCashSessionDto dto);
    Task<(bool Success, string? Error, CashSessionDto? Session)> AddMovementAsync(int sessionId, CreateCashMovementDto dto);
    Task<(bool Success, string? Error, CashSessionDto? Session)> CloseAsync(int sessionId, CloseCashSessionDto dto);
}
