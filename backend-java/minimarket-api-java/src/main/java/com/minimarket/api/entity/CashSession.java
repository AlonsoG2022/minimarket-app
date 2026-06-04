package com.minimarket.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CajaSesiones")
public class CashSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "UsuarioId", nullable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UsuarioId", insertable = false, updatable = false)
    private User user;

    @Column(name = "FechaApertura", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "FechaCierre")
    private LocalDateTime closedAt;

    @Column(name = "MontoInicial", nullable = false, precision = 10, scale = 2)
    private BigDecimal openingAmount;

    @Column(name = "MontoEsperadoCierre", precision = 10, scale = 2)
    private BigDecimal closingExpectedAmount;

    @Column(name = "MontoContadoCierre", precision = 10, scale = 2)
    private BigDecimal closingCountedAmount;

    @Column(name = "Diferencia", precision = 10, scale = 2)
    private BigDecimal difference;

    @Column(name = "Estado", nullable = false, length = 20)
    private String status;

    @Column(name = "Notas", length = 250)
    private String notes;

    @OneToMany(mappedBy = "cashSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CashMovement> movements = new ArrayList<>();

    @OneToMany(mappedBy = "cashSession")
    private List<Sale> sales = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public BigDecimal getOpeningAmount() { return openingAmount; }
    public void setOpeningAmount(BigDecimal openingAmount) { this.openingAmount = openingAmount; }
    public BigDecimal getClosingExpectedAmount() { return closingExpectedAmount; }
    public void setClosingExpectedAmount(BigDecimal closingExpectedAmount) { this.closingExpectedAmount = closingExpectedAmount; }
    public BigDecimal getClosingCountedAmount() { return closingCountedAmount; }
    public void setClosingCountedAmount(BigDecimal closingCountedAmount) { this.closingCountedAmount = closingCountedAmount; }
    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<CashMovement> getMovements() { return movements; }
    public void setMovements(List<CashMovement> movements) { this.movements = movements; }
    public List<Sale> getSales() { return sales; }
    public void setSales(List<Sale> sales) { this.sales = sales; }
}
