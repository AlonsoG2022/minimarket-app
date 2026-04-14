namespace Minimarket.Api.DTOs;

public record CategoryDto(int Id, string Name, string? Description, bool IsActive);
