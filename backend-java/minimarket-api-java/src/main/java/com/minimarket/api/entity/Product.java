package com.minimarket.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Productos")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "Nombre", nullable = false, length = 150)
    private String name;

    @Column(name = "Sku", nullable = false, length = 30)
    private String sku;

    @Column(name = "CodigoBarras", length = 50)
    private String barcode;

    @Column(name = "CodigoBarrasCompra", length = 50)
    private String purchaseBarcode;

    @Column(name = "Descripcion", length = 250)
    private String description;

    @Column(name = "Precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "Costo", nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "Stock", nullable = false)
    private Integer stock;

    @Column(name = "StockMinimo", nullable = false)
    private Integer minimumStock;

    @Column(name = "FechaCaducidad")
    private LocalDate expirationDate;

    @Column(name = "UnidadVenta", nullable = false, length = 30)
    private String salesUnitName;

    @Column(name = "UnidadCompra", nullable = false, length = 30)
    private String purchaseUnitName;

    @Column(name = "UnidadesPorCompra", nullable = false)
    private Integer unitsPerPurchaseUnit;

    @Column(name = "Activo", nullable = false)
    private Boolean isActive;

    @Column(name = "CategoriaId", nullable = false)
    private Integer categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoriaId", insertable = false, updatable = false)
    private Category category;

    @OneToMany(mappedBy = "product")
    private List<SaleDetail> saleDetails = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<PurchaseDetail> purchaseDetails = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getPurchaseBarcode() { return purchaseBarcode; }
    public void setPurchaseBarcode(String purchaseBarcode) { this.purchaseBarcode = purchaseBarcode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getMinimumStock() { return minimumStock; }
    public void setMinimumStock(Integer minimumStock) { this.minimumStock = minimumStock; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }
    public String getSalesUnitName() { return salesUnitName; }
    public void setSalesUnitName(String salesUnitName) { this.salesUnitName = salesUnitName; }
    public String getPurchaseUnitName() { return purchaseUnitName; }
    public void setPurchaseUnitName(String purchaseUnitName) { this.purchaseUnitName = purchaseUnitName; }
    public Integer getUnitsPerPurchaseUnit() { return unitsPerPurchaseUnit; }
    public void setUnitsPerPurchaseUnit(Integer unitsPerPurchaseUnit) { this.unitsPerPurchaseUnit = unitsPerPurchaseUnit; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public List<SaleDetail> getSaleDetails() { return saleDetails; }
    public void setSaleDetails(List<SaleDetail> saleDetails) { this.saleDetails = saleDetails; }
    public List<PurchaseDetail> getPurchaseDetails() { return purchaseDetails; }
    public void setPurchaseDetails(List<PurchaseDetail> purchaseDetails) { this.purchaseDetails = purchaseDetails; }
}
