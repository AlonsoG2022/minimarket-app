using Microsoft.AspNetCore.Mvc;
using Minimarket.Api.DTOs;
using Minimarket.Api.Services;

namespace Minimarket.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class SuppliersController(ISupplierService supplierService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<IReadOnlyCollection<SupplierDto>>> GetAll()
        => Ok(await supplierService.GetAllAsync());

    [HttpGet("{id:int}")]
    public async Task<ActionResult<SupplierDto>> GetById(int id)
    {
        var supplier = await supplierService.GetByIdAsync(id);
        return supplier is null ? NotFound() : Ok(supplier);
    }

    [HttpPost]
    public async Task<ActionResult<SupplierDto>> Create([FromBody] SaveSupplierDto dto)
    {
        var result = await supplierService.CreateAsync(dto);
        if (!result.Success)
        {
            return BadRequest(new { message = result.Error });
        }

        return CreatedAtAction(nameof(GetById), new { id = result.Supplier!.Id }, result.Supplier);
    }

    [HttpPut("{id:int}")]
    public async Task<ActionResult<SupplierDto>> Update(int id, [FromBody] SaveSupplierDto dto)
    {
        var result = await supplierService.UpdateAsync(id, dto);
        if (!result.Success)
        {
            return result.Error == "Proveedor no encontrado."
                ? NotFound(new { message = result.Error })
                : BadRequest(new { message = result.Error });
        }

        return Ok(result.Supplier);
    }

    [HttpDelete("{id:int}")]
    public async Task<IActionResult> Delete(int id)
    {
        var result = await supplierService.DeleteAsync(id);
        if (!result.Success)
        {
            return NotFound(new { message = result.Error });
        }

        return NoContent();
    }
}
