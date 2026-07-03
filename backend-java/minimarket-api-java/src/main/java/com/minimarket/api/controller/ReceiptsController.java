package com.minimarket.api.controller;

import com.minimarket.api.dto.ApiMessageResponse;
import com.minimarket.api.dto.ReceiptConfirmRequestDto;
import com.minimarket.api.dto.ReceiptScanRequestDto;
import com.minimarket.api.service.ReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptsController {

    private final ReceiptService receiptService;

    public ReceiptsController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    // POST /api/receipts/scan -> lee la foto de la boleta con IA y propone productos/costos.
    @PostMapping("/scan")
    public ResponseEntity<?> scan(@RequestBody ReceiptScanRequestDto request) {
        var result = receiptService.scan(request);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }
        return ResponseEntity.ok(result.data());
    }

    // POST /api/receipts/confirm -> guarda los productos/costos confirmados por el usuario.
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody ReceiptConfirmRequestDto request) {
        var result = receiptService.confirm(request);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }
        return ResponseEntity.ok(result.data());
    }
}
