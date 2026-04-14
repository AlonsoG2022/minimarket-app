using Microsoft.AspNetCore.Mvc;
using Minimarket.Api.DTOs;
using Minimarket.Api.Services;

namespace Minimarket.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class ReportsController(IReportService reportService) : ControllerBase
{
    [HttpGet("sales-summary")]
    public async Task<ActionResult<IReadOnlyCollection<SalesSummaryDto>>> GetSalesSummary(
        [FromQuery] DateTime? startDate,
        [FromQuery] DateTime? endDate)
    {
        var start = startDate?.Date ?? DateTime.Today.AddDays(-6);
        var end = endDate?.Date.AddDays(1).AddTicks(-1) ?? DateTime.Today.AddDays(1).AddTicks(-1);

        return Ok(await reportService.GetSalesSummaryAsync(start, end));
    }

    [HttpGet("dashboard")]
    public async Task<ActionResult<DashboardDto>> GetDashboard()
        => Ok(await reportService.GetDashboardAsync());
}
