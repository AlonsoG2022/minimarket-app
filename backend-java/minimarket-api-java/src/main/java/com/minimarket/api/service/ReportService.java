package com.minimarket.api.service;

import com.minimarket.api.dto.DashboardDto;
import com.minimarket.api.dto.SalesSummaryDto;
import com.minimarket.api.dto.TopSellingProductDto;
import com.minimarket.api.repository.ProductRepository;
import com.minimarket.api.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ReportService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;

    public ReportService(SaleRepository saleRepository, ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
    }

    public List<SalesSummaryDto> getSalesSummary(LocalDateTime startDate, LocalDateTime endDate) {
        record SummaryAccumulator(BigDecimal total, int count) {}

        Map<LocalDate, SummaryAccumulator> grouped = new TreeMap<>();

        for (var sale : saleRepository.findBySaleDateBetween(startDate, endDate)) {
            var date = sale.getSaleDate().toLocalDate();
            var current = grouped.get(date);

            if (current == null) {
                grouped.put(date, new SummaryAccumulator(sale.getTotal(), 1));
            } else {
                grouped.put(
                    date,
                    new SummaryAccumulator(current.total().add(sale.getTotal()), current.count() + 1)
                );
            }
        }

        return grouped.entrySet()
            .stream()
            .map(entry -> new SalesSummaryDto(entry.getKey(), entry.getValue().total(), entry.getValue().count()))
            .toList();
    }

    public List<TopSellingProductDto> getTopSellingProducts(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        return saleRepository.findTopSellingProducts(startDate, endDate)
            .stream()
            .limit(limit)
            .map(row -> new TopSellingProductDto(
                row.getProductId(),
                row.getProductName(),
                row.getSku(),
                row.getTotalQuantity(),
                row.getTotalAmount()
            ))
            .toList();
    }

    public DashboardDto getDashboard() {
        var dayStart = LocalDate.now().atStartOfDay();
        var dayEnd = dayStart.plusDays(1).minusNanos(1);
        var todaySales = saleRepository.getTotalBySaleDateBetween(dayStart, dayEnd);
        var todayTransactions = (int) saleRepository.countBySaleDateBetween(dayStart, dayEnd);
        var lowStockProducts = (int) productRepository.findAll()
            .stream()
            .filter(product -> product.getStock() <= product.getMinimumStock())
            .count();

        return new DashboardDto(
            todaySales,
            todayTransactions,
            Math.toIntExact(productRepository.count()),
            lowStockProducts
        );
    }
}
