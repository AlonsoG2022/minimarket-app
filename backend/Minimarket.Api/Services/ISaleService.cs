using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface ISaleService
{
    Task<IReadOnlyCollection<SaleDto>> GetAllAsync();
    Task<SaleDto?> GetByIdAsync(int id);
    Task<(bool Success, string? Error, SaleDto? Sale)> CreateAsync(CreateSaleDto dto);
}
