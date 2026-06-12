CREATE TABLE invoice_payment_reminders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    user_id BIGINT,
    reminder_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    outstanding_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    message VARCHAR(1000),
    sent_at DATETIME(6) NOT NULL,
    generated_by_name VARCHAR(120),
    generated_by_email VARCHAR(160),
    PRIMARY KEY (id),
    CONSTRAINT fk_invoice_payment_reminders_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id),
    CONSTRAINT fk_invoice_payment_reminders_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_invoice_payment_reminders_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_invoice_payment_reminders_invoice ON invoice_payment_reminders (invoice_id, sent_at);
CREATE INDEX idx_invoice_payment_reminders_company ON invoice_payment_reminders (company_account_id, sent_at);
