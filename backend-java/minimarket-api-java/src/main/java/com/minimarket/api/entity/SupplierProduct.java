package com.minimarket.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Historico del costo de compra de un producto por proveedor.
// Se alimenta desde la sincronizacion del catalogo del proveedor (ej. Coca-Cola / AIC Digital).
@Entity
@Table(name = "ProveedorProducto")
public class SupplierProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "ProveedorId", nullable = false)
    private Integer supplierId;

    @Column(name = "ProductoId", nullable = false)
    private Integer productId;

    @Column(name = "UltimoCosto", nullable = false, precision = 10, scale = 2)
    private BigDecimal lastCost;

    @Column(name = "Fecha", nullable = false)
    private LocalDateTime date;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public BigDecimal getLastCost() { return lastCost; }
    public void setLastCost(BigDecimal lastCost) { this.lastCost = lastCost; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
}
