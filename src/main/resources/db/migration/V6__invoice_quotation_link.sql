ALTER TABLE invoices ADD COLUMN quotation_id BIGINT;

ALTER TABLE invoices
    ADD CONSTRAINT fk_invoices_quotation
    FOREIGN KEY (quotation_id) REFERENCES quotations (id);

CREATE INDEX idx_invoices_quotation ON invoices (quotation_id);
