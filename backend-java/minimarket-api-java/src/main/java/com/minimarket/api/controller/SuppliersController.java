package com.minimarket.api.controller;

import com.minimarket.api.dto.ApiMessageResponse;
import com.minimarket.api.dto.SaveSupplierDto;
import com.minimarket.api.dto.SupplierDto;
import com.minimarket.api.service.SupplierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SuppliersController {

    private final SupplierService supplierService;

    public SuppliersController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<SupplierDto> getAll() {
        return supplierService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierDto> getById(@PathVariable Integer id) {
        var supplier = supplierService.getById(id);
        return supplier == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(supplier);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody SaveSupplierDto dto) {
        var result = supplierService.create(dto);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(result.data().id())
            .toUri();

        return ResponseEntity.created(location).body(result.data());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody SaveSupplierDto dto) {
        var result = supplierService.update(id, dto);
        if (!result.success()) {
            if ("Proveedor no encontrado.".equals(result.error())) {
                return ResponseEntity.status(404).body(new ApiMessageResponse(result.error()));
            }

            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }

        return ResponseEntity.ok(result.data());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        var result = supplierService.delete(id);
        if (!result.success()) {
            return ResponseEntity.status(404).body(new ApiMessageResponse(result.error()));
        }

        return ResponseEntity.noContent().build();
    }
}
