package cz.hopik4kids.cms.billing.domain;

/** Invoice payment status (prd §6A.5). */
public enum InvoiceStatus {
    UNPAID,
    PAID,
    CANCELLED
}
