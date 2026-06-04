using Microsoft.AspNetCore.Mvc;
using Minimarket.Api.DTOs;
using Minimarket.Api.Services;

namespace Minimarket.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class CashSessionsController(ICashSessionService cashSessionService) : ControllerBase
{
    [HttpGet("current/{userId:int}")]
    public async Task<ActionResult<CashSessionDto?>> GetCurrent(int userId)
    {
        var session = await cashSessionService.GetCurrentAsync(userId);
        return Ok(session);
    }

    [HttpGet("user/{userId:int}")]
    public async Task<ActionResult<IReadOnlyCollection<CashSessionDto>>> GetRecentByUser(int userId) =>
        Ok(await cashSessionService.GetRecentByUserAsync(userId));

    [HttpPost]
    public async Task<ActionResult<CashSessionDto>> Open([FromBody] OpenCashSessionDto dto)
    {
        var result = await cashSessionService.OpenAsync(dto);
        if (!result.Success)
        {
            return BadRequest(new { message = result.Error });
        }

        return CreatedAtAction(nameof(GetCurrent), new { userId = result.Session!.UserId }, result.Session);
    }

    [HttpPost("{sessionId:int}/movements")]
    public async Task<ActionResult<CashSessionDto>> AddMovement(int sessionId, [FromBody] CreateCashMovementDto dto)
    {
        var result = await cashSessionService.AddMovementAsync(sessionId, dto);
        if (!result.Success)
        {
            return BadRequest(new { message = result.Error });
        }

        return Ok(result.Session);
    }

    [HttpPost("{sessionId:int}/close")]
    public async Task<ActionResult<CashSessionDto>> Close(int sessionId, [FromBody] CloseCashSessionDto dto)
    {
        var result = await cashSessionService.CloseAsync(sessionId, dto);
        if (!result.Success)
        {
            return BadRequest(new { message = result.Error });
        }

        return Ok(result.Session);
    }
}
