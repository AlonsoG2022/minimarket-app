package com.minimarket.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CajaMovimientos")
public class CashMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "CajaSesionId", nullable = false)
    private Integer cashSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CajaSesionId", insertable = false, updatable = false)
    private CashSession cashSession;

    @Column(name = "FechaMovimiento", nullable = false)
    private LocalDateTime movementDate;

    @Column(name = "Tipo", nullable = false, length = 30)
    private String type;

    @Column(name = "Monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "Descripcion", length = 250)
    private String description;

    @Column(name = "TipoReferencia", length = 30)
    private String referenceType;

    @Column(name = "ReferenciaId")
    private Integer referenceId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCashSessionId() { return cashSessionId; }
    public void setCashSessionId(Integer cashSessionId) { this.cashSessionId = cashSessionId; }
    public CashSession getCashSession() { return cashSession; }
    public void setCashSession(CashSession cashSession) { this.cashSession = cashSession; }
    public LocalDateTime getMovementDate() { return movementDate; }
    public void setMovementDate(LocalDateTime movementDate) { this.movementDate = movementDate; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public Integer getReferenceId() { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }
}
