package com.minimarket.api.controller;

import com.minimarket.api.dto.ApiMessageResponse;
import com.minimarket.api.dto.CreatePurchaseDto;
import com.minimarket.api.dto.PurchaseDto;
import com.minimarket.api.service.PurchaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchasesController {

    private final PurchaseService purchaseService;

    public PurchasesController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    public List<PurchaseDto> getAll() {
        return purchaseService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseDto> getById(@PathVariable Integer id) {
        var purchase = purchaseService.getById(id);
        return purchase == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(purchase);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreatePurchaseDto dto) {
        var result = purchaseService.create(dto);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(result.data().id())
            .toUri();

        return ResponseEntity.created(location).body(result.data());
    }
}
