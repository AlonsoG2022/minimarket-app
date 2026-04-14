using Minimarket.Api.DTOs;
using Minimarket.Api.Helpers;
using Minimarket.Api.Mapping;
using Minimarket.Api.Models;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

public class UserService(IUserRepository userRepository, IPasswordHasher passwordHasher) : IUserService
{
    public async Task<IReadOnlyCollection<UserDto>> GetAllAsync() =>
        (await userRepository.GetAllAsync()).Select(x => x.ToDto()).ToList();

    public async Task<(bool Success, string? Error, UserDto? User)> CreateAsync(CreateUserDto dto)
    {
        if (await userRepository.GetByUsernameAsync(dto.Username.Trim()) is not null)
        {
            return (false, "El nombre de usuario ya esta registrado.", null);
        }

        var user = new User
        {
            FullName = dto.FullName.Trim(),
            Username = dto.Username.Trim(),
            PasswordHash = passwordHasher.Hash(dto.Password),
            Role = dto.Role.Trim().ToLowerInvariant(),
            IsActive = dto.IsActive
        };

        await userRepository.AddAsync(user);
        await userRepository.SaveChangesAsync();
        return (true, null, user.ToDto());
    }

    public async Task<LoginResponseDto?> LoginAsync(LoginRequestDto dto)
    {
        var user = await userRepository.GetByUsernameAsync(dto.Username.Trim());
        if (user is null || !user.IsActive || !passwordHasher.Verify(dto.Password, user.PasswordHash))
        {
            return null;
        }

        return new LoginResponseDto(user.Id, user.FullName, user.Username, user.Role);
    }
}
