using Minimarket.Api.DTOs;
using Minimarket.Api.Mapping;
using Minimarket.Api.Models;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

public class CashSessionService(
    ICashSessionRepository cashSessionRepository,
    IUserRepository userRepository) : ICashSessionService
{
    public async Task<CashSessionDto?> GetCurrentAsync(int userId) =>
        (await cashSessionRepository.GetCurrentOpenAsync(userId))?.ToDto();

    public async Task<IReadOnlyCollection<CashSessionDto>> GetRecentByUserAsync(int userId) =>
        (await cashSessionRepository.GetRecentByUserAsync(userId)).Select(session => session.ToDto()).ToList();

    public async Task<(bool Success, string? Error, CashSessionDto? Session)> OpenAsync(OpenCashSessionDto dto)
    {
        var user = await userRepository.GetByIdAsync(dto.UserId);
        if (user is null || !user.IsActive)
        {
            return (false, "El usuario no existe o esta inactivo.", null);
        }

        if (dto.OpeningAmount < 0)
        {
            return (false, "El monto inicial no puede ser negativo.", null);
        }

        var currentSession = await cashSessionRepository.GetCurrentOpenAsync(dto.UserId);
        if (currentSession is not null)
        {
            return (false, "Ya existe una caja abierta para este usuario.", null);
        }

        var session = new CashSession
        {
            UserId = dto.UserId,
            OpenedAt = DateTime.Now,
            OpeningAmount = dto.OpeningAmount,
            Notes = dto.Notes?.Trim(),
            Status = "abierta"
        };

        await cashSessionRepository.AddAsync(session);
        await cashSessionRepository.SaveChangesAsync();

        return (true, null, (await cashSessionRepository.GetByIdAsync(session.Id))?.ToDto());
    }

    public async Task<(bool Success, string? Error, CashSessionDto? Session)> AddMovementAsync(int sessionId, CreateCashMovementDto dto)
    {
        var session = await cashSessionRepository.GetByIdAsync(sessionId);
        if (session is null)
        {
            return (false, "La caja indicada no existe.", null);
        }

        if (session.UserId != dto.UserId)
        {
            return (false, "La caja no pertenece al usuario actual.", null);
        }

        if (!string.Equals(session.Status, "abierta", StringComparison.OrdinalIgnoreCase))
        {
            return (false, "Solo puedes registrar movimientos en una caja abierta.", null);
        }

        var type = dto.Type.Trim().ToLowerInvariant();
        if (type is not ("ingreso" or "retiro" or "gasto"))
        {
            return (false, "Tipo de movimiento no valido.", null);
        }

        if (dto.Amount <= 0)
        {
            return (false, "El monto debe ser mayor que cero.", null);
        }

        session.Movements.Add(new CashMovement
        {
            MovementDate = DateTime.Now,
            Type = type,
            Amount = dto.Amount,
            Description = dto.Description?.Trim()
        });

        await cashSessionRepository.SaveChangesAsync();
        return (true, null, (await cashSessionRepository.GetByIdAsync(sessionId))?.ToDto());
    }

    public async Task<(bool Success, string? Error, CashSessionDto? Session)> CloseAsync(int sessionId, CloseCashSessionDto dto)
    {
        var session = await cashSessionRepository.GetByIdAsync(sessionId);
        if (session is null)
        {
            return (false, "La caja indicada no existe.", null);
        }

        if (session.UserId != dto.UserId)
        {
            return (false, "La caja no pertenece al usuario actual.", null);
        }

        if (!string.Equals(session.Status, "abierta", StringComparison.OrdinalIgnoreCase))
        {
            return (false, "La caja ya fue cerrada.", null);
        }

        var expectedAmount = session.ToDto().CurrentAmount;
        session.ClosedAt = DateTime.Now;
        session.ClosingExpectedAmount = expectedAmount;
        session.ClosingCountedAmount = dto.CountedAmount;
        session.Difference = dto.CountedAmount - expectedAmount;
        session.Notes = string.IsNullOrWhiteSpace(dto.Notes) ? session.Notes : dto.Notes.Trim();
        session.Status = "cerrada";

        await cashSessionRepository.SaveChangesAsync();
        return (true, null, (await cashSessionRepository.GetByIdAsync(sessionId))?.ToDto());
    }
}
