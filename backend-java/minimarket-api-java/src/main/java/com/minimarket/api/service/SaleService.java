package com.minimarket.api.service;

import com.minimarket.api.dto.CreateSaleDto;
import com.minimarket.api.dto.SaleDto;
import com.minimarket.api.entity.Sale;
import com.minimarket.api.entity.SaleDetail;
import com.minimarket.api.repository.ProductRepository;
import com.minimarket.api.repository.SaleRepository;
import com.minimarket.api.repository.UserRepository;
import com.minimarket.api.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public SaleService(SaleRepository saleRepository, UserRepository userRepository, ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public List<SaleDto> getAll() {
        return saleRepository.findAllByOrderBySaleDateDesc()
            .stream()
            .map(DtoMapper::toDto)
            .toList();
    }

    public SaleDto getById(Integer id) {
        return saleRepository.findWithRelationsById(id)
            .map(DtoMapper::toDto)
            .orElse(null);
    }

    @Transactional
    public ServiceResult<SaleDto> create(CreateSaleDto dto) {
        if (dto.details() == null || dto.details().isEmpty()) {
            return ServiceResult.failure("La venta debe tener al menos un item.");
        }

        var user = userRepository.findById(dto.userId()).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            return ServiceResult.failure("El usuario seleccionado no existe o esta inactivo.");
        }

        var sale = new Sale();
        sale.setSaleDate(LocalDateTime.now());
        sale.setUserId(dto.userId());
        sale.setPaymentMethod(dto.paymentMethod().trim());
        sale.setNotes(dto.notes() != null ? dto.notes().trim() : null);
        sale.setTotal(BigDecimal.ZERO);

        for (var item : dto.details()) {
            var product = productRepository.findById(item.productId()).orElse(null);
            if (product == null || !Boolean.TRUE.equals(product.getIsActive())) {
                return ServiceResult.failure("El producto con id " + item.productId() + " no existe.");
            }

            if (item.quantity() <= 0) {
                return ServiceResult.failure("La cantidad del producto " + product.getName() + " debe ser mayor que cero.");
            }

            if (product.getStock() < item.quantity()) {
                return ServiceResult.failure("Stock insuficiente para " + product.getName() + ". Disponible: " + product.getStock() + ".");
            }

            product.setStock(product.getStock() - item.quantity());

            var detail = new SaleDetail();
            detail.setSale(sale);
            detail.setProductId(product.getId());
            detail.setQuantity(item.quantity());
            detail.setUnitPrice(product.getPrice());
            detail.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())));

            sale.getDetails().add(detail);
        }

        sale.setTotal(
            sale.getDetails()
                .stream()
                .map(SaleDetail::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        var saved = saleRepository.save(sale);
        var created = saleRepository.findWithRelationsById(saved.getId()).orElse(saved);
        return ServiceResult.success(DtoMapper.toDto(created));
    }
}
