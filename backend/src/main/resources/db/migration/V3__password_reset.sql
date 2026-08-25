-- V3: password reset tokens (prd §7.1).
CREATE TABLE password_reset_token (
    id         VARCHAR(36)  PRIMARY KEY,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    user_id    VARCHAR(36)  NOT NULL REFERENCES app_user (id),
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used_at    TIMESTAMPTZ
);
CREATE INDEX idx_password_reset_token_hash ON password_reset_token (token_hash);
