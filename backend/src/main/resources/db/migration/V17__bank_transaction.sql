-- Bank statement import + payment matching (prd todo #5). Stores processed transactions
-- (idempotency by bank transaction id) so re-uploading a statement can't double-pay an invoice.

CREATE TABLE bank_transaction (
    id             VARCHAR(36)   NOT NULL PRIMARY KEY,
    tx_id          VARCHAR(100)  NOT NULL,            -- bank's "Id transakce" (idempotency key)
    tx_date        DATE,
    amount         NUMERIC(12, 2) NOT NULL,
    variable_symbol VARCHAR(30),
    counterparty   VARCHAR(255),                      -- payer name / message
    message        TEXT,
    matched_invoice_id VARCHAR(36) REFERENCES invoice (id) ON DELETE SET NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'UNMATCHED', -- UNMATCHED | MATCHED | IGNORED
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_bank_transaction_txid UNIQUE (tx_id)
);

CREATE INDEX idx_bank_transaction_vs ON bank_transaction (variable_symbol);
CREATE INDEX idx_bank_transaction_status ON bank_transaction (status);
