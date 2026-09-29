package cz.hopik4kids.cms.billing.web.dto;

import cz.hopik4kids.cms.billing.domain.CreditNote;

import java.time.Instant;
import java.time.LocalDate;

public record CreditNoteDto(
        String id,
        String number,
        String invoiceId,
        String invoiceNumber,
        String payerName,
        String payerAddress,
        String payerEmail,
        String items,
        int totalAmount,
        LocalDate issueDate,
        String variableSymbol,
        String reason,
        Instant createdAt
) {
    public static CreditNoteDto from(CreditNote c) {
        return new CreditNoteDto(
                c.getId(),
                c.getNumber(),
                c.getInvoiceId(),
                c.getInvoiceNumber(),
                c.getPayerName(),
                c.getPayerAddress(),
                c.getPayerEmail(),
                c.getItems(),
                c.getTotalAmount(),
                c.getIssueDate(),
                c.getVariableSymbol(),
                c.getReason(),
                c.getCreatedAt());
    }
}
