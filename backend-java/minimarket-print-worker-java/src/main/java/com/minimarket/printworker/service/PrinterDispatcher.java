package com.minimarket.printworker.service;

import com.minimarket.printworker.dto.TicketPrintPayloadDto;

public interface PrinterDispatcher {
    String printTicket(String printerName, TicketPrintPayloadDto payload) throws Exception;
}
