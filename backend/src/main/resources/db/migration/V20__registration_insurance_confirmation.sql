-- Parent can request a health-insurance payment confirmation, sent automatically once paid.
ALTER TABLE registration ADD COLUMN wants_insurance_confirmation BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE registration ADD COLUMN insurance_confirmation_sent BOOLEAN NOT NULL DEFAULT FALSE;
