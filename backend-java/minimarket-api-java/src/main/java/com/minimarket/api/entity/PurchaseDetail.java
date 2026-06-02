package com.minimarket.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "DetalleCompra")
public class PurchaseDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "CompraId", nullable = false, insertable = false, updatable = false)
    private Integer purchaseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CompraId")
    private Purchase purchase;

    @Column(name = "ProductoId", nullable = false)
    private Integer productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductoId", insertable = false, updatable = false)
    private Product product;

    @Column(name = "CantidadPaquetes", nullable = false)
    private Integer packageQuantity;

    @Column(name = "UnidadesPorPaquete", nullable = false)
    private Integer unitsPerPackage;

    @Column(name = "TotalUnidades", nullable = false)
    private Integer totalUnits;

    @Column(name = "CostoPaquete", nullable = false, precision = 10, scale = 2)
    private BigDecimal packageCost;

    @Column(name = "CostoUnitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "Subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "UnidadCompra", nullable = false, length = 30)
    private String purchaseUnitName;

    @Column(name = "CodigoBarrasLeido", length = 50)
    private String barcodeSnapshot;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Integer purchaseId) { this.purchaseId = purchaseId; }
    public Purchase getPurchase() { return purchase; }
    public void setPurchase(Purchase purchase) { this.purchase = purchase; }
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Integer getPackageQuantity() { return packageQuantity; }
    public void setPackageQuantity(Integer packageQuantity) { this.packageQuantity = packageQuantity; }
    public Integer getUnitsPerPackage() { return unitsPerPackage; }
    public void setUnitsPerPackage(Integer unitsPerPackage) { this.unitsPerPackage = unitsPerPackage; }
    public Integer getTotalUnits() { return totalUnits; }
    public void setTotalUnits(Integer totalUnits) { this.totalUnits = totalUnits; }
    public BigDecimal getPackageCost() { return packageCost; }
    public void setPackageCost(BigDecimal packageCost) { this.packageCost = packageCost; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public String getPurchaseUnitName() { return purchaseUnitName; }
    public void setPurchaseUnitName(String purchaseUnitName) { this.purchaseUnitName = purchaseUnitName; }
    public String getBarcodeSnapshot() { return barcodeSnapshot; }
    public void setBarcodeSnapshot(String barcodeSnapshot) { this.barcodeSnapshot = barcodeSnapshot; }
}
