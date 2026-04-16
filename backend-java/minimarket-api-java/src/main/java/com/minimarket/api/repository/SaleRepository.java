package com.minimarket.api.repository;

import com.minimarket.api.entity.Sale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Integer> {

    @EntityGraph(attributePaths = {"user", "details", "details.product"})
    List<Sale> findAllByOrderBySaleDateDesc();

    @Query("""
        select distinct s
        from Sale s
        left join fetch s.user
        left join fetch s.details d
        left join fetch d.product
        where s.id = :id
        """)
    Optional<Sale> findWithRelationsById(@Param("id") Integer id);

    List<Sale> findBySaleDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    long countBySaleDateBetween(LocalDateTime dayStart, LocalDateTime dayEnd);

    @Query("select coalesce(sum(s.total), 0) from Sale s where s.saleDate between :start and :end")
    BigDecimal getTotalBySaleDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT
            p.Id AS productId,
            p.Nombre AS productName,
            p.Sku AS sku,
            SUM(dv.Cantidad) AS totalQuantity,
            SUM(dv.Subtotal) AS totalAmount
        FROM DetalleVenta dv
        INNER JOIN Ventas v ON v.Id = dv.VentaId
        INNER JOIN Productos p ON p.Id = dv.ProductoId
        WHERE v.FechaVenta BETWEEN :startDate AND :endDate
        GROUP BY p.Id, p.Nombre, p.Sku
        ORDER BY SUM(dv.Cantidad) DESC, SUM(dv.Subtotal) DESC, p.Nombre ASC
        """, nativeQuery = true)
    List<TopSellingProductProjection> findTopSellingProducts(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
