-- V6: assign trainers to programs (M:N) for scoped access (prd §7.5).
CREATE TABLE program_trainer (
    program_id VARCHAR(36) NOT NULL REFERENCES program (id) ON DELETE CASCADE,
    trainer_id VARCHAR(36) NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    PRIMARY KEY (program_id, trainer_id)
);
CREATE INDEX idx_program_trainer_trainer ON program_trainer (trainer_id);
