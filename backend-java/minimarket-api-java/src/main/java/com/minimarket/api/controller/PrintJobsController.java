package com.minimarket.api.controller;

import com.minimarket.api.dto.ApiMessageResponse;
import com.minimarket.api.dto.PrintJobDto;
import com.minimarket.api.service.PrintJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/print-jobs")
public class PrintJobsController {

    private final PrintJobService printJobService;

    public PrintJobsController(PrintJobService printJobService) {
        this.printJobService = printJobService;
    }

    @GetMapping("/recent")
    public List<PrintJobDto> getRecent() {
        return printJobService.getRecent();
    }

    @PostMapping("/sales/{saleId}/enqueue")
    public ResponseEntity<?> enqueueSaleTicket(@PathVariable Integer saleId) {
        var result = printJobService.enqueueSaleTicket(saleId);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }

        return ResponseEntity.ok(result.data());
    }

    @PostMapping("/{id}/requeue")
    public ResponseEntity<?> requeue(@PathVariable Integer id) {
        var result = printJobService.requeue(id);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }

        return ResponseEntity.ok(result.data());
    }
}
