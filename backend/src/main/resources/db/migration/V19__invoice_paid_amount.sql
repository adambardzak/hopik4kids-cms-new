-- Actual paid amount override on invoices (parent paid more/less than invoiced, e.g. extra shirt).
ALTER TABLE invoice ADD COLUMN paid_amount INTEGER;
