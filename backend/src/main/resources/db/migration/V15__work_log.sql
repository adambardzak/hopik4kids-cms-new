-- Work logs / timesheets for part-time staff (prd todo #3). Trainers record hours,
-- admins approve and export. Hours can be seeded from approved shift signups.

CREATE TABLE work_log (
    id          VARCHAR(36)   NOT NULL PRIMARY KEY,
    trainer_id  VARCHAR(36)   NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    work_date   DATE          NOT NULL,
    hours       NUMERIC(5, 2) NOT NULL,
    note        TEXT,
    source      VARCHAR(20)   NOT NULL DEFAULT 'MANUAL', -- MANUAL | SHIFT
    status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING', -- PENDING | APPROVED | REJECTED
    program_id  VARCHAR(36)   REFERENCES program (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_work_log_trainer_date ON work_log (trainer_id, work_date);
CREATE INDEX idx_work_log_status ON work_log (status);
-- Prevent duplicate shift-seeded rows for the same trainer+program+date.
CREATE UNIQUE INDEX uq_work_log_shift
    ON work_log (trainer_id, program_id, work_date)
    WHERE source = 'SHIFT';
