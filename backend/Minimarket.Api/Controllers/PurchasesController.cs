using Microsoft.AspNetCore.Mvc;
using Minimarket.Api.DTOs;
using Minimarket.Api.Services;

namespace Minimarket.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class PurchasesController(IPurchaseService purchaseService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<IReadOnlyCollection<PurchaseDto>>> GetAll()
        => Ok(await purchaseService.GetAllAsync());

    [HttpGet("{id:int}")]
    public async Task<ActionResult<PurchaseDto>> GetById(int id)
    {
        var purchase = await purchaseService.GetByIdAsync(id);
        return purchase is null ? NotFound() : Ok(purchase);
    }

    [HttpPost]
    public async Task<ActionResult<PurchaseDto>> Create([FromBody] CreatePurchaseDto dto)
    {
        var result = await purchaseService.CreateAsync(dto);
        if (!result.Success)
        {
            return BadRequest(new { message = result.Error });
        }

        return CreatedAtAction(nameof(GetById), new { id = result.Purchase!.Id }, result.Purchase);
    }
}
