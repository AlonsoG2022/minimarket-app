using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface IReceiptService
{
    Task<(bool Success, string? Error, ReceiptScanResultDto? Result)> ScanAsync(ReceiptScanRequestDto request);
    Task<(bool Success, string? Error, ReceiptConfirmResultDto? Result)> ConfirmAsync(ReceiptConfirmRequestDto request);
}
