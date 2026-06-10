package com.minimarket.api.repository;

import com.minimarket.api.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    @EntityGraph(attributePaths = "category")
    List<Product> findAllByOrderByNameAsc();

    @Query("select p from Product p left join fetch p.category where p.id = :id")
    Optional<Product> findWithCategoryById(@Param("id") Integer id);

    @Query("select p from Product p left join fetch p.category where p.barcode = :barcode")
    Optional<Product> findWithCategoryByBarcode(@Param("barcode") String barcode);

    @Query("select p from Product p left join fetch p.category where p.purchaseBarcode = :barcode")
    Optional<Product> findWithCategoryByPurchaseBarcode(@Param("barcode") String barcode);

    List<Product> findBySkuStartingWith(String prefix);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Integer id);

    boolean existsByBarcodeIgnoreCase(String barcode);

    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, Integer id);

    boolean existsByPurchaseBarcodeIgnoreCase(String purchaseBarcode);

    boolean existsByPurchaseBarcodeIgnoreCaseAndIdNot(String purchaseBarcode, Integer id);

    @Query("select count(sd) > 0 from SaleDetail sd where sd.productId = :productId")
    boolean existsSaleDetailsByProductId(@Param("productId") Integer productId);

    @Query("select count(pd) > 0 from PurchaseDetail pd where pd.productId = :productId")
    boolean existsPurchaseDetailsByProductId(@Param("productId") Integer productId);

    @Modifying
    @Query("update Product p set p.minimumStock = :minimumStock where p.minimumStock <> :minimumStock")
    int updateAllMinimumStock(@Param("minimumStock") Integer minimumStock);
}
