package com.minimarket.api.dto;

public record CompanyDto(
    Integer id,
    String businessName,
    String legalName,
    String taxId,
    String addressLine,
    String phone,
    String tagline,
    String documentTitle,
    String customerLabel,
    String footerLine1,
    String footerLine2,
    Boolean showTicketPreview,
    Integer minimumStock,
    String theme
) {}
