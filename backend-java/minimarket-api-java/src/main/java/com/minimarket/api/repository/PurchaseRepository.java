package com.minimarket.api.repository;

import com.minimarket.api.entity.Purchase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {

    @EntityGraph(attributePaths = {"supplier", "user", "details", "details.product"})
    List<Purchase> findAllByOrderByPurchaseDateDesc();

    @EntityGraph(attributePaths = {"supplier", "user", "details", "details.product"})
    Optional<Purchase> findById(Integer id);
}
