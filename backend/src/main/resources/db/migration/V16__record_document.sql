-- Accounting / HR document records (prd todo #8): receipts, DPP agreements, contracts.
-- Files are stored privately (NOT under public /media) and streamed via an authenticated
-- download endpoint. Sensitive by nature → owner/admin/accountant only.

CREATE TABLE record_document (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    type          VARCHAR(20)  NOT NULL,             -- RECEIPT | DPP | CONTRACT | OTHER
    title         VARCHAR(255) NOT NULL,
    person_id     VARCHAR(36)  REFERENCES app_user (id) ON DELETE SET NULL, -- optional (e.g. DPP for a trainer)
    person_name   VARCHAR(255),                      -- free-text when not a system user
    doc_date      DATE,
    amount        NUMERIC(12, 2),                    -- for receipts
    note          TEXT,
    stored_name   VARCHAR(255) NOT NULL,             -- filename on disk (uuid.ext)
    original_name VARCHAR(255),
    content_type  VARCHAR(100),
    size_bytes    BIGINT,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_record_document_type ON record_document (type);
CREATE INDEX idx_record_document_person ON record_document (person_id);
CREATE INDEX idx_record_document_date ON record_document (doc_date);
