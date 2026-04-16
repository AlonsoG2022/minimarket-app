package com.minimarket.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Categorias")
public class Category {

    @Id
    @Column(name = "Id")
    private Integer id;

    @Column(name = "Nombre", nullable = false, length = 100)
    private String name;

    @Column(name = "Descripcion", length = 250)
    private String description;

    @Column(name = "Activo", nullable = false)
    private Boolean isActive;

    @OneToMany(mappedBy = "category")
    private List<Product> products = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
}
