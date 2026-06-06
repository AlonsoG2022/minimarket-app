using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface IPrintJobService
{
    Task<IReadOnlyCollection<PrintJobDto>> GetRecentAsync(int take = 20);
    Task<(bool Success, string? Error, PrintJobDto? Job)> EnqueueSaleTicketAsync(int saleId);
    Task<(bool Success, string? Error, PrintJobDto? Job)> RequeueAsync(int id);
}
