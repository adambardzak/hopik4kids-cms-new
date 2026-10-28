package cz.hopik4kids.cms.billing.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Invoice for a registration (prd §6A.5). Payer = parent (snapshot from the registration).
 * {@code invoiceNumber} is the year-based sequential number and doubles as the variable symbol.
 * {@code items} is a JSON array of {@code {label, qty, unitPrice}} (program + optional shirt).
 * Amounts snapshot the price at issue time (independent of later price changes).
 */
@Entity
@Table(name = "invoice")
public class Invoice extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    /** Set for parent invoices; null for school invoices (then {@link #schoolContractId} is set). */
    @Column
    private String registrationId;

    /** Set for kindergarten invoices (morning programs) — mutually exclusive with registrationId. */
    @Column
    private String schoolContractId;

    /** Who is billed: a parent (registration) or a kindergarten (school contract). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoicePayerKind payerKind = InvoicePayerKind.PARENT;

    /** club | school | camp — type of the source program. */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private String payerName;

    @Column
    private String payerAddress;

    @Column
    private String payerEmail;

    /** Buyer's company id — only school invoices carry one (required on a B2B invoice). */
    @Column(length = 50)
    private String payerIco;

    /** Billed period (school invoices): first and last day the invoice covers. */
    @Column
    private LocalDate periodFrom;

    @Column
    private LocalDate periodTo;

    /** JSON: [{label, qty, unitPrice}] */
    @Column(columnDefinition = "text", nullable = false)
    private String items;

    @Column(nullable = false)
    private int totalAmount;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private String variableSymbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    @Column
    private Instant paidAt;

    /** When the invoice PDF was last e-mailed to the payer; null = never sent. */
    @Column
    private Instant sentAt;

    /** How many times the invoice was e-mailed (re-sends included). */
    @Column(nullable = false)
    private int sentCount = 0;

    /**
     * Actual amount the parent paid, when it differs from {@code totalAmount}
     * (e.g. they added an extra shirt and paid more). Null = paid exactly the invoiced amount.
     * This is what the accountant exports.
     */
    @Column
    private Integer paidAmount;

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getVariableSymbol() {
        return variableSymbol;
    }

    public void setVariableSymbol(String variableSymbol) {
        this.variableSymbol = variableSymbol;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Integer getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Integer paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getSchoolContractId() {
        return schoolContractId;
    }

    public void setSchoolContractId(String schoolContractId) {
        this.schoolContractId = schoolContractId;
    }

    public InvoicePayerKind getPayerKind() {
        return payerKind;
    }

    public void setPayerKind(InvoicePayerKind payerKind) {
        this.payerKind = payerKind;
    }

    public String getPayerIco() {
        return payerIco;
    }

    public void setPayerIco(String payerIco) {
        this.payerIco = payerIco;
    }

    public LocalDate getPeriodFrom() {
        return periodFrom;
    }

    public void setPeriodFrom(LocalDate periodFrom) {
        this.periodFrom = periodFrom;
    }

    public LocalDate getPeriodTo() {
        return periodTo;
    }

    public void setPeriodTo(LocalDate periodTo) {
        this.periodTo = periodTo;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public int getSentCount() {
        return sentCount;
    }

    public void setSentCount(int sentCount) {
        this.sentCount = sentCount;
    }
}
