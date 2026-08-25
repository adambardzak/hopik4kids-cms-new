-- Shift-signup (prd §7.4): trainers sign up for lesson occurrences (program + date).

ALTER TABLE program ADD COLUMN trainers_needed INTEGER NOT NULL DEFAULT 1;

CREATE TABLE shift_signup (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    program_id  VARCHAR(36)  NOT NULL REFERENCES program (id) ON DELETE CASCADE,
    lesson_date DATE         NOT NULL,
    trainer_id  VARCHAR(36)  NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_shift_signup UNIQUE (program_id, lesson_date, trainer_id)
);

CREATE INDEX idx_shift_signup_program_date ON shift_signup (program_id, lesson_date);
CREATE INDEX idx_shift_signup_trainer ON shift_signup (trainer_id);
