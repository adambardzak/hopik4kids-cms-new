-- V7: invoicing (prd §6A.5).

CREATE TABLE supplier_settings (
    id               VARCHAR(36)  PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    ico              VARCHAR(50),
    dic              VARCHAR(50),
    address          VARCHAR(512),
    iban             VARCHAR(64),
    account_number   VARCHAR(64),
    default_due_days INTEGER      NOT NULL DEFAULT 14,
    footer_text      TEXT
);

CREATE TABLE invoice (
    id              VARCHAR(36)  PRIMARY KEY,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    invoice_number  VARCHAR(32)  NOT NULL UNIQUE,
    registration_id VARCHAR(36)  NOT NULL REFERENCES registration (id),
    type            VARCHAR(20)  NOT NULL,
    payer_name      VARCHAR(255) NOT NULL,
    payer_address   VARCHAR(512),
    payer_email     VARCHAR(255),
    items           TEXT         NOT NULL,
    total_amount    INTEGER      NOT NULL,
    issue_date      DATE         NOT NULL,
    due_date        DATE         NOT NULL,
    variable_symbol VARCHAR(32)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    paid_at         TIMESTAMPTZ
);
CREATE INDEX idx_invoice_registration ON invoice (registration_id);
CREATE INDEX idx_invoice_status ON invoice (status);

-- Atomic per-year invoice numbering (row locked on increment).
CREATE TABLE invoice_counter (
    year        INTEGER PRIMARY KEY,
    last_number INTEGER NOT NULL DEFAULT 0
);
