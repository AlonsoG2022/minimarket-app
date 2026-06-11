package com.minimarket.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ConfiguracionEmpresa")
public class Company {

    @Id
    @Column(name = "Id")
    private Integer id;

    @Column(name = "NombreComercial", nullable = false, length = 150)
    private String businessName;

    @Column(name = "RazonSocial", nullable = false, length = 200)
    private String legalName;

    @Column(name = "Ruc", nullable = false, length = 20)
    private String taxId;

    @Column(name = "Direccion", nullable = false, length = 250)
    private String addressLine;

    @Column(name = "Telefono", nullable = false, length = 50)
    private String phone;

    @Column(name = "Eslogan", nullable = false, length = 250)
    private String tagline;

    @Column(name = "TituloDocumento", nullable = false, length = 100)
    private String documentTitle;

    @Column(name = "EtiquetaCliente", nullable = false, length = 100)
    private String customerLabel;

    @Column(name = "PiePagina1", nullable = false, length = 150)
    private String footerLine1;

    @Column(name = "PiePagina2", nullable = false, length = 150)
    private String footerLine2;

    @Column(name = "MostrarVistaPreviaTicket", nullable = false)
    private Boolean showTicketPreview;

    @Column(name = "StockMinimoDefault", nullable = false)
    private Integer minimumStock;

    @Column(name = "Tema", nullable = false, length = 20)
    private String theme;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }
    public String getCustomerLabel() { return customerLabel; }
    public void setCustomerLabel(String customerLabel) { this.customerLabel = customerLabel; }
    public String getFooterLine1() { return footerLine1; }
    public void setFooterLine1(String footerLine1) { this.footerLine1 = footerLine1; }
    public String getFooterLine2() { return footerLine2; }
    public void setFooterLine2(String footerLine2) { this.footerLine2 = footerLine2; }
    public Boolean getShowTicketPreview() { return showTicketPreview; }
    public void setShowTicketPreview(Boolean showTicketPreview) { this.showTicketPreview = showTicketPreview; }
    public Integer getMinimumStock() { return minimumStock; }
    public void setMinimumStock(Integer minimumStock) { this.minimumStock = minimumStock; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
}
