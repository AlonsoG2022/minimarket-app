package com.minimarket.api.controller;

import com.minimarket.api.dto.ApiMessageResponse;
import com.minimarket.api.dto.SupplierSyncRequestDto;
import com.minimarket.api.service.SupplierSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/supplier-sync")
public class SupplierSyncController {

    private final SupplierSyncService supplierSyncService;

    public SupplierSyncController(SupplierSyncService supplierSyncService) {
        this.supplierSyncService = supplierSyncService;
    }

    // POST /api/supplier-sync -> ejecuta o previsualiza la sincronizacion del catalogo del proveedor.
    @PostMapping
    public ResponseEntity<?> sync(@RequestBody SupplierSyncRequestDto request) {
        var result = supplierSyncService.sync(request);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }

        return ResponseEntity.ok(result.data());
    }
}
