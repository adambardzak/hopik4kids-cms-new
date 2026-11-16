-- V25: PR manager role (filming signups) + monthly work reports with an hourly rate per person.

-- Hourly rate (CZK/h) for the monthly work report; NULL = not set yet.
ALTER TABLE app_user ADD COLUMN hourly_rate NUMERIC(8, 2);

-- TRAINER = regular shift (counts towards trainersNeeded), FILMING = PR manager comes to film.
ALTER TABLE shift_signup ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'TRAINER';

-- One monthly report per person and month (period = 'YYYY-MM'). A row exists once the person
-- submits; totals + rate are frozen at submit/approve so later rate changes don't rewrite history.
CREATE TABLE work_report (
    id           VARCHAR(36)   PRIMARY KEY,
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL,
    user_id      VARCHAR(36)   NOT NULL REFERENCES app_user (id),
    period       VARCHAR(7)    NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    total_hours  NUMERIC(7, 2) NOT NULL DEFAULT 0,
    hourly_rate  NUMERIC(8, 2),
    total_amount NUMERIC(10, 2),
    submitted_at TIMESTAMPTZ,
    decided_at   TIMESTAMPTZ,
    decided_by   VARCHAR(36),
    return_note  TEXT,
    CONSTRAINT uq_work_report_user_period UNIQUE (user_id, period)
);
CREATE INDEX idx_work_report_period ON work_report (period);
