package com.minimarket.api.controller;

import com.minimarket.api.dto.ApiMessageResponse;
import com.minimarket.api.dto.CreateSaleDto;
import com.minimarket.api.dto.SaleDto;
import com.minimarket.api.service.SaleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    private final SaleService saleService;

    public SalesController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public List<SaleDto> getAll() {
        return saleService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleDto> getById(@PathVariable Integer id) {
        var sale = saleService.getById(id);
        return sale == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(sale);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateSaleDto dto) {
        var result = saleService.create(dto);
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
