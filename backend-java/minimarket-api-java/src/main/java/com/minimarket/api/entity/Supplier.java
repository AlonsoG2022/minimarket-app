package com.minimarket.api.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Proveedores")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "Nombre", nullable = false, length = 150)
    private String name;

    @Column(name = "NumeroDocumento", length = 30)
    private String documentNumber;

    @Column(name = "NombreContacto", length = 120)
    private String contactName;

    @Column(name = "Telefono", length = 30)
    private String phone;

    @Column(name = "Correo", length = 120)
    private String email;

    @Column(name = "Direccion", length = 250)
    private String address;

    @Column(name = "Notas", length = 250)
    private String notes;

    @Column(name = "Activo", nullable = false)
    private Boolean isActive;

    @OneToMany(mappedBy = "supplier")
    private List<Purchase> purchases = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
    public List<Purchase> getPurchases() { return purchases; }
    public void setPurchases(List<Purchase> purchases) { this.purchases = purchases; }
}
