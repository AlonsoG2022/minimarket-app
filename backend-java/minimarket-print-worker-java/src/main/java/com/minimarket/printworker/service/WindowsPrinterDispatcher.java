package com.minimarket.printworker.service;

import com.minimarket.printworker.config.PrintingProperties;
import com.minimarket.printworker.dto.TicketPrintPayloadDto;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import java.nio.charset.StandardCharsets;

@Service
public class WindowsPrinterDispatcher implements PrinterDispatcher {

    private final TicketTextRenderer renderer;
    private final PrintingProperties properties;

    public WindowsPrinterDispatcher(TicketTextRenderer renderer, PrintingProperties properties) {
        this.renderer = renderer;
        this.properties = properties;
    }

    @Override
    public String printTicket(String printerName, TicketPrintPayloadDto payload) throws Exception {
        var targetPrinter = resolvePrinter(printerName == null || printerName.isBlank() ? properties.getPrinterName() : printerName);
        var content = renderer.build(payload, properties);
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        Doc doc = new SimpleDoc(data, flavor, null);
        DocPrintJob printJob = targetPrinter.createPrintJob();
        printJob.print(doc, null);
        return targetPrinter.getName();
    }

    private PrintService resolvePrinter(String configuredPrinter) throws PrintException {
        if (configuredPrinter != null && !configuredPrinter.isBlank()) {
            for (var service : PrintServiceLookup.lookupPrintServices(DocFlavor.BYTE_ARRAY.AUTOSENSE, null)) {
                if (service.getName().equalsIgnoreCase(configuredPrinter)) {
                    return service;
                }
            }

            throw new PrintException("No se encontro la impresora configurada: " + configuredPrinter);
        }

        var defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();
        if (defaultPrinter == null) {
            throw new PrintException("No se encontro una impresora predeterminada para el worker local.");
        }

        return defaultPrinter;
    }
}
