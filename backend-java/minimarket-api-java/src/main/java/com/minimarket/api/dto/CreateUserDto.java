package com.minimarket.api.dto;

public record CreateUserDto(String fullName, String username, String password, String role, Boolean isActive) {
}
