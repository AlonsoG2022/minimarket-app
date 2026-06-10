package com.minimarket.api.service;

import com.minimarket.api.dto.CompanyDto;
import com.minimarket.api.dto.SaveCompanyDto;
import com.minimarket.api.repository.CompanyRepository;
import com.minimarket.api.repository.ProductRepository;
import com.minimarket.api.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;

    public CompanyService(CompanyRepository companyRepository, ProductRepository productRepository) {
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
    }

    public CompanyDto get() {
        return companyRepository.findById(1).map(DtoMapper::toDto).orElse(null);
    }

    @Transactional
    public ServiceResult<CompanyDto> update(SaveCompanyDto dto) {
        if (dto.businessName() == null || dto.businessName().isBlank()) {
            return ServiceResult.failure("El nombre comercial es obligatorio.");
        }

        if (dto.taxId() == null || dto.taxId().isBlank()) {
            return ServiceResult.failure("El RUC es obligatorio.");
        }

        if (dto.minimumStock() == null || dto.minimumStock() < 0) {
            return ServiceResult.failure("El stock minimo no puede ser negativo.");
        }

        var company = companyRepository.findById(1).orElse(null);
        if (company == null) {
            return ServiceResult.failure("La configuracion de empresa no existe.");
        }

        company.setBusinessName(dto.businessName().trim());
        company.setLegalName(dto.legalName() != null ? dto.legalName().trim() : "");
        company.setTaxId(dto.taxId().trim());
        company.setAddressLine(dto.addressLine() != null ? dto.addressLine().trim() : "");
        company.setPhone(dto.phone() != null ? dto.phone().trim() : "");
        company.setTagline(dto.tagline() != null ? dto.tagline().trim() : "");
        company.setDocumentTitle(dto.documentTitle() != null ? dto.documentTitle().trim() : "");
        company.setCustomerLabel(dto.customerLabel() != null ? dto.customerLabel().trim() : "");
        company.setFooterLine1(dto.footerLine1() != null ? dto.footerLine1().trim() : "");
        company.setFooterLine2(dto.footerLine2() != null ? dto.footerLine2().trim() : "");
        company.setShowTicketPreview(dto.showTicketPreview() != null ? dto.showTicketPreview() : Boolean.TRUE);
        company.setMinimumStock(dto.minimumStock());

        var saved = companyRepository.save(company);

        // El stock minimo es global: sincroniza todos los productos con el nuevo valor.
        productRepository.updateAllMinimumStock(dto.minimumStock());

        return ServiceResult.success(DtoMapper.toDto(saved));
    }
}
