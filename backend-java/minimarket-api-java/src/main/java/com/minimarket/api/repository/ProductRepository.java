package com.minimarket.api.repository;

import com.minimarket.api.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    @EntityGraph(attributePaths = "category")
    List<Product> findAllByOrderByNameAsc();

    @Query("select p from Product p left join fetch p.category where p.id = :id")
    Optional<Product> findWithCategoryById(@Param("id") Integer id);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Integer id);
}
