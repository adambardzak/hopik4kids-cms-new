package cz.hopik4kids.cms.billing.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Credit note (dobropis) for an invoice. Has its own yearly numbering series ({@code D-2026-0001}),
 * independent of invoices. Cancels the whole invoice (full amount). Payer + items are snapshotted
 * from the invoice at issue time so history stays intact even if the invoice changes.
 */
@Entity
@Table(name = "credit_note")
public class CreditNote extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String number;

    /** The invoice this credit note reverses. */
    @Column(nullable = false)
    private String invoiceId;

    /** Snapshot of the invoice number (for display/PDF, independent of the invoice row). */
    @Column(nullable = false)
    private String invoiceNumber;

    @Column(nullable = false)
    private String payerName;

    @Column
    private String payerAddress;

    @Column
    private String payerEmail;

    /** JSON snapshot of the invoice items: [{label, qty, unitPrice}]. Amounts are positive here. */
    @Column(columnDefinition = "text", nullable = false)
    private String items;

    /** Total credited amount (= invoice total). Stored positive; rendered as a minus on the PDF. */
    @Column(nullable = false)
    private int totalAmount;

    @Column(nullable = false)
    private LocalDate issueDate;

    /** Variable symbol carried over from the invoice. */
    @Column
    private String variableSymbol;

    /** Optional reason for the credit note (e.g. "Zrušení registrace"). */
    @Column(columnDefinition = "text")
    private String reason;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public String getPayerAddress() {
        return payerAddress;
    }

    public void setPayerAddress(String payerAddress) {
        this.payerAddress = payerAddress;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public String getVariableSymbol() {
        return variableSymbol;
    }

    public void setVariableSymbol(String variableSymbol) {
        this.variableSymbol = variableSymbol;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
