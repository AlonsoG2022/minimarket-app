package com.minimarket.api.service;

import com.minimarket.api.dto.*;
import com.minimarket.api.entity.User;
import com.minimarket.api.repository.UserRepository;
import com.minimarket.api.util.DtoMapper;
import com.minimarket.api.util.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public List<UserDto> getAll() {
        return userRepository.findAllByOrderByFullNameAsc()
            .stream()
            .map(DtoMapper::toDto)
            .toList();
    }

    @Transactional
    public ServiceResult<UserDto> create(CreateUserDto dto) {
        var username = dto.username().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            return ServiceResult.failure("El nombre de usuario ya esta registrado.");
        }

        var user = new User();
        user.setFullName(dto.fullName().trim());
        user.setUsername(username);
        user.setPasswordHash(passwordHasher.hash(dto.password()));
        user.setRole(dto.role().trim().toLowerCase());
        user.setIsActive(dto.isActive());

        var saved = userRepository.save(user);
        return ServiceResult.success(DtoMapper.toDto(saved));
    }

    public LoginResponseDto login(LoginRequestDto dto) {
        var user = userRepository.findByUsername(dto.username().trim()).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive()) || !passwordHasher.verify(dto.password(), user.getPasswordHash())) {
            return null;
        }

        return new LoginResponseDto(user.getId(), user.getFullName(), user.getUsername(), user.getRole());
    }
}
