package cz.hopik4kids.cms.billing.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An imported bank transaction (prd todo #5). Persisted for idempotency (unique tx_id) so
 * re-uploading a statement never double-pays. Incoming payments get matched to invoices by
 * variable symbol + amount.
 */
@Entity
@Table(name = "bank_transaction")
public class BankTransaction extends BaseEntity {

    @Column(name = "tx_id", nullable = false, unique = true, length = 100)
    private String txId;

    @Column(name = "tx_date")
    private LocalDate txDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "variable_symbol", length = 30)
    private String variableSymbol;

    @Column(length = 255)
    private String counterparty;

    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "matched_invoice_id")
    private String matchedInvoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BankTransactionStatus status = BankTransactionStatus.UNMATCHED;

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public LocalDate getTxDate() {
        return txDate;
    }

    public void setTxDate(LocalDate txDate) {
        this.txDate = txDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getVariableSymbol() {
        return variableSymbol;
    }

    public void setVariableSymbol(String variableSymbol) {
        this.variableSymbol = variableSymbol;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMatchedInvoiceId() {
        return matchedInvoiceId;
    }

    public void setMatchedInvoiceId(String matchedInvoiceId) {
        this.matchedInvoiceId = matchedInvoiceId;
    }

    public BankTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(BankTransactionStatus status) {
        this.status = status;
    }
}
