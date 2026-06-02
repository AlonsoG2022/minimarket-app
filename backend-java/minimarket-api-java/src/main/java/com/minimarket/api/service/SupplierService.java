package com.minimarket.api.service;

import com.minimarket.api.dto.SaveSupplierDto;
import com.minimarket.api.dto.SupplierDto;
import com.minimarket.api.entity.Supplier;
import com.minimarket.api.repository.SupplierRepository;
import com.minimarket.api.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<SupplierDto> getAll() {
        return supplierRepository.findAllByOrderByNameAsc()
            .stream()
            .map(DtoMapper::toDto)
            .toList();
    }

    public SupplierDto getById(Integer id) {
        return supplierRepository.findById(id)
            .map(DtoMapper::toDto)
            .orElse(null);
    }

    @Transactional
    public ServiceResult<SupplierDto> create(SaveSupplierDto dto) {
        if (dto.name() == null || dto.name().trim().isBlank()) {
            return ServiceResult.failure("El nombre del proveedor es obligatorio.");
        }

        var supplier = new Supplier();
        supplier.setName(dto.name().trim());
        supplier.setDocumentNumber(normalizeOptional(dto.documentNumber()));
        supplier.setContactName(normalizeOptional(dto.contactName()));
        supplier.setPhone(normalizeOptional(dto.phone()));
        supplier.setEmail(normalizeOptional(dto.email()));
        supplier.setAddress(normalizeOptional(dto.address()));
        supplier.setNotes(normalizeOptional(dto.notes()));
        supplier.setIsActive(Boolean.TRUE.equals(dto.isActive()));

        var saved = supplierRepository.save(supplier);
        return ServiceResult.success(DtoMapper.toDto(saved));
    }

    @Transactional
    public ServiceResult<SupplierDto> update(Integer id, SaveSupplierDto dto) {
        var supplier = supplierRepository.findById(id).orElse(null);
        if (supplier == null) {
            return ServiceResult.failure("Proveedor no encontrado.");
        }

        if (dto.name() == null || dto.name().trim().isBlank()) {
            return ServiceResult.failure("El nombre del proveedor es obligatorio.");
        }

        supplier.setName(dto.name().trim());
        supplier.setDocumentNumber(normalizeOptional(dto.documentNumber()));
        supplier.setContactName(normalizeOptional(dto.contactName()));
        supplier.setPhone(normalizeOptional(dto.phone()));
        supplier.setEmail(normalizeOptional(dto.email()));
        supplier.setAddress(normalizeOptional(dto.address()));
        supplier.setNotes(normalizeOptional(dto.notes()));
        supplier.setIsActive(Boolean.TRUE.equals(dto.isActive()));

        var updated = supplierRepository.save(supplier);
        return ServiceResult.success(DtoMapper.toDto(updated));
    }

    @Transactional
    public ServiceResult<Void> delete(Integer id) {
        var supplier = supplierRepository.findById(id).orElse(null);
        if (supplier == null) {
            return ServiceResult.failure("Proveedor no encontrado.");
        }

        supplierRepository.delete(supplier);
        return ServiceResult.success(null);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        var trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
