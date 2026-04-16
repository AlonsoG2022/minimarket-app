package com.minimarket.api.dto;

public record UserDto(Integer id, String fullName, String username, String role, Boolean isActive) {
}
