package com.minimarket.api.controller;

import com.minimarket.api.dto.ApiMessageResponse;
import com.minimarket.api.dto.CreateUserDto;
import com.minimarket.api.dto.UserDto;
import com.minimarket.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> getAll() {
        return userService.getAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateUserDto dto) {
        var result = userService.create(dto);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }

        return ResponseEntity.created(URI.create("/api/users/" + result.data().id())).body(result.data());
    }
}
