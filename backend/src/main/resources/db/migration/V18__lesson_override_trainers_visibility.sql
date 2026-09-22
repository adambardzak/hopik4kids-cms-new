-- One-off lessons: number of trainers needed + admin-only visibility (schedule requests).
ALTER TABLE lesson_override ADD COLUMN trainers_needed INTEGER;
ALTER TABLE lesson_override ADD COLUMN admin_only BOOLEAN NOT NULL DEFAULT FALSE;
