using Microsoft.AspNetCore.Mvc;
using Minimarket.Api.DTOs;
using Minimarket.Api.Services;

namespace Minimarket.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class PrintJobsController(IPrintJobService printJobService) : ControllerBase
{
    [HttpGet("recent")]
    public async Task<ActionResult<IReadOnlyCollection<PrintJobDto>>> GetRecent([FromQuery] int take = 20) =>
        Ok(await printJobService.GetRecentAsync(Math.Clamp(take, 1, 100)));

    [HttpPost("sales/{saleId:int}/enqueue")]
    public async Task<ActionResult<PrintJobDto>> EnqueueSaleTicket(int saleId)
    {
        var result = await printJobService.EnqueueSaleTicketAsync(saleId);
        if (!result.Success)
        {
            return BadRequest(new { message = result.Error });
        }

        return Ok(result.Job);
    }

    [HttpPost("{id:int}/requeue")]
    public async Task<ActionResult<PrintJobDto>> Requeue(int id)
    {
        var result = await printJobService.RequeueAsync(id);
        if (!result.Success)
        {
            return BadRequest(new { message = result.Error });
        }

        return Ok(result.Job);
    }
}
