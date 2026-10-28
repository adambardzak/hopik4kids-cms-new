-- V24: billing for kindergarten morning programs ("faktury školky").
-- The lesson itself stays a Program (type=school, school_part=morning) so it keeps propagating
-- into the schedule/calendar, shifts and attendance; this table only adds who pays and how much.

CREATE TABLE school_contract (
    id              VARCHAR(36)  PRIMARY KEY,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    program_id      VARCHAR(36)  NOT NULL UNIQUE REFERENCES program (id),
    billing_name    VARCHAR(255),
    billing_email   VARCHAR(255),
    billing_address VARCHAR(512),
    ico             VARCHAR(50),
    dic             VARCHAR(50),
    price           INTEGER      NOT NULL DEFAULT 0,
    price_unit      VARCHAR(20)  NOT NULL DEFAULT 'LESSON',
    note            TEXT
);

-- Invoices are no longer always tied to a child's registration: a school invoice has a contract instead.
ALTER TABLE invoice ALTER COLUMN registration_id DROP NOT NULL;
ALTER TABLE invoice ADD COLUMN school_contract_id VARCHAR(36) REFERENCES school_contract (id);
ALTER TABLE invoice ADD COLUMN payer_kind         VARCHAR(20) NOT NULL DEFAULT 'PARENT';
ALTER TABLE invoice ADD COLUMN payer_ico          VARCHAR(50);
ALTER TABLE invoice ADD COLUMN period_from        DATE;
ALTER TABLE invoice ADD COLUMN period_to          DATE;
-- "Sent by e-mail" is tracked on the invoice itself so the table can show it at a glance.
ALTER TABLE invoice ADD COLUMN sent_at            TIMESTAMPTZ;
ALTER TABLE invoice ADD COLUMN sent_count         INTEGER     NOT NULL DEFAULT 0;

CREATE INDEX idx_invoice_school_contract ON invoice (school_contract_id);
CREATE INDEX idx_invoice_payer_kind ON invoice (payer_kind);

-- Backfill sent_at/sent_count from the e-mail log (V23 onwards); the attachment name
-- is exactly "faktura-<invoice_number>.pdf", so the match is unambiguous.
UPDATE invoice i SET
    sent_at = s.last_sent,
    sent_count = s.cnt
FROM (
    SELECT attachment, MAX(created_at) AS last_sent, COUNT(*) AS cnt
    FROM email_log
    WHERE success AND attachment IS NOT NULL
    GROUP BY attachment
) s
WHERE s.attachment = 'faktura-' || i.invoice_number || '.pdf';
