using Microsoft.AspNetCore.Mvc;
using Minimarket.Api.DTOs;
using Minimarket.Api.Services;

namespace Minimarket.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class CategoriesController(ICategoryService categoryService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<IReadOnlyCollection<CategoryDto>>> GetAll([FromQuery] bool includeInactive = false)
        => Ok(await categoryService.GetAllAsync(includeInactive));

    [HttpPost]
    public async Task<ActionResult<CategoryDto>> Create([FromBody] SaveCategoryDto dto)
    {
        var result = await categoryService.CreateAsync(dto);
        if (!result.Success)
        {
            return BadRequest(new { message = result.Error });
        }

        return CreatedAtAction(nameof(GetAll), new { id = result.Category!.Id }, result.Category);
    }

    [HttpPut("{id:int}")]
    public async Task<ActionResult<CategoryDto>> Update(int id, [FromBody] SaveCategoryDto dto)
    {
        var result = await categoryService.UpdateAsync(id, dto);
        if (!result.Success)
        {
            return result.Error == "Categoria no encontrada."
                ? NotFound(new { message = result.Error })
                : BadRequest(new { message = result.Error });
        }

        return Ok(result.Category);
    }
}
