-- V9: waitlist for full programs (prd §6A.2). Lightweight, no sensitive data.
CREATE TABLE waitlist_entry (
    id           VARCHAR(36)  PRIMARY KEY,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    program_id   VARCHAR(36)  NOT NULL REFERENCES program (id),
    child_name   VARCHAR(255) NOT NULL,
    parent_name  VARCHAR(255) NOT NULL,
    parent_phone VARCHAR(255) NOT NULL,
    parent_email VARCHAR(255) NOT NULL,
    note         TEXT,
    status       VARCHAR(20)  NOT NULL
);
CREATE INDEX idx_waitlist_program ON waitlist_entry (program_id, created_at);
CREATE INDEX idx_waitlist_status ON waitlist_entry (status);
