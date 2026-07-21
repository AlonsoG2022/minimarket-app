package com.minimarket.printworker.service;

import com.minimarket.printworker.config.PrintingProperties;
import com.minimarket.printworker.dto.TicketPrintPayloadDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class TicketTextRenderer {
    private static final String DATE_PATTERN = "dd/MM/yyyy HH:mm";

    public String build(TicketPrintPayloadDto payload, PrintingProperties properties) {
        var width = Math.max(32, properties.getLineWidth());

        // Los datos vienen del snapshot (igual que la vista previa). Si un ticket viejo no los trae,
        // se usan como respaldo las propiedades locales del worker.
        var businessName = fallback(payload.businessName(), properties.getBusinessName());
        var legalName = fallback(payload.legalName(), properties.getLegalName());
        var taxId = fallback(payload.taxId(), properties.getTaxId());
        var addressLine = fallback(payload.addressLine(), properties.getAddressLine());
        var phone = fallback(payload.phone(), properties.getPhone());
        var tagline = payload.tagline() != null ? payload.tagline().trim() : "";
        var documentTitle = fallback(payload.documentTitle(), "Ticket de venta");
        var customerLabel = fallback(payload.customerLabel(), properties.getCustomerLabel());
        var footerLine1 = fallback(payload.footerLine1(), properties.getFooterLine1());
        var footerLine2 = fallback(payload.footerLine2(), properties.getFooterLine2());

        // Totales del snapshot; si un ticket viejo no trae el desglose, se deriva del total (IGV 18% incluido).
        var total = payload.total();
        var subTotal = payload.subTotal() != null && payload.subTotal().signum() > 0
            ? payload.subTotal()
            : total.divide(BigDecimal.valueOf(1.18), 2, java.math.RoundingMode.HALF_UP);
        var igv = payload.igv() != null && payload.igv().signum() > 0
            ? payload.igv()
            : total.subtract(subTotal);

        var lines = new ArrayList<String>();
        lines.add(center(businessName, width));
        if (!tagline.isBlank()) {
            lines.add(center(tagline, width));
        }
        lines.add("-".repeat(width));
        lines.add(center(legalName, width));
        lines.add(center("RUC: " + taxId, width));
        lines.add(center(addressLine, width));
        lines.add(center("Telefono: " + phone, width));
        lines.add("-".repeat(width));
        lines.add(center(documentTitle.toUpperCase(), width));
        lines.add(center("#" + payload.saleId() + " - " + payload.saleDate().format(java.time.format.DateTimeFormatter.ofPattern(DATE_PATTERN)), width));
        lines.add("-".repeat(width));
        lines.add(metaRow("Fecha", payload.saleDate().format(java.time.format.DateTimeFormatter.ofPattern(DATE_PATTERN)), width));
        lines.add(metaRow("Cajero", payload.cashierName(), width));
        lines.add(metaRow("Cliente", customerLabel, width));
        lines.add(metaRow("Pago", payload.paymentMethod(), width));
        lines.add("-".repeat(width));
        lines.add(ticketHeader(width));

        for (var item : payload.items()) {
            lines.addAll(ticketItemLines(item, width));
            lines.add("-".repeat(width));
        }

        lines.add(row("Items", String.valueOf(payload.items().size()), width));
        var totalUnits = payload.items().stream().mapToInt(item -> item.quantity() == null ? 0 : item.quantity()).sum();
        lines.add(row("Unidades", String.valueOf(totalUnits), width));
        lines.add(row("Subtotal", formatMoney(subTotal), width));
        lines.add(row("IGV (18%)", formatMoney(igv), width));

        if (payload.notes() != null && !payload.notes().isBlank()) {
            lines.add("-".repeat(width));
            lines.add("Notas:");
            lines.addAll(wrap(payload.notes(), width));
        }

        lines.add("-".repeat(width));
        lines.add(row("Total", formatMoney(total), width));
        lines.add("");
        lines.add(center(footerLine1, width));
        lines.add(center(footerLine2, width));
        lines.add("");
        lines.add("");

        return String.join(System.lineSeparator(), lines);
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String row(String label, String value, int width) {
        return padRight(label, Math.max(0, width - value.length())) + value;
    }

    private static String metaRow(String label, String value, int width) {
        return row(label, value, width);
    }

    private static String formatMoney(BigDecimal value) {
        return "S/ " + value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static String center(String value, int width) {
        if (value.length() >= width) {
            return value;
        }

        var padding = (width - value.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + value;
    }

    private static String padRight(String value, int width) {
        return String.format("%1$-" + Math.max(value.length(), width) + "s", value);
    }

    private static List<String> wrap(String value, int width) {
        var lines = new ArrayList<String>();
        if (value == null || value.isBlank()) {
            return lines;
        }

        var words = value.split("\\s+");
        var current = new StringBuilder();
        for (var word : words) {
            var candidate = current.length() == 0 ? word : current + " " + word;
            if (candidate.length() <= width) {
                current = new StringBuilder(candidate);
                continue;
            }

            if (current.length() > 0) {
                lines.add(current.toString());
            }

            current = new StringBuilder(word);
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        return lines;
    }

    private static String ticketHeader(int width) {
        var layout = columnLayout(width);
        return padRight("CANT.", layout.qtyWidth())
            + " "
            + padRight("PRODUCTO", layout.productWidth())
            + " "
            + padLeft("P. UNIT", layout.unitWidth())
            + " "
            + padLeft("IMPORTE", layout.amountWidth());
    }

    private static List<String> ticketItemLines(com.minimarket.printworker.dto.TicketPrintItemDto item, int width) {
        var layout = columnLayout(width);
        var productLines = wrap(item.productName(), layout.productWidth());
        if (productLines.isEmpty()) {
            productLines = List.of("");
        }

        var lines = new ArrayList<String>();
        var firstLine = padRight(String.valueOf(item.quantity()), layout.qtyWidth())
            + " "
            + padRight(productLines.getFirst(), layout.productWidth())
            + " "
            + padLeft(formatMoney(item.unitPrice()), layout.unitWidth())
            + " "
            + padLeft(formatMoney(item.subtotal()), layout.amountWidth());
        lines.add(firstLine);

        for (var index = 1; index < productLines.size(); index++) {
            lines.add(padRight("", layout.qtyWidth())
                + " "
                + padRight(productLines.get(index), layout.productWidth()));
        }

        return lines;
    }

    private static ColumnLayout columnLayout(int width) {
        var qtyWidth = 5;
        var unitWidth = 10;
        var amountWidth = 10;
        var spaces = 3;
        var productWidth = Math.max(12, width - qtyWidth - unitWidth - amountWidth - spaces);
        return new ColumnLayout(qtyWidth, productWidth, unitWidth, amountWidth);
    }

    private static String padLeft(String value, int width) {
        return String.format("%" + Math.max(value.length(), width) + "s", value);
    }

    private record ColumnLayout(int qtyWidth, int productWidth, int unitWidth, int amountWidth) {
    }
}
