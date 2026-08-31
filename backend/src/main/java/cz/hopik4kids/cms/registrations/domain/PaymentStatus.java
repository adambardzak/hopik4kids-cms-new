package cz.hopik4kids.cms.registrations.domain;

/**
 * Payment status of a registration (prd §3B.5, §6A.3).
 * INVOICE_SENT = an invoice has been generated and emailed but not yet paid.
 * "Po splatnosti" (overdue) is derived (invoice due date passed and not paid), not stored.
 */
public enum PaymentStatus {
    UNPAID,
    INVOICE_SENT,
    PAID,
    CANCELLED
}
