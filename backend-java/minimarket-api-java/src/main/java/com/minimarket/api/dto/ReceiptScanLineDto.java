package com.minimarket.api.dto;

import java.math.BigDecimal;

// Una linea propuesta tras leer la boleta (con match sugerido contra el catalogo).
public record ReceiptScanLineDto(
    String description,
    BigDecimal quantity,
    Integer packUnits,
    BigDecimal unitCost,
    BigDecimal lineTotal,
    String suggestedName,
    String suggestedShortName,
    String suggestedCategory,
    BigDecimal suggestedPrice,
    Integer matchProductId,
    String matchProductName,
    Double matchScore) {
}
