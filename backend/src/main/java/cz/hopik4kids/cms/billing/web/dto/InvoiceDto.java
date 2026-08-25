package cz.hopik4kids.cms.billing.web.dto;

import cz.hopik4kids.cms.billing.domain.Invoice;

import java.time.Instant;
import java.time.LocalDate;

public record InvoiceDto(
        String id,
        String invoiceNumber,
        String registrationId,
        String type,
        String payerName,
        String payerAddress,
        String payerEmail,
        String items,
        int totalAmount,
        LocalDate issueDate,
        LocalDate dueDate,
        String variableSymbol,
        String status,
        Instant paidAt
) {
    public static InvoiceDto from(Invoice i) {
        return new InvoiceDto(
                i.getId(),
                i.getInvoiceNumber(),
                i.getRegistrationId(),
                i.getType(),
                i.getPayerName(),
                i.getPayerAddress(),
                i.getPayerEmail(),
                i.getItems(),
                i.getTotalAmount(),
                i.getIssueDate(),
                i.getDueDate(),
                i.getVariableSymbol(),
                i.getStatus().name().toLowerCase(),
                i.getPaidAt());
    }
}
