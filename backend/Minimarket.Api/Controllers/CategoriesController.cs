using Microsoft.AspNetCore.Mvc;
using Minimarket.Api.DTOs;
using Minimarket.Api.Services;

namespace Minimarket.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class CategoriesController(ICategoryService categoryService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<IReadOnlyCollection<CategoryDto>>> GetAll()
        => Ok(await categoryService.GetAllAsync());

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
}
