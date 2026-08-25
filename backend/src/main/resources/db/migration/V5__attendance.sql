-- V5: attendance records per child + program + lesson date (prd §6A.3).
CREATE TABLE attendance_record (
    id          VARCHAR(36)  PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    program_id  VARCHAR(36)  NOT NULL REFERENCES program (id),
    child_id    VARCHAR(36)  NOT NULL REFERENCES child (id),
    lesson_date DATE         NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    note        TEXT,
    CONSTRAINT uq_attendance UNIQUE (program_id, child_id, lesson_date)
);
CREATE INDEX idx_attendance_program_date ON attendance_record (program_id, lesson_date);
CREATE INDEX idx_attendance_child ON attendance_record (child_id);
