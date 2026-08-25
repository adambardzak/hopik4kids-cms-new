-- V1: Hopik4Kids CMS - target data model (prd §3B). Phase 0 skeleton.
-- Public string ids (= preserved Strapi documentId, prd §3C/§10). All entities carry created_at/updated_at.

-- ============================ users & rbac module ============================

CREATE TABLE app_user (
    id            VARCHAR(36)  PRIMARY KEY,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    phone         VARCHAR(255),
    color         VARCHAR(20),
    last_login_at TIMESTAMPTZ
);

CREATE TABLE invitation (
    id          VARCHAR(36)  PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    email       VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    invited_by  VARCHAR(36)  REFERENCES app_user (id),
    expires_at  TIMESTAMPTZ  NOT NULL,
    accepted_at TIMESTAMPTZ
);
CREATE INDEX idx_invitation_email ON invitation (email);

CREATE TABLE audit_log (
    id         VARCHAR(36)  PRIMARY KEY,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    user_id    VARCHAR(36),
    action     VARCHAR(255) NOT NULL,
    entity     VARCHAR(255) NOT NULL,
    entity_id  VARCHAR(36),
    meta       JSONB
);
CREATE INDEX idx_audit_entity ON audit_log (entity, entity_id);

-- ================================ core module ================================

CREATE TABLE location (
    id         VARCHAR(36)  PRIMARY KEY,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    kind       VARCHAR(20)  NOT NULL,
    address    VARCHAR(255),
    note       TEXT
);

CREATE TABLE media (
    id         VARCHAR(36)  PRIMARY KEY,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    url        VARCHAR(1024) NOT NULL,
    alt        VARCHAR(512),
    width      INTEGER,
    height     INTEGER,
    variants   JSONB
);

CREATE TABLE program (
    id               VARCHAR(36)  PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    type             VARCHAR(20)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    slug             VARCHAR(255) UNIQUE,
    location_id      VARCHAR(36)  REFERENCES location (id),
    price            INTEGER      NOT NULL,
    capacity         INTEGER,
    spots_taken      INTEGER      NOT NULL DEFAULT 0,
    access_mode      VARCHAR(20)  NOT NULL,
    restriction_note TEXT,
    access_code_hash VARCHAR(255),
    shirt_policy     VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    weekday          INTEGER,
    time             VARCHAR(5),
    school_part      VARCHAR(20),
    valid_from       DATE,
    valid_to         DATE,
    duration_min     INTEGER,
    start_date       DATE,
    end_date         DATE
);
CREATE INDEX idx_program_type_status ON program (type, status);
CREATE INDEX idx_program_location ON program (location_id);

CREATE TABLE article (
    id           VARCHAR(36)  PRIMARY KEY,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    slug         VARCHAR(255) NOT NULL UNIQUE,
    excerpt      TEXT,
    content      TEXT,
    cover_id     VARCHAR(36)  REFERENCES media (id),
    published_at TIMESTAMPTZ
);
CREATE INDEX idx_article_published ON article (published_at);

-- ============================ registrations module ==========================

CREATE TABLE parent (
    id           VARCHAR(36)  PRIMARY KEY,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    name         VARCHAR(255) NOT NULL,
    phone        VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    second_name  VARCHAR(255),
    second_phone VARCHAR(255)
);

CREATE TABLE child (
    id               VARCHAR(36)  PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    full_name        VARCHAR(255) NOT NULL,
    birth_date       DATE         NOT NULL,
    personal_id      TEXT         NOT NULL,  -- encrypted at-rest (AES-GCM, prd §3B.9)
    address          VARCHAR(255) NOT NULL,
    health_insurance VARCHAR(255) NOT NULL,
    parent_id        VARCHAR(36)  NOT NULL REFERENCES parent (id)
);
CREATE INDEX idx_child_parent ON child (parent_id);

CREATE TABLE registration (
    id                    VARCHAR(36) PRIMARY KEY,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL,
    program_id            VARCHAR(36) NOT NULL REFERENCES program (id),
    child_id              VARCHAR(36) NOT NULL REFERENCES child (id),
    class_name            VARCHAR(255),
    wants_shirt           BOOLEAN     NOT NULL,
    shirt_size            VARCHAR(20),
    nick_name             VARCHAR(255),
    allergies             TEXT,
    note                  TEXT,
    consent_personal_data BOOLEAN     NOT NULL,
    consent_media         BOOLEAN     NOT NULL,
    payment_status        VARCHAR(20) NOT NULL,
    price_snapshot        INTEGER     NOT NULL,
    status                VARCHAR(20) NOT NULL,
    source                VARCHAR(255)
);
CREATE INDEX idx_registration_program ON registration (program_id);
CREATE INDEX idx_registration_child ON registration (child_id);
CREATE INDEX idx_registration_status ON registration (status);
CREATE INDEX idx_registration_payment ON registration (payment_status);
