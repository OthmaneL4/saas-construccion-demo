# LSOTOTALBOUW - Security And Data Protection Hardening

## Purpose

This document defines the security and data protection controls required before LSOTOTALBOUW is used with real customer, contractor, invoice, payment, contract, or project document data.

The application is intended for construction businesses and self-employed contractors in the Netherlands. Production use must therefore consider GDPR/AVG obligations, business confidentiality, financial records, and operational continuity.

## Current Security Baseline

The application already includes the following controls:

- Spring Security form login.
- BCrypt password hashing.
- Role-based access control by business module.
- CSRF protection for state-changing requests.
- Secure session cookie configuration for production.
- H2 console disabled in production.
- Demo login disabled in production.
- Generic production error responses without stack traces.
- Content Security Policy and Referrer Policy headers.
- Login failure tracking and temporary account lockout.
- Audit trail for sensitive business and security actions.
- Production bootstrap validation for the first administrator account.
- Flyway-controlled schema changes with Hibernate validation in production.
- External document storage through `DOCUMENTS_DIR`.
- Production startup validation for document storage.
- Health monitoring for document storage availability.
- SHA-256 integrity checks for stored documents before preview or download.
- Local document security scanning for known high-risk signatures before accepting uploaded files.
- MySQL application user separated from the MySQL root account.

## Sensitive Data Inventory

The system can store or process these sensitive data categories:

- User account data: names, emails, roles, login status, failed login counters.
- Customer data: names, companies, emails, phone numbers, addresses, notes.
- Supplier data: business contacts, emails, phone numbers, addresses.
- Project data: customer relationships, work descriptions, planning information.
- Financial data: quotations, invoices, payments, receivables, expenses, totals, VAT information.
- Operational data: work logs, materials, tools, calendar entries.
- Documents: contracts, photos, PDFs, invoices, repair reports, and project evidence.
- Audit data: user actions, login events, downloads, payment actions, document actions.

## GDPR/AVG Control Requirements

Before production use with real data, complete these controls:

- Define the legal basis for processing customer, contractor, supplier, and employee data.
- Define retention periods for customers, invoices, audit logs, documents, and archived records.
- Define a procedure to export customer data when a data subject requests access.
- Define a procedure to delete or anonymize personal data when legally allowed.
- Define which financial and tax records must be retained even if deletion is requested.
- Define who is responsible for privacy requests and incident decisions.
- Document all processors involved in hosting, backups, email, storage, and monitoring.

## Production Hardening Requirements

Production must satisfy these requirements before real data is entered:

- HTTPS must terminate at a trusted proxy or platform.
- `server.servlet.session.cookie.secure=true` must remain enabled.
- Database credentials must be stored outside Git.
- Bootstrap administrator credentials must be rotated after the first login.
- MySQL must run with a dedicated application user, not `root`.
- `DOCUMENTS_DIR` must be outside the application artifact and outside any public web root.
- MySQL and `DOCUMENTS_DIR` must be backed up together.
- Restore must be tested before production launch.
- The latest CI run must be green.
- The smoke test script must pass against the target environment.
- Logs must not contain passwords, tokens, database credentials, or private document content.

## Document Security Requirements

Documents are among the highest-risk data assets in this application.

Required controls:

- Store uploaded files using generated server-side filenames.
- Keep original filenames as metadata only.
- Validate file type and size before storage.
- Reject files that match known high-risk malware or script signatures before making them available.
- Store and verify a SHA-256 checksum for each document before preview or download.
- Block direct public access to stored files.
- Authorize every document download through the application.
- Audit document upload, download, update, and archive actions.
- Audit and notify document integrity failures.
- Include document storage in backup and restore procedures.

Future recommended controls:

- Connect the existing document scanning integration point to a production antivirus or malware scanning provider.
- Optional document versioning.
- Optional encryption at rest for the document storage volume.

## Access Control Review

Before production use, review each role with the business owner:

| Role | Intended Access | Review Required |
| --- | --- | --- |
| `OWNER` | Full business and administration access. | Confirm only the business owner has this role. |
| `ADMIN` | Full administrative access. | Confirm it is not assigned casually. |
| `FINANCE` | Financial modules and customer context. | Confirm access to invoices, payments, quotations, and expenses is appropriate. |
| `OPERATIONS` | Projects, planning, work logs, customers, inventory context. | Confirm no unnecessary financial access. |
| `INVENTORY` | Materials, tools, suppliers. | Confirm no unnecessary customer/finance access. |
| `DOCUMENTS` | Document workspace and shared notifications. | Confirm document access is appropriate. |
| `AUDITOR` | Audit trail and read-oriented operational overview. | Confirm no modification permissions are granted. |

## Incident Response

If a security or privacy incident is suspected:

1. Preserve logs and audit records.
2. Record the exact time, affected user, module, data type, and action.
3. Disable or lock affected accounts if needed.
4. Remove the instance from traffic if active exploitation is suspected.
5. Preserve MySQL and `DOCUMENTS_DIR` before making destructive changes.
6. Determine whether personal data was exposed, modified, deleted, or downloaded.
7. Decide whether GDPR/AVG notification obligations apply.
8. Document root cause and corrective actions before returning to normal operation.

## Final Security Go/No-Go

The project is not ready for real production data if any of these are true:

- HTTPS is not configured.
- Production cookies are not secure.
- H2 console or demo login is enabled.
- Real credentials are stored in Git or documentation.
- Backups have not been tested.
- Document storage is publicly accessible.
- Role permissions have not been reviewed with the business owner.
- Security smoke tests or CI are failing.
- Privacy request and incident procedures are undefined.
