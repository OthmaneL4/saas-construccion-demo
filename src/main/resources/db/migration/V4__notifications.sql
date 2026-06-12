CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    source_key VARCHAR(160) NOT NULL,
    severity VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(500) NOT NULL,
    target_url VARCHAR(240) NOT NULL,
    due_date DATE,
    read_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_notifications_company_source UNIQUE (company_account_id, source_key),
    CONSTRAINT fk_notifications_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id)
);

CREATE INDEX idx_notifications_company_read ON notifications (company_account_id, read_at);
CREATE INDEX idx_notifications_company_due ON notifications (company_account_id, due_date);
