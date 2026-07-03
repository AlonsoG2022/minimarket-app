package com.minimarket.api.dto;

import java.util.List;

public record ReceiptScanResultDto(
    String supplierName,
    String supplierRuc,
    Integer supplierId,
    List<ReceiptScanLineDto> lines,
    List<String> warnings) {
}
