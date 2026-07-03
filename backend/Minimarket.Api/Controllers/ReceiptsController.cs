using Microsoft.AspNetCore.Mvc;
using Minimarket.Api.DTOs;
using Minimarket.Api.Services;

namespace Minimarket.Api.Controllers;

[ApiController]
[Route("api/receipts")]
public class ReceiptsController(IReceiptService receiptService) : ControllerBase
{
    // POST api/receipts/scan -> lee la foto de la boleta con IA y propone productos/costos.
    [HttpPost("scan")]
    public async Task<ActionResult<ReceiptScanResultDto>> Scan([FromBody] ReceiptScanRequestDto request)
    {
        var result = await receiptService.ScanAsync(request);
        if (!result.Success)
        {
            return BadRequest(new { message = result.Error });
        }

        return Ok(result.Result);
    }

    // POST api/receipts/confirm -> guarda los productos/costos confirmados por el usuario.
    [HttpPost("confirm")]
    public async Task<ActionResult<ReceiptConfirmResultDto>> Confirm([FromBody] ReceiptConfirmRequestDto request)
    {
        var result = await receiptService.ConfirmAsync(request);
        if (!result.Success)
        {
            return BadRequest(new { message = result.Error });
        }

        return Ok(result.Result);
    }
}
