package cz.hopik4kids.cms.billing.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A previewed bank transaction with its proposed invoice match (prd todo #5).
 * matchStatus: EXACT (VS + amount ok), PARTIAL (VS ok, amount differs), NONE (no invoice),
 * ALREADY (transaction already imported), OUTGOING (negative amount — ignored).
 */
public record BankMatchDto(
        String txId,
        LocalDate txDate,
        BigDecimal amount,
        String variableSymbol,
        String counterparty,
        String message,
        String matchStatus,
        String invoiceId,
        String invoiceNumber,
        Integer invoiceAmount,
        boolean invoiceAlreadyPaid
) {
}
