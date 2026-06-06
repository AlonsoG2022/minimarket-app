package com.minimarket.printworker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.printworker.dto.TicketPrintPayloadDto;
import com.minimarket.printworker.entity.PrintJob;
import com.minimarket.printworker.repository.PrintJobRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PrintQueueWorkerService {

    private static final Logger logger = LoggerFactory.getLogger(PrintQueueWorkerService.class);

    private final PrintJobRepository printJobRepository;
    private final PrinterDispatcher printerDispatcher;
    private final ObjectMapper objectMapper;

    public PrintQueueWorkerService(PrintJobRepository printJobRepository, PrinterDispatcher printerDispatcher, ObjectMapper objectMapper) {
        this.printJobRepository = printJobRepository;
        this.printerDispatcher = printerDispatcher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${printing.poll-ms:5000}")
    public void processQueue() {
        while (processNext()) {
        }
    }

    private boolean processNext() {
        var job = claimNextPending();
        if (job == null) {
            return false;
        }

        try {
            if (!"ticket".equalsIgnoreCase(job.getDocumentType())) {
                throw new IllegalStateException("Tipo de documento no soportado por este worker: " + job.getDocumentType());
            }

            var payload = objectMapper.readValue(job.getPayloadJson(), TicketPrintPayloadDto.class);
            var usedPrinter = printerDispatcher.printTicket(job.getPrinterName(), payload);
            markSuccess(job.getId(), usedPrinter);
            logger.info("Trabajo de impresion {} procesado correctamente.", job.getId());
        } catch (Exception ex) {
            markError(job.getId(), ex.getMessage());
            logger.error("El trabajo de impresion {} fallo.", job.getId(), ex);
        }

        return true;
    }

    @Transactional
    protected PrintJob claimNextPending() {
        var job = printJobRepository.findFirstByStatusOrderByRequestedAtAscIdAsc("pendiente").orElse(null);
        if (job == null) {
            return null;
        }

        job.setStatus("imprimiendo");
        job.setAttempts((job.getAttempts() == null ? 0 : job.getAttempts()) + 1);
        job.setStartedAt(LocalDateTime.now());
        job.setProcessedAt(null);
        job.setLastError(null);
        return printJobRepository.save(job);
    }

    @Transactional
    protected void markSuccess(Integer id, String printerName) {
        var job = printJobRepository.findById(id).orElseThrow();
        job.setPrinterName(printerName);
        job.setStatus("impreso");
        job.setProcessedAt(LocalDateTime.now());
        job.setLastError(null);
        printJobRepository.save(job);
    }

    @Transactional
    protected void markError(Integer id, String error) {
        var job = printJobRepository.findById(id).orElseThrow();
        job.setStatus("error");
        job.setProcessedAt(LocalDateTime.now());
        job.setLastError(error == null ? "Error desconocido." : truncate(error, 500));
        printJobRepository.save(job);
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
