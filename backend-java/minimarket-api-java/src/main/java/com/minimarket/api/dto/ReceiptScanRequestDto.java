package com.minimarket.api.dto;

// Imagen de la boleta (base64) que sube el usuario para leerla con IA.
public record ReceiptScanRequestDto(String imageBase64, String mediaType) {
}
