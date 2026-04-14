namespace Minimarket.Api.DTOs;

public record UserDto(int Id, string FullName, string Username, string Role, bool IsActive);

public record CreateUserDto(string FullName, string Username, string Password, string Role, bool IsActive);

public record LoginRequestDto(string Username, string Password);

public record LoginResponseDto(int Id, string FullName, string Username, string Role);
