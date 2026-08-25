-- V8: supplier web + email for invoice header.
ALTER TABLE supplier_settings ADD COLUMN web   VARCHAR(255);
ALTER TABLE supplier_settings ADD COLUMN email VARCHAR(255);
