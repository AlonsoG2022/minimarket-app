package com.minimarket.api.controller;

import com.minimarket.api.dto.*;
import com.minimarket.api.service.CashSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/cash-sessions")
public class CashSessionsController {

    private final CashSessionService cashSessionService;

    public CashSessionsController(CashSessionService cashSessionService) {
        this.cashSessionService = cashSessionService;
    }

    @GetMapping("/current/{userId}")
    public ResponseEntity<CashSessionDto> getCurrent(@PathVariable Integer userId) {
        return ResponseEntity.ok(cashSessionService.getCurrent(userId));
    }

    @GetMapping("/user/{userId}")
    public List<CashSessionDto> getRecent(@PathVariable Integer userId) {
        return cashSessionService.getRecent(userId);
    }

    @PostMapping
    public ResponseEntity<?> open(@RequestBody OpenCashSessionDto dto) {
        var result = cashSessionService.open(dto);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/current/{userId}")
            .buildAndExpand(result.data().userId())
            .toUri();

        return ResponseEntity.created(location).body(result.data());
    }

    @PostMapping("/{sessionId}/movements")
    public ResponseEntity<?> addMovement(@PathVariable Integer sessionId, @RequestBody CreateCashMovementDto dto) {
        var result = cashSessionService.addMovement(sessionId, dto);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }

        return ResponseEntity.ok(result.data());
    }

    @PostMapping("/{sessionId}/close")
    public ResponseEntity<?> close(@PathVariable Integer sessionId, @RequestBody CloseCashSessionDto dto) {
        var result = cashSessionService.close(sessionId, dto);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }

        return ResponseEntity.ok(result.data());
    }
}
