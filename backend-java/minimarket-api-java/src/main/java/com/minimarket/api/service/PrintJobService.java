package com.minimarket.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.api.dto.PrintJobDto;
import com.minimarket.api.dto.TicketPrintItemDto;
import com.minimarket.api.dto.TicketPrintPayloadDto;
import com.minimarket.api.entity.PrintJob;
import com.minimarket.api.repository.PrintJobRepository;
import com.minimarket.api.repository.SaleRepository;
import com.minimarket.api.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PrintJobService {

    private final PrintJobRepository printJobRepository;
    private final SaleRepository saleRepository;
    private final ObjectMapper objectMapper;

    public PrintJobService(PrintJobRepository printJobRepository, SaleRepository saleRepository, ObjectMapper objectMapper) {
        this.printJobRepository = printJobRepository;
        this.saleRepository = saleRepository;
        this.objectMapper = objectMapper;
    }

    public List<PrintJobDto> getRecent() {
        return printJobRepository.findTop20ByOrderByRequestedAtDesc()
            .stream()
            .map(DtoMapper::toDto)
            .toList();
    }

    @Transactional
    public ServiceResult<PrintJobDto> enqueueSaleTicket(Integer saleId) {
        var sale = saleRepository.findWithRelationsById(saleId).orElse(null);
        if (sale == null) {
            return ServiceResult.failure("La venta no existe.");
        }

        try {
            var payload = new TicketPrintPayloadDto(
                sale.getId(),
                sale.getSaleDate(),
                sale.getUser() != null ? sale.getUser().getFullName() : "",
                sale.getPaymentMethod(),
                sale.getTotal(),
                sale.getNotes(),
                sale.getDetails().stream()
                    .map(detail -> new TicketPrintItemDto(
                        detail.getProduct() != null ? detail.getProduct().getName() : "",
                        detail.getQuantity(),
                        detail.getUnitPrice(),
                        detail.getSubtotal()
                    ))
                    .toList()
            );

            var job = new PrintJob();
            job.setSaleId(sale.getId());
            job.setSourceType("sale");
            job.setDocumentType("ticket");
            job.setStatus("pendiente");
            job.setAttempts(0);
            job.setRequestedAt(LocalDateTime.now());
            job.setPayloadJson(objectMapper.writeValueAsString(payload));

            var saved = printJobRepository.save(job);
            return ServiceResult.success(DtoMapper.toDto(saved));
        } catch (Exception ex) {
            return ServiceResult.failure("No se pudo generar el payload de impresion: " + ex.getMessage());
        }
    }

    @Transactional
    public ServiceResult<PrintJobDto> requeue(Integer id) {
        var job = printJobRepository.findById(id).orElse(null);
        if (job == null) {
            return ServiceResult.failure("El trabajo de impresion no existe.");
        }

        job.setStatus("pendiente");
        job.setStartedAt(null);
        job.setProcessedAt(null);
        job.setLastError(null);
        job.setRequestedAt(LocalDateTime.now());

        return ServiceResult.success(DtoMapper.toDto(printJobRepository.save(job)));
    }
}
