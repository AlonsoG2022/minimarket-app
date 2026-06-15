package com.minimarket.api.service;

import com.minimarket.api.dto.CreateSaleDto;
import com.minimarket.api.dto.SaleDto;
import com.minimarket.api.entity.Sale;
import com.minimarket.api.entity.SaleDetail;
import com.minimarket.api.entity.CashMovement;
import com.minimarket.api.repository.CashSessionRepository;
import com.minimarket.api.repository.ProductRepository;
import com.minimarket.api.repository.SaleRepository;
import com.minimarket.api.repository.UserRepository;
import com.minimarket.api.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SaleService {

    private static final Logger logger = LoggerFactory.getLogger(SaleService.class);
    private static final BigDecimal IGV_DIVISOR = new BigDecimal("1.18");

    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CashSessionRepository cashSessionRepository;
    private final PrintJobService printJobService;

    public SaleService(SaleRepository saleRepository, UserRepository userRepository, ProductRepository productRepository, CashSessionRepository cashSessionRepository, PrintJobService printJobService) {
        this.saleRepository = saleRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.printJobService = printJobService;
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
        sale.setCashSessionId(dto.cashSessionId());
        sale.setPaymentMethod(dto.paymentMethod().trim());
        sale.setNotes(dto.notes() != null ? dto.notes().trim() : null);
        sale.setSubTotal(BigDecimal.ZERO);
        sale.setIgv(BigDecimal.ZERO);
        sale.setTotal(BigDecimal.ZERO);

        var cashSession = cashSessionRepository.findFirstByUserIdAndStatusOrderByOpenedAtDesc(dto.userId(), "abierta")
            .orElse(null);

        if (cashSession == null) {
            return ServiceResult.failure("Debes abrir caja antes de registrar ventas.");
        }

        if (dto.cashSessionId() != null && !cashSession.getId().equals(dto.cashSessionId())) {
            return ServiceResult.failure("La caja activa no coincide con la sesion enviada.");
        }

        sale.setCashSessionId(cashSession.getId());

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
            // Se asocia la entidad del producto (ademas del id) para que el nombre quede
            // disponible al generar el snapshot del ticket en la misma transaccion.
            detail.setProduct(product);
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
        sale.setSubTotal(calculateSubTotalFromGross(sale.getTotal()));
        sale.setIgv(calculateIgvFromGross(sale.getTotal(), sale.getSubTotal()));

        if ("Efectivo".equalsIgnoreCase(sale.getPaymentMethod())) {
            var movement = new CashMovement();
            movement.setCashSessionId(cashSession.getId());
            movement.setCashSession(cashSession);
            movement.setMovementDate(LocalDateTime.now());
            movement.setType("venta_efectivo");
            movement.setAmount(sale.getTotal());
            movement.setDescription("Venta #" + (sale.getId() != null ? sale.getId() : ""));
            movement.setReferenceType("venta");
            cashSession.getMovements().add(movement);
        }

        var saved = saleRepository.save(sale);
        if ("Efectivo".equalsIgnoreCase(saved.getPaymentMethod())) {
            var pendingMovement = cashSession.getMovements()
                .stream()
                .filter(movement -> "venta".equals(movement.getReferenceType()) && movement.getReferenceId() == null)
                .reduce((first, second) -> second)
                .orElse(null);

            if (pendingMovement != null) {
                pendingMovement.setReferenceId(saved.getId());
                pendingMovement.setDescription("Venta #" + saved.getId());
                cashSessionRepository.save(cashSession);
            }
        }
        var created = saleRepository.findWithRelationsById(saved.getId()).orElse(saved);
        try {
            printJobService.enqueueSaleTicket(created.getId());
        } catch (Exception ex) {
            logger.error("La venta {} se guardo, pero no se pudo encolar el ticket.", created.getId(), ex);
        }
        created = saleRepository.findWithRelationsById(saved.getId()).orElse(saved);
        return ServiceResult.success(DtoMapper.toDto(created));
    }

    private BigDecimal calculateSubTotalFromGross(BigDecimal total) {
        return total.divide(IGV_DIVISOR, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateIgvFromGross(BigDecimal total, BigDecimal subTotal) {
        return total.subtract(subTotal).setScale(2, RoundingMode.HALF_UP);
    }
}
