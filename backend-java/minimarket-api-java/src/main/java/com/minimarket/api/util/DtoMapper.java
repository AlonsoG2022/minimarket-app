package com.minimarket.api.util;

import com.minimarket.api.dto.*;
import com.minimarket.api.entity.Category;
import com.minimarket.api.entity.Product;
import com.minimarket.api.entity.Purchase;
import com.minimarket.api.entity.Sale;
import com.minimarket.api.entity.Supplier;
import com.minimarket.api.entity.User;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static CategoryDto toDto(Category category) {
        return new CategoryDto(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getIsActive()
        );
    }

    public static ProductDto toDto(Product product) {
        return new ProductDto(
            product.getId(),
            product.getName(),
            product.getSku(),
            product.getBarcode(),
            product.getPurchaseBarcode(),
            product.getDescription(),
            product.getPrice(),
            product.getCost(),
            product.getStock(),
            product.getMinimumStock(),
            product.getSalesUnitName(),
            product.getPurchaseUnitName(),
            product.getUnitsPerPurchaseUnit(),
            product.getIsActive(),
            product.getCategoryId(),
            product.getCategory() != null ? product.getCategory().getName() : ""
        );
    }

    public static SupplierDto toDto(Supplier supplier) {
        return new SupplierDto(
            supplier.getId(),
            supplier.getName(),
            supplier.getDocumentNumber(),
            supplier.getContactName(),
            supplier.getPhone(),
            supplier.getEmail(),
            supplier.getAddress(),
            supplier.getNotes(),
            supplier.getIsActive()
        );
    }

    public static UserDto toDto(User user) {
        return new UserDto(
            user.getId(),
            user.getFullName(),
            user.getUsername(),
            user.getRole(),
            user.getIsActive()
        );
    }

    public static SaleDto toDto(Sale sale) {
        var details = sale.getDetails()
            .stream()
            .map(detail -> new SaleDetailDto(
                detail.getId(),
                detail.getProductId(),
                detail.getProduct() != null ? detail.getProduct().getName() : "",
                detail.getQuantity(),
                detail.getUnitPrice(),
                detail.getSubtotal()
            ))
            .toList();

        return new SaleDto(
            sale.getId(),
            sale.getSaleDate(),
            sale.getUserId(),
            sale.getUser() != null ? sale.getUser().getFullName() : "",
            sale.getPaymentMethod(),
            sale.getTotal(),
            sale.getNotes(),
            details
        );
    }

    public static PurchaseDto toDto(Purchase purchase) {
        var details = purchase.getDetails()
            .stream()
            .map(detail -> new PurchaseDetailDto(
                detail.getId(),
                detail.getProductId(),
                detail.getProduct() != null ? detail.getProduct().getName() : "",
                detail.getPackageQuantity(),
                detail.getUnitsPerPackage(),
                detail.getTotalUnits(),
                detail.getPackageCost(),
                detail.getUnitCost(),
                detail.getSubtotal(),
                detail.getPurchaseUnitName(),
                detail.getBarcodeSnapshot()
            ))
            .toList();

        return new PurchaseDto(
            purchase.getId(),
            purchase.getPurchaseDate(),
            purchase.getSupplierId(),
            purchase.getSupplier() != null ? purchase.getSupplier().getName() : "",
            purchase.getUserId(),
            purchase.getUser() != null ? purchase.getUser().getFullName() : "",
            purchase.getInvoiceNumber(),
            purchase.getNotes(),
            purchase.getTotal(),
            details
        );
    }
}
