using Minimarket.PrintWorker.DTOs;

namespace Minimarket.PrintWorker.Services;

public interface IPrinterDispatcher
{
    Task<string> PrintTicketAsync(string? printerName, TicketPrintPayload payload, CancellationToken cancellationToken);
}
