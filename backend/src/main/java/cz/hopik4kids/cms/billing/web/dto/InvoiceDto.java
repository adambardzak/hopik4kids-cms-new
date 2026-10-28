package cz.hopik4kids.cms.billing.web.dto;

import cz.hopik4kids.cms.billing.domain.Invoice;

import java.time.Instant;
import java.time.LocalDate;

public record InvoiceDto(
        String id,
        String invoiceNumber,
        String registrationId,
        String schoolContractId,
        String payerKind,
        String type,
        String payerName,
        String payerAddress,
        String payerEmail,
        String payerIco,
        LocalDate periodFrom,
        LocalDate periodTo,
        String items,
        int totalAmount,
        int programAmount,
        int shirtAmount,
        LocalDate issueDate,
        LocalDate dueDate,
        String variableSymbol,
        String status,
        Instant paidAt,
        Integer paidAmount,
        Instant sentAt,
        int sentCount
) {
    public static InvoiceDto from(Invoice i) {
        return from(i, 0, 0);
    }

    /** Variant that carries the program/shirt split (computed from items by the service). */
    public static InvoiceDto from(Invoice i, int programAmount, int shirtAmount) {
        return new InvoiceDto(
                i.getId(),
                i.getInvoiceNumber(),
                i.getRegistrationId(),
                i.getSchoolContractId(),
                i.getPayerKind().name().toLowerCase(),
                i.getType(),
                i.getPayerName(),
                i.getPayerAddress(),
                i.getPayerEmail(),
                i.getPayerIco(),
                i.getPeriodFrom(),
                i.getPeriodTo(),
                i.getItems(),
                i.getTotalAmount(),
                programAmount,
                shirtAmount,
                i.getIssueDate(),
                i.getDueDate(),
                i.getVariableSymbol(),
                i.getStatus().name().toLowerCase(),
                i.getPaidAt(),
                i.getPaidAmount(),
                i.getSentAt(),
                i.getSentCount());
    }
}
