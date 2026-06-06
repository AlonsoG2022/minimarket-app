package com.minimarket.printworker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "TrabajosImpresion")
public class PrintJob {

    @Id
    private Integer id;

    @Column(name = "VentaId")
    private Integer saleId;

    @Column(name = "TipoOrigen")
    private String sourceType;

    @Column(name = "TipoDocumento")
    private String documentType;

    @Column(name = "Estado")
    private String status;

    @Column(name = "Intentos")
    private Integer attempts;

    @Column(name = "NombreImpresora")
    private String printerName;

    @Column(name = "SolicitadoEn")
    private LocalDateTime requestedAt;

    @Column(name = "ProcesandoEn")
    private LocalDateTime startedAt;

    @Column(name = "ProcesadoEn")
    private LocalDateTime processedAt;

    @Column(name = "UltimoError")
    private String lastError;

    @Column(name = "PayloadJson")
    private String payloadJson;

    public Integer getId() {
        return id;
    }

    public Integer getSaleId() {
        return saleId;
    }

    public void setSaleId(Integer saleId) {
        this.saleId = saleId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}
