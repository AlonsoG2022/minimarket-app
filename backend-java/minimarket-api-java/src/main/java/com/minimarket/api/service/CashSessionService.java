package com.minimarket.api.service;

import com.minimarket.api.dto.*;
import com.minimarket.api.entity.CashMovement;
import com.minimarket.api.entity.CashSession;
import com.minimarket.api.repository.CashSessionRepository;
import com.minimarket.api.repository.UserRepository;
import com.minimarket.api.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CashSessionService {

    private final CashSessionRepository cashSessionRepository;
    private final UserRepository userRepository;

    public CashSessionService(CashSessionRepository cashSessionRepository, UserRepository userRepository) {
        this.cashSessionRepository = cashSessionRepository;
        this.userRepository = userRepository;
    }

    public CashSessionDto getCurrent(Integer userId) {
        return cashSessionRepository.findFirstByUserIdAndStatusOrderByOpenedAtDesc(userId, "abierta")
            .map(DtoMapper::toDto)
            .orElse(null);
    }

    public List<CashSessionDto> getRecent(Integer userId) {
        return cashSessionRepository.findTop10ByUserIdOrderByOpenedAtDesc(userId)
            .stream()
            .map(DtoMapper::toDto)
            .toList();
    }

    @Transactional
    public ServiceResult<CashSessionDto> open(OpenCashSessionDto dto) {
        var user = userRepository.findById(dto.userId()).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            return ServiceResult.failure("El usuario no existe o esta inactivo.");
        }

        if (dto.openingAmount() == null || dto.openingAmount().compareTo(BigDecimal.ZERO) < 0) {
            return ServiceResult.failure("El monto inicial no puede ser negativo.");
        }

        var current = cashSessionRepository.findFirstByUserIdAndStatusOrderByOpenedAtDesc(dto.userId(), "abierta");
        if (current.isPresent()) {
            return ServiceResult.failure("Ya existe una caja abierta para este usuario.");
        }

        var session = new CashSession();
        session.setUserId(dto.userId());
        session.setOpenedAt(LocalDateTime.now());
        session.setOpeningAmount(dto.openingAmount());
        session.setNotes(dto.notes() != null ? dto.notes().trim() : null);
        session.setStatus("abierta");

        var saved = cashSessionRepository.save(session);
        return ServiceResult.success(DtoMapper.toDto(cashSessionRepository.findById(saved.getId()).orElse(saved)));
    }

    @Transactional
    public ServiceResult<CashSessionDto> addMovement(Integer sessionId, CreateCashMovementDto dto) {
        var session = cashSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return ServiceResult.failure("La caja indicada no existe.");
        }

        if (!session.getUserId().equals(dto.userId())) {
            return ServiceResult.failure("La caja no pertenece al usuario actual.");
        }

        if (!"abierta".equalsIgnoreCase(session.getStatus())) {
            return ServiceResult.failure("Solo puedes registrar movimientos en una caja abierta.");
        }

        var type = dto.type() != null ? dto.type().trim().toLowerCase() : "";
        if (!List.of("ingreso", "retiro", "gasto").contains(type)) {
            return ServiceResult.failure("Tipo de movimiento no valido.");
        }

        if (dto.amount() == null || dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ServiceResult.failure("El monto debe ser mayor que cero.");
        }

        var movement = new CashMovement();
        movement.setCashSessionId(session.getId());
        movement.setCashSession(session);
        movement.setMovementDate(LocalDateTime.now());
        movement.setType(type);
        movement.setAmount(dto.amount());
        movement.setDescription(dto.description() != null ? dto.description().trim() : null);

        session.getMovements().add(movement);
        var saved = cashSessionRepository.save(session);
        return ServiceResult.success(DtoMapper.toDto(cashSessionRepository.findById(saved.getId()).orElse(saved)));
    }

    @Transactional
    public ServiceResult<CashSessionDto> close(Integer sessionId, CloseCashSessionDto dto) {
        var session = cashSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return ServiceResult.failure("La caja indicada no existe.");
        }

        if (!session.getUserId().equals(dto.userId())) {
            return ServiceResult.failure("La caja no pertenece al usuario actual.");
        }

        if (!"abierta".equalsIgnoreCase(session.getStatus())) {
            return ServiceResult.failure("La caja ya fue cerrada.");
        }

        var expectedAmount = DtoMapper.toDto(session).currentAmount();
        var countedAmount = dto.countedAmount() != null ? dto.countedAmount() : BigDecimal.ZERO;

        session.setClosedAt(LocalDateTime.now());
        session.setClosingExpectedAmount(expectedAmount);
        session.setClosingCountedAmount(countedAmount);
        session.setDifference(countedAmount.subtract(expectedAmount));
        session.setStatus("cerrada");
        if (dto.notes() != null && !dto.notes().isBlank()) {
            session.setNotes(dto.notes().trim());
        }

        var saved = cashSessionRepository.save(session);
        return ServiceResult.success(DtoMapper.toDto(cashSessionRepository.findById(saved.getId()).orElse(saved)));
    }
}
