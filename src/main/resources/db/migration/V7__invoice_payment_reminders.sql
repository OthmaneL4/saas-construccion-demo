ALTER TABLE invoices ADD COLUMN last_payment_reminder_at DATETIME(6);
ALTER TABLE invoices ADD COLUMN payment_reminder_count INT NOT NULL DEFAULT 0;

CREATE INDEX idx_invoices_company_reminder ON invoices (company_account_id, last_payment_reminder_at);
