package com.minimarket.api.controller;

import com.minimarket.api.dto.DashboardDto;
import com.minimarket.api.dto.SalesSummaryDto;
import com.minimarket.api.dto.TopSellingProductDto;
import com.minimarket.api.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {

    private final ReportService reportService;

    public ReportsController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales-summary")
    public List<SalesSummaryDto> getSalesSummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        var start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(6).atStartOfDay();
        var end = endDate != null
            ? endDate.plusDays(1).atStartOfDay().minusNanos(1)
            : LocalDate.now().plusDays(1).atStartOfDay().minusNanos(1);

        return reportService.getSalesSummary(start, end);
    }

    @GetMapping("/top-products")
    public List<TopSellingProductDto> getTopProducts(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Integer limit
    ) {
        var start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(6).atStartOfDay();
        var end = endDate != null
            ? endDate.plusDays(1).atStartOfDay().minusNanos(1)
            : LocalDate.now().plusDays(1).atStartOfDay().minusNanos(1);
        var topLimit = limit != null && limit > 0 && limit <= 20 ? limit : 5;

        return reportService.getTopSellingProducts(start, end, topLimit);
    }

    @GetMapping("/dashboard")
    public DashboardDto getDashboard() {
        return reportService.getDashboard();
    }
}
