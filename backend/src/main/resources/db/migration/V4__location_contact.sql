-- V4: contact person on a venue (kindergarten coordinator etc.) — prd §6A.8.
ALTER TABLE location ADD COLUMN contact_name  VARCHAR(255);
ALTER TABLE location ADD COLUMN contact_phone VARCHAR(255);
ALTER TABLE location ADD COLUMN contact_email VARCHAR(255);
