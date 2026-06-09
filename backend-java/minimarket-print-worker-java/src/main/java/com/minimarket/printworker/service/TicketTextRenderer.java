package com.minimarket.printworker.service;

import com.minimarket.printworker.config.PrintingProperties;
import com.minimarket.printworker.dto.TicketPrintPayloadDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class TicketTextRenderer {

    public String build(TicketPrintPayloadDto payload, PrintingProperties properties) {
        var width = Math.max(32, properties.getLineWidth());
        var lines = new ArrayList<String>();
        lines.add(center(properties.getBusinessName(), width));
        lines.add(center("Abarrotes - Bebidas - Limpieza", width));
        lines.add("-".repeat(width));
        lines.add(center(properties.getLegalName(), width));
        lines.add(center("RUC: " + properties.getTaxId(), width));
        lines.add(center(properties.getAddressLine(), width));
        lines.add(center("Telefono: " + properties.getPhone(), width));
        lines.add("-".repeat(width));
        lines.add(center("TICKET DE VENTA", width));
        lines.add(center("#" + payload.saleId(), width));
        lines.add("-".repeat(width));
        lines.add("Fecha: " + payload.saleDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        lines.add("Cajero: " + payload.cashierName());
        lines.add("Cliente: " + properties.getCustomerLabel());
        lines.add("Pago: " + payload.paymentMethod());
        lines.add("-".repeat(width));
        lines.add(ticketHeader(width));
        lines.add("-".repeat(width));

        for (var item : payload.items()) {
            lines.addAll(wrap(item.productName(), width - 2));
            var subtotal = formatMoney(item.subtotal());
            var detail = item.quantity() + " x " + formatMoney(item.unitPrice());
            lines.add(padRight(detail, Math.max(0, width - subtotal.length())) + subtotal);
        }

        lines.add("-".repeat(width));
        lines.add(padRight("Items", Math.max(0, width - String.valueOf(payload.items().size()).length())) + payload.items().size());
        var totalUnits = payload.items().stream().mapToInt(item -> item.quantity() == null ? 0 : item.quantity()).sum();
        lines.add(padRight("Unidades", Math.max(0, width - String.valueOf(totalUnits).length())) + totalUnits);
        lines.add(padRight("Subtotal", Math.max(0, width - formatMoney(payload.total()).length())) + formatMoney(payload.total()));

        if (payload.notes() != null && !payload.notes().isBlank()) {
            lines.add("-".repeat(width));
            lines.add("Notas:");
            lines.addAll(wrap(payload.notes(), width));
        }

        lines.add("-".repeat(width));
        lines.add(padRight("TOTAL", Math.max(0, width - formatMoney(payload.total()).length())) + formatMoney(payload.total()));
        lines.add("");
        lines.add(center(properties.getFooterLine1(), width));
        lines.add(center(properties.getFooterLine2(), width));
        lines.add("");
        lines.add("");

        return String.join(System.lineSeparator(), lines);
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
        var header = "CANT DESCRIPCION P.UNIT IMPORTE";
        return header.length() <= width ? header : header.substring(0, width);
    }
}
