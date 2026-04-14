using Minimarket.Api.DTOs;

namespace Minimarket.Api.Services;

public interface IUserService
{
    Task<IReadOnlyCollection<UserDto>> GetAllAsync();
    Task<(bool Success, string? Error, UserDto? User)> CreateAsync(CreateUserDto dto);
    Task<LoginResponseDto?> LoginAsync(LoginRequestDto dto);
}
