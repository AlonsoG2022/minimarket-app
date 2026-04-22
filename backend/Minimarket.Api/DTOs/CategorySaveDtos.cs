namespace Minimarket.Api.DTOs;

public record SaveCategoryDto(
    string Name,
    string? Description,
    bool IsActive);
