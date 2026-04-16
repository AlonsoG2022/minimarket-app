package com.minimarket.api.controller;

import com.minimarket.api.dto.ApiMessageResponse;
import com.minimarket.api.dto.LoginRequestDto;
import com.minimarket.api.dto.LoginResponseDto;
import com.minimarket.api.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto dto) {
        LoginResponseDto user = userService.login(dto);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiMessageResponse("Credenciales invalidas."));
        }

        return ResponseEntity.ok(user);
    }
}
