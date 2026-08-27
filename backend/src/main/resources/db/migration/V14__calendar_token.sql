-- Personal calendar feed token (webcal/iCal subscription URL). Random, per user.
ALTER TABLE app_user ADD COLUMN calendar_token VARCHAR(64);
CREATE UNIQUE INDEX idx_app_user_calendar_token ON app_user (calendar_token);
