CREATE TABLE company_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    name VARCHAR(160) NOT NULL,
    vat_number VARCHAR(80),
    email VARCHAR(160),
    phone VARCHAR(80),
    address VARCHAR(240),
    PRIMARY KEY (id)
);

CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    name VARCHAR(60) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    password VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE customers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    email VARCHAR(160),
    phone VARCHAR(80),
    address VARCHAR(240),
    city VARCHAR(80),
    PRIMARY KEY (id),
    CONSTRAINT fk_customers_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id)
);

CREATE TABLE projects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    name VARCHAR(180) NOT NULL,
    work_address VARCHAR(240),
    start_date DATE,
    end_date DATE,
    status VARCHAR(40) NOT NULL,
    budget DECIMAL(12,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_projects_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id),
    CONSTRAINT fk_projects_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);

CREATE TABLE quotations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    quotation_number VARCHAR(40) NOT NULL,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(500),
    amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    issue_date DATE,
    expiry_date DATE,
    status VARCHAR(40) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_quotations_company_number UNIQUE (company_account_id, quotation_number),
    CONSTRAINT fk_quotations_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id),
    CONSTRAINT fk_quotations_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);

CREATE TABLE quotation_lines (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    quotation_id BIGINT NOT NULL,
    description VARCHAR(240) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    vat_rate DECIMAL(5,2) NOT NULL DEFAULT 21,
    PRIMARY KEY (id),
    CONSTRAINT fk_quotation_lines_quotation FOREIGN KEY (quotation_id) REFERENCES quotations (id)
);

CREATE TABLE invoices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    project_id BIGINT,
    invoice_number VARCHAR(40) NOT NULL,
    amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    issue_date DATE,
    due_date DATE,
    status VARCHAR(40) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_invoices_company_number UNIQUE (company_account_id, invoice_number),
    CONSTRAINT fk_invoices_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id),
    CONSTRAINT fk_invoices_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_invoices_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE invoice_lines (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    invoice_id BIGINT NOT NULL,
    description VARCHAR(240) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    vat_rate DECIMAL(5,2) NOT NULL DEFAULT 21,
    PRIMARY KEY (id),
    CONSTRAINT fk_invoice_lines_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
);

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_date DATE NOT NULL,
    method VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payments_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id),
    CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
);

CREATE TABLE expenses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    project_id BIGINT,
    description VARCHAR(160) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    category VARCHAR(80) NOT NULL,
    expense_date DATE,
    PRIMARY KEY (id),
    CONSTRAINT fk_expenses_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id),
    CONSTRAINT fk_expenses_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE materials (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    unit VARCHAR(40) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    minimum_stock INT NOT NULL DEFAULT 0,
    unit_cost DECIMAL(12,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_materials_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id)
);

CREATE TABLE tools (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    serial_number VARCHAR(80),
    status VARCHAR(40) NOT NULL,
    next_maintenance_date DATE,
    PRIMARY KEY (id),
    CONSTRAINT fk_tools_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id)
);

CREATE TABLE suppliers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    contact_name VARCHAR(120),
    email VARCHAR(160),
    phone VARCHAR(80),
    address VARCHAR(240),
    city VARCHAR(80),
    PRIMARY KEY (id),
    CONSTRAINT fk_suppliers_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id)
);

CREATE TABLE calendar_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    project_id BIGINT,
    title VARCHAR(160) NOT NULL,
    notes VARCHAR(500),
    event_date DATE NOT NULL,
    type VARCHAR(40) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_calendar_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id),
    CONSTRAINT fk_calendar_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE business_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    customer_id BIGINT,
    project_id BIGINT,
    title VARCHAR(180) NOT NULL,
    category VARCHAR(40) NOT NULL,
    original_filename VARCHAR(240) NOT NULL,
    stored_filename VARCHAR(120) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    notes VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT uk_documents_stored_filename UNIQUE (stored_filename),
    CONSTRAINT fk_documents_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id),
    CONSTRAINT fk_documents_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_documents_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE work_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    worker_name VARCHAR(160) NOT NULL,
    description VARCHAR(240) NOT NULL,
    hours DECIMAL(8,2) NOT NULL DEFAULT 0,
    hourly_rate DECIMAL(10,2) NOT NULL DEFAULT 0,
    billable BOOLEAN NOT NULL DEFAULT TRUE,
    invoice_line_id BIGINT,
    status VARCHAR(40) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_work_logs_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id),
    CONSTRAINT fk_work_logs_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_work_logs_invoice_line FOREIGN KEY (invoice_line_id) REFERENCES invoice_lines (id)
);

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    company_account_id BIGINT NOT NULL,
    user_id BIGINT,
    action VARCHAR(40) NOT NULL,
    module_name VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80),
    summary VARCHAR(240) NOT NULL,
    details VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_company FOREIGN KEY (company_account_id) REFERENCES company_accounts (id),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_users_company_active_email ON users (company_account_id, active, email);
CREATE INDEX idx_customers_company_active_name ON customers (company_account_id, active, name);
CREATE INDEX idx_projects_company_active_start ON projects (company_account_id, active, start_date);
CREATE INDEX idx_projects_company_status_active ON projects (company_account_id, status, active);
CREATE INDEX idx_projects_customer ON projects (customer_id);
CREATE INDEX idx_quotations_company_active_date ON quotations (company_account_id, active, issue_date);
CREATE INDEX idx_quotations_company_status_active ON quotations (company_account_id, status, active);
CREATE INDEX idx_quotations_customer ON quotations (customer_id);
CREATE INDEX idx_quotation_lines_quotation_active ON quotation_lines (quotation_id, active);
CREATE INDEX idx_invoices_company_active_due ON invoices (company_account_id, active, due_date);
CREATE INDEX idx_invoices_company_status_active ON invoices (company_account_id, status, active);
CREATE INDEX idx_invoices_customer ON invoices (customer_id);
CREATE INDEX idx_invoices_project ON invoices (project_id);
CREATE INDEX idx_invoice_lines_invoice_active ON invoice_lines (invoice_id, active);
CREATE INDEX idx_payments_company_active_date ON payments (company_account_id, active, payment_date);
CREATE INDEX idx_payments_invoice ON payments (invoice_id);
CREATE INDEX idx_expenses_company_active_date ON expenses (company_account_id, active, expense_date);
CREATE INDEX idx_expenses_project ON expenses (project_id);
CREATE INDEX idx_materials_company_active_name ON materials (company_account_id, active, name);
CREATE INDEX idx_tools_company_active_name ON tools (company_account_id, active, name);
CREATE INDEX idx_tools_company_status_active ON tools (company_account_id, status, active);
CREATE INDEX idx_suppliers_company_active_name ON suppliers (company_account_id, active, name);
CREATE INDEX idx_calendar_company_active_date ON calendar_events (company_account_id, active, event_date);
CREATE INDEX idx_calendar_project ON calendar_events (project_id);
CREATE INDEX idx_documents_company_active_category ON business_documents (company_account_id, active, category);
CREATE INDEX idx_documents_customer ON business_documents (customer_id);
CREATE INDEX idx_documents_project ON business_documents (project_id);
CREATE INDEX idx_work_logs_company_active_date ON work_logs (company_account_id, active, work_date);
CREATE INDEX idx_work_logs_project ON work_logs (project_id);
CREATE INDEX idx_work_logs_company_status ON work_logs (company_account_id, status);
CREATE INDEX idx_audit_company_created ON audit_logs (company_account_id, created_at);
CREATE INDEX idx_audit_company_module ON audit_logs (company_account_id, module_name);
CREATE INDEX idx_audit_company_action ON audit_logs (company_account_id, action);
