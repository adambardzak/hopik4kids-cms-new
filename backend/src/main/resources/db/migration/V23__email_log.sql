-- V23: log of every outgoing e-mail (recipient, subject, success/failure) so admins can trace delivery problems.
CREATE TABLE email_log (
    id          VARCHAR(36)  PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    recipient   VARCHAR(255) NOT NULL,
    subject     VARCHAR(500) NOT NULL,
    success     BOOLEAN      NOT NULL,
    error       TEXT,
    attachment  VARCHAR(255)
);
CREATE INDEX idx_email_log_created ON email_log (created_at DESC);
CREATE INDEX idx_email_log_recipient ON email_log (recipient);
