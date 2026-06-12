ALTER TABLE business_documents
    ADD COLUMN invoice_id BIGINT;

ALTER TABLE business_documents
    ADD CONSTRAINT fk_documents_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id);

CREATE INDEX idx_documents_invoice ON business_documents (invoice_id);
