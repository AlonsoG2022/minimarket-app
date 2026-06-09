using Microsoft.AspNetCore.Mvc;
using Minimarket.Api.DTOs;
using Minimarket.Api.Services;

namespace Minimarket.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class CompanyController(ICompanyService companyService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<CompanyDto>> Get()
    {
        var company = await companyService.GetAsync();
        return company is null ? NotFound() : Ok(company);
    }

    [HttpPut]
    public async Task<ActionResult<CompanyDto>> Update([FromBody] SaveCompanyDto dto)
    {
        var result = await companyService.UpdateAsync(dto);
        if (!result.Success)
            return BadRequest(new { message = result.Error });

        return Ok(result.Company);
    }
}
