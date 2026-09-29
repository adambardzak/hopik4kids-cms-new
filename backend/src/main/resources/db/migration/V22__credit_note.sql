-- Credit notes (dobropisy) with their own yearly numbering series (D-2026-0001), independent
-- of invoice numbering. Each credit note reverses one invoice (full amount).

CREATE TABLE credit_note_counter (
    year        INTEGER NOT NULL PRIMARY KEY,
    last_number INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE credit_note (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    number          VARCHAR(32)  NOT NULL UNIQUE,
    invoice_id      VARCHAR(36)  NOT NULL REFERENCES invoice (id) ON DELETE CASCADE,
    invoice_number  VARCHAR(32)  NOT NULL,
    payer_name      VARCHAR(255) NOT NULL,
    payer_address   VARCHAR(255),
    payer_email     VARCHAR(255),
    items           TEXT         NOT NULL,
    total_amount    INTEGER      NOT NULL,
    issue_date      DATE         NOT NULL,
    variable_symbol VARCHAR(32),
    reason          TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_credit_note_invoice ON credit_note (invoice_id);
