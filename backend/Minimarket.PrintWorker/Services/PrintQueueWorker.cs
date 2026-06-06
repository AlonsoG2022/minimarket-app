using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using Minimarket.PrintWorker.Data;
using Minimarket.PrintWorker.DTOs;
using Minimarket.PrintWorker.Models;
using Minimarket.PrintWorker.Options;

namespace Minimarket.PrintWorker.Services;

public class PrintQueueWorker(
    IServiceScopeFactory scopeFactory,
    IPrinterDispatcher printerDispatcher,
    IOptions<PrintingOptions> options,
    ILogger<PrintQueueWorker> logger) : BackgroundService
{
    private readonly JsonSerializerOptions jsonOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        logger.LogInformation("Worker de impresion iniciado.");

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                while (await ProcessNextJobAsync(stoppingToken))
                {
                }
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Ocurrio un error procesando la cola de impresion.");
            }

            await Task.Delay(TimeSpan.FromSeconds(Math.Max(2, options.Value.PollSeconds)), stoppingToken);
        }
    }

    private async Task<bool> ProcessNextJobAsync(CancellationToken cancellationToken)
    {
        await using var scope = scopeFactory.CreateAsyncScope();
        var context = scope.ServiceProvider.GetRequiredService<PrintWorkerDbContext>();

        var job = await context.PrintJobs
            .OrderBy(x => x.RequestedAt)
            .ThenBy(x => x.Id)
            .FirstOrDefaultAsync(x => x.Status == "pendiente", cancellationToken);

        if (job is null)
        {
            return false;
        }

        job.Status = "imprimiendo";
        job.Attempts += 1;
        job.StartedAt = DateTime.Now;
        job.ProcessedAt = null;
        job.LastError = null;
        await context.SaveChangesAsync(cancellationToken);

        try
        {
            if (!string.Equals(job.DocumentType, "ticket", StringComparison.OrdinalIgnoreCase))
            {
                throw new InvalidOperationException($"Tipo de documento no soportado por el worker actual: {job.DocumentType}");
            }

            var payload = JsonSerializer.Deserialize<TicketPrintPayload>(job.PayloadJson, jsonOptions)
                ?? throw new InvalidOperationException("No se pudo deserializar el payload del ticket.");

            var usedPrinter = await printerDispatcher.PrintTicketAsync(job.PrinterName, payload, cancellationToken);
            job.PrinterName = usedPrinter;
            job.Status = "impreso";
            job.ProcessedAt = DateTime.Now;
            job.LastError = null;
            await context.SaveChangesAsync(cancellationToken);

            logger.LogInformation("Trabajo de impresion {JobId} procesado correctamente.", job.Id);
            return true;
        }
        catch (Exception ex)
        {
            job.Status = "error";
            job.ProcessedAt = DateTime.Now;
            job.LastError = Truncate(ex.Message, 500);
            await context.SaveChangesAsync(cancellationToken);

            logger.LogError(ex, "El trabajo de impresion {JobId} fallo.", job.Id);
            return true;
        }
    }

    private static string Truncate(string value, int maxLength) =>
        value.Length <= maxLength ? value : value[..maxLength];
}
