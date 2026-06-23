using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface ISupplierSyncService
{
    Task<(bool Success, string? Error, SupplierSyncResultDto? Result)> SyncAsync(SupplierSyncRequestDto request);
}
