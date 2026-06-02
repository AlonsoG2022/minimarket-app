using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface IPurchaseService
{
    Task<IReadOnlyCollection<PurchaseDto>> GetAllAsync();
    Task<PurchaseDto?> GetByIdAsync(int id);
    Task<(bool Success, string? Error, PurchaseDto? Purchase)> CreateAsync(CreatePurchaseDto dto);
}
