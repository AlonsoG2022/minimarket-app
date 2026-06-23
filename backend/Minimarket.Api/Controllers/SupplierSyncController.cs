using Microsoft.AspNetCore.Mvc;
using Minimarket.Api.DTOs;
using Minimarket.Api.Services;

namespace Minimarket.Api.Controllers;

[ApiController]
[Route("api/supplier-sync")]
public class SupplierSyncController(ISupplierSyncService supplierSyncService) : ControllerBase
{
    // POST api/supplier-sync  -> ejecuta o previsualiza la sincronizacion del catalogo del proveedor.
    [HttpPost]
    public async Task<ActionResult<SupplierSyncResultDto>> Sync([FromBody] SupplierSyncRequestDto request)
    {
        var result = await supplierSyncService.SyncAsync(request);
        if (!result.Success)
        {
            return BadRequest(new { message = result.Error });
        }

        return Ok(result.Result);
    }
}
