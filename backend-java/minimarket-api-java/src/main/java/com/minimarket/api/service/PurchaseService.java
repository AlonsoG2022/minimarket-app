package com.minimarket.api.service;

import com.minimarket.api.dto.CreatePurchaseDto;
import com.minimarket.api.dto.PurchaseDto;
import com.minimarket.api.entity.Purchase;
import com.minimarket.api.entity.PurchaseDetail;
import com.minimarket.api.repository.ProductRepository;
import com.minimarket.api.repository.PurchaseRepository;
import com.minimarket.api.repository.SupplierRepository;
import com.minimarket.api.repository.UserRepository;
import com.minimarket.api.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PurchaseService {
    private static final BigDecimal IGV_DIVISOR = new BigDecimal("1.18");

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public PurchaseService(
        PurchaseRepository purchaseRepository,
        SupplierRepository supplierRepository,
        UserRepository userRepository,
        ProductRepository productRepository
    ) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public List<PurchaseDto> getAll() {
        return purchaseRepository.findAllByOrderByPurchaseDateDesc()
            .stream()
            .map(DtoMapper::toDto)
            .toList();
    }

    public PurchaseDto getById(Integer id) {
        return purchaseRepository.findById(id)
            .map(DtoMapper::toDto)
            .orElse(null);
    }

    @Transactional
    public ServiceResult<PurchaseDto> create(CreatePurchaseDto dto) {
        if (dto.details() == null || dto.details().isEmpty()) {
            return ServiceResult.failure("La compra debe tener al menos un item.");
        }

        var supplier = supplierRepository.findById(dto.supplierId()).orElse(null);
        if (supplier == null || !Boolean.TRUE.equals(supplier.getIsActive())) {
            return ServiceResult.failure("El proveedor seleccionado no existe o esta inactivo.");
        }

        var user = userRepository.findById(dto.userId()).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            return ServiceResult.failure("El usuario seleccionado no existe o esta inactivo.");
        }

        var purchase = new Purchase();
        purchase.setPurchaseDate(LocalDateTime.now());
        purchase.setSupplierId(dto.supplierId());
        purchase.setUserId(dto.userId());
        purchase.setInvoiceNumber(normalizeOptional(dto.invoiceNumber()));
        purchase.setNotes(normalizeOptional(dto.notes()));
        purchase.setSubTotal(BigDecimal.ZERO);
        purchase.setIgv(BigDecimal.ZERO);
        purchase.setTotal(BigDecimal.ZERO);

        for (var item : dto.details()) {
            var product = productRepository.findById(item.productId()).orElse(null);
            if (product == null || !Boolean.TRUE.equals(product.getIsActive())) {
                return ServiceResult.failure("El producto con id " + item.productId() + " no existe.");
            }

            if (item.packageQuantity() == null || item.packageQuantity() <= 0
                || item.unitsPerPackage() == null || item.unitsPerPackage() <= 0
                || item.packageCost() == null || item.packageCost().signum() <= 0) {
                return ServiceResult.failure("Los valores de compra para " + product.getName() + " no son validos.");
            }

            var totalUnits = item.packageQuantity() * item.unitsPerPackage();
            var subtotal = item.packageCost().multiply(BigDecimal.valueOf(item.packageQuantity()));
            var unitCost = subtotal.divide(BigDecimal.valueOf(totalUnits), 4, RoundingMode.HALF_UP);
            var currentStock = product.getStock();
            var currentCost = product.getCost() != null ? product.getCost() : BigDecimal.ZERO;

            product.setStock(currentStock + totalUnits);
            var weightedCost = currentStock + totalUnits > 0
                ? currentCost.multiply(BigDecimal.valueOf(currentStock))
                    .add(unitCost.multiply(BigDecimal.valueOf(totalUnits)))
                    .divide(BigDecimal.valueOf(currentStock + totalUnits), 2, RoundingMode.HALF_UP)
                : unitCost.setScale(2, RoundingMode.HALF_UP);
            product.setCost(weightedCost);
            product.setPurchaseUnitName(normalizeUnitName(item.purchaseUnitName(), product.getPurchaseUnitName()));
            product.setUnitsPerPurchaseUnit(item.unitsPerPackage());

            var detail = new PurchaseDetail();
            detail.setPurchase(purchase);
            detail.setProductId(product.getId());
            detail.setPackageQuantity(item.packageQuantity());
            detail.setUnitsPerPackage(item.unitsPerPackage());
            detail.setTotalUnits(totalUnits);
            detail.setPackageCost(item.packageCost().setScale(2, RoundingMode.HALF_UP));
            detail.setUnitCost(unitCost.setScale(2, RoundingMode.HALF_UP));
            detail.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
            detail.setPurchaseUnitName(normalizeUnitName(item.purchaseUnitName(), product.getPurchaseUnitName()));
            detail.setBarcodeSnapshot(normalizeOptional(item.barcodeSnapshot()));

            purchase.getDetails().add(detail);
        }

        purchase.setTotal(
            purchase.getDetails()
                .stream()
                .map(PurchaseDetail::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
        purchase.setSubTotal(calculateSubTotalFromGross(purchase.getTotal()));
        purchase.setIgv(calculateIgvFromGross(purchase.getTotal(), purchase.getSubTotal()));

        var saved = purchaseRepository.save(purchase);
        var created = purchaseRepository.findById(saved.getId()).orElse(saved);
        return ServiceResult.success(DtoMapper.toDto(created));
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        var trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalizeUnitName(String providedValue, String fallbackValue) {
        if (providedValue == null) {
            return fallbackValue;
        }

        var trimmed = providedValue.trim();
        return trimmed.isBlank() ? fallbackValue : trimmed;
    }

    private BigDecimal calculateSubTotalFromGross(BigDecimal total) {
        return total.divide(IGV_DIVISOR, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateIgvFromGross(BigDecimal total, BigDecimal subTotal) {
        return total.subtract(subTotal).setScale(2, RoundingMode.HALF_UP);
    }
}
