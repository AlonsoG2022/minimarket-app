package com.minimarket.api.repository;

import com.minimarket.api.entity.SupplierProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Integer> {
}
