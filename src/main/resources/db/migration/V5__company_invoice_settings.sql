ALTER TABLE company_accounts ADD COLUMN kvk_number VARCHAR(80);
ALTER TABLE company_accounts ADD COLUMN iban VARCHAR(80);
ALTER TABLE company_accounts ADD COLUMN payment_terms_days INT NOT NULL DEFAULT 14;
