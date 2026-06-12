ALTER TABLE business_documents
    ADD COLUMN sha256_checksum VARCHAR(64);

CREATE INDEX idx_documents_company_checksum
    ON business_documents (company_account_id, sha256_checksum);
