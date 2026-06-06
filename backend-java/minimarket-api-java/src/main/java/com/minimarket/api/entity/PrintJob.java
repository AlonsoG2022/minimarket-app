package com.minimarket.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TrabajosImpresion")
public class PrintJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "VentaId")
    private Integer saleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VentaId", insertable = false, updatable = false)
    private Sale sale;

    @Column(name = "TipoOrigen", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "TipoDocumento", nullable = false, length = 20)
    private String documentType;

    @Column(name = "Estado", nullable = false, length = 20)
    private String status;

    @Column(name = "Intentos", nullable = false)
    private Integer attempts;

    @Column(name = "NombreImpresora", length = 120)
    private String printerName;

    @Column(name = "SolicitadoEn", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "ProcesandoEn")
    private LocalDateTime startedAt;

    @Column(name = "ProcesadoEn")
    private LocalDateTime processedAt;

    @Column(name = "UltimoError", length = 500)
    private String lastError;

    @Column(name = "PayloadJson", nullable = false, columnDefinition = "nvarchar(max)")
    private String payloadJson;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getSaleId() { return saleId; }
    public void setSaleId(Integer saleId) { this.saleId = saleId; }
    public Sale getSale() { return sale; }
    public void setSale(Sale sale) { this.sale = sale; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }
    public String getPrinterName() { return printerName; }
    public void setPrinterName(String printerName) { this.printerName = printerName; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
}
