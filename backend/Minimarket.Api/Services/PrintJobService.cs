using System.Text.Json;
using Minimarket.Api.DTOs;
using Minimarket.Api.Mapping;
using Minimarket.Api.Models;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

public class PrintJobService(
    IPrintJobRepository printJobRepository,
    ISaleRepository saleRepository) : IPrintJobService
{
    public async Task<IReadOnlyCollection<PrintJobDto>> GetRecentAsync(int take = 20) =>
        (await printJobRepository.GetRecentAsync(take)).Select(job => job.ToDto()).ToList();

    public async Task<(bool Success, string? Error, PrintJobDto? Job)> EnqueueSaleTicketAsync(int saleId)
    {
        var sale = await saleRepository.GetByIdAsync(saleId);
        if (sale is null)
        {
            return (false, "La venta no existe.", null);
        }

        var payload = new TicketPrintPayloadDto(
            sale.Id,
            sale.SaleDate,
            sale.User?.FullName ?? string.Empty,
            sale.PaymentMethod,
            sale.Total,
            sale.Notes,
            sale.Details.Select(detail => new TicketPrintItemDto(
                detail.Product?.Name ?? string.Empty,
                detail.Quantity,
                detail.UnitPrice,
                detail.Subtotal)).ToList());

        var job = new PrintJob
        {
            SaleId = sale.Id,
            SourceType = "sale",
            DocumentType = "ticket",
            Status = "pendiente",
            Attempts = 0,
            RequestedAt = DateTime.Now,
            PayloadJson = JsonSerializer.Serialize(payload)
        };

        await printJobRepository.AddAsync(job);
        await printJobRepository.SaveChangesAsync();
        return (true, null, (await printJobRepository.GetByIdAsync(job.Id))?.ToDto());
    }

    public async Task<(bool Success, string? Error, PrintJobDto? Job)> RequeueAsync(int id)
    {
        var job = await printJobRepository.GetByIdAsync(id);
        if (job is null)
        {
            return (false, "El trabajo de impresion no existe.", null);
        }

        job.Status = "pendiente";
        job.StartedAt = null;
        job.ProcessedAt = null;
        job.LastError = null;
        job.RequestedAt = DateTime.Now;

        await printJobRepository.SaveChangesAsync();
        return (true, null, job.ToDto());
    }
}
