package com.minimarket.api.repository;

import com.minimarket.api.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    List<Supplier> findAllByOrderByNameAsc();
    Optional<Supplier> findByDocumentNumber(String documentNumber);
}
