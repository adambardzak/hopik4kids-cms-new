-- Schedule overrides (prd §7.4 extension): cancel / move a recurring lesson, or add a one-off.
CREATE TABLE lesson_override (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    type          VARCHAR(20)  NOT NULL,
    program_id    VARCHAR(36)  REFERENCES program (id) ON DELETE CASCADE,
    original_date DATE,
    lesson_date   DATE,
    time          VARCHAR(5),
    duration_min  INTEGER,
    title         VARCHAR(255),
    location_id   VARCHAR(36)  REFERENCES location (id) ON DELETE SET NULL,
    note          TEXT,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_lesson_override_program ON lesson_override (program_id);
CREATE INDEX idx_lesson_override_dates ON lesson_override (original_date, lesson_date);
