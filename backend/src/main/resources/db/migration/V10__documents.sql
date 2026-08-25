-- V10: internal documents / handbooks for trainers (prd §6A.8 B).
CREATE TABLE document (
    id         VARCHAR(36)  PRIMARY KEY,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    category   VARCHAR(20)  NOT NULL,
    file_id    VARCHAR(36)  REFERENCES media (id),
    content    TEXT,
    visibility VARCHAR(20)  NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0
);
CREATE INDEX idx_document_category ON document (category, sort_order);
