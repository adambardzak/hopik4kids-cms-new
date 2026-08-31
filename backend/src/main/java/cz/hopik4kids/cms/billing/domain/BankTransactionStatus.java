package cz.hopik4kids.cms.billing.domain;

/** Matching state of an imported bank transaction (prd todo #5). */
public enum BankTransactionStatus {
    UNMATCHED,
    MATCHED,
    IGNORED
}
