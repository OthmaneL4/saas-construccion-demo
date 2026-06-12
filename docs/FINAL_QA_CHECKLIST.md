# LSOTOTALBOUW - Final QA Checklist

## Scope

This checklist tracks the final functional verification before presenting LSOTOTALBOUW as a production-ready business management application.

For the executive project handoff status, review `docs/PROJECT_HANDOFF_SUMMARY.md`.

## Environment Verified

- Latest release candidate: `releases/20260612_125712-clean-release-candidate`
- Release Git revision: `f49b4830d591e638dc8c009ab0118157c83597df`
- Release manifest: valid JSON, tests enabled, jar SHA-256 verified, Git dirty flag `false`
- Local smoke-test profile: default `dev` profile against local H2 data
- Authentication: `admin@lsototalbouw.nl` development admin user
- Production validation assets: `prod` profile, MySQL/Flyway configuration, bootstrap validation, preflight, backup, restore, release, and smoke-test scripts are prepared

## Smoke Test - Authenticated Navigation

The following routes were verified with an authenticated admin session and returned HTTP `200`:

- `/actuator/health`
- `/dashboard`
- `/customers`
- `/projects`
- `/quotations`
- `/invoices`
- `/payments`
- `/receivables`
- `/expenses`
- `/materials`
- `/tools`
- `/suppliers`
- `/calendar`
- `/documents`
- `/work-logs`
- `/profitability`
- `/audit`
- `/users`
- `/settings/company`
- `/notifications`

Automated coverage:

- Added `AuthenticatedNavigationIT` to verify the primary authenticated pages continue to return HTTP `200` for an OWNER/ADMIN user.
- Executed `scripts/smoke-test.ps1` against the clean release candidate jar and verified health, login, and all primary authenticated pages.
- Completed desktop/mobile visual walkthrough for Dashboard and Auditor; sidebar scrolling and audit filter actions render correctly.

## CRUD Flow - Finance And Operations

The following authenticated workflow was executed successfully against MySQL:

- Created a QA customer.
- Opened the customer detail page.
- Edited the QA customer.
- Created a QA project linked to that customer.
- Opened the project detail page.
- Edited the QA project.
- Created a QA invoice linked to the customer/project.
- Opened the invoice detail page.
- Added an invoice line and verified invoice amount recalculation.
- Created a pending payment.
- Opened the payment detail page.
- Confirmed the pending payment and verified invoice paid amount update.
- Archived the payment and verified the invoice paid amount was reversed.
- Archived the QA invoice, project, and customer.
- Verified audit events were written for the QA flow.

## CRUD Flow - Calendar And Notifications

The following authenticated workflow was executed successfully against MySQL:

- Created a QA calendar event.
- Opened the event detail page.
- Edited the QA calendar event.
- Archived the QA calendar event.
- Created a QA manual notification.
- Marked the notification as read.
- Repeated the read action and verified it remained idempotent.
- Verified calendar audit events were written.

## CRUD Flow - Inventory And Suppliers

The following authenticated workflow was executed successfully against MySQL:

- Created a QA material.
- Opened the material detail page.
- Edited the QA material.
- Archived the QA material.
- Created a QA tool.
- Opened the tool detail page.
- Edited the QA tool and changed its status.
- Archived the QA tool.
- Created a QA supplier.
- Opened the supplier detail page.
- Edited the QA supplier.
- Archived the QA supplier.
- Verified audit events were written for inventory and supplier actions.

## Document Upload And Validation

The following authenticated workflow was executed successfully against MySQL and the configured document directory:

- Uploaded an allowed PDF document.
- Opened the document detail page.
- Downloaded the stored PDF and verified a non-empty response.
- Edited document metadata.
- Archived the document.
- Attempted to upload a disallowed text/XML file.
- Verified the disallowed upload did not create a database record.
- Verified document audit events were written.

## Quotation To Invoice And Payment Reminder Flow

The following authenticated workflow was executed successfully against MySQL:

- Created a QA customer for quotation testing.
- Created a QA quotation.
- Opened the quotation detail page.
- Added a quotation line and verified quotation amount recalculation.
- Generated and downloaded the quotation PDF.
- Converted the quotation to an invoice.
- Verified the converted invoice was created with copied line items and matching amount.
- Verified the source quotation was marked as accepted.
- Repeated the conversion action and verified no duplicate active invoice was created.
- Opened the converted invoice detail page.
- Generated and downloaded the invoice PDF.
- Registered a manual payment reminder.
- Generated and downloaded a payment reminder PDF.
- Verified reminder history rows and invoice reminder counter were updated.
- Archived the QA invoice, quotation, and customer.
- Verified audit events were written for conversion, PDF generation, and reminders.

## Security And Static Assets

The following security regression check was executed successfully:

- Verified `/js/app.js` is publicly accessible as a static asset.
- Verified the dashboard references the external JavaScript file instead of inline scripts.
- Verified the Content Security Policy still allows self-hosted scripts and does not allow unsafe inline scripts.
- Verified the `DOCUMENTS` role can access the document workspace and shared notification area.
- Verified the `DOCUMENTS` role cannot access invoices, audit, or user management.
- Verified oversized document uploads are rejected without persisting document metadata.

## Responsive Layout Review

The following browser-based responsive checks were executed successfully:

- Verified the authenticated dashboard at desktop width has no global horizontal overflow.
- Verified the desktop sidebar remains sticky and scrollable when the full navigation exceeds viewport height.
- Verified the authenticated dashboard at mobile width has no global horizontal overflow.
- Verified the mobile navigation collapses to a single-column layout on small screens.
- Verified the invoice table overflows only inside its `.table-responsive` container, without breaking the page width.

## Sensitive Action Audit Trail

The following audit traceability checks were executed successfully:

- Verified payment creation writes a `CREATE` audit event in the `Pagos` module.
- Verified payment confirmation writes an `UPDATE` audit event tied to the same payment entity.
- Verified payment archive writes an `ARCHIVE` audit event tied to the same payment entity.
- Verified the payment audit lifecycle stores the acting user email for investigation.

## Operational Readiness

The following production operation assets are prepared:

- Added `.github/workflows/ci.yml` so GitHub Actions compiles the application and runs the automated test suite on pushes and pull requests.
- Added `scripts/build-release.ps1` to create traceable release artifacts with `release-manifest.json` and jar SHA-256 checksums.
- Added `scripts/verify-release.ps1` to validate release manifests, jar presence, size, SHA-256 checksums, and whether tests were skipped.
- Added `scripts/start-release.ps1` to start verified release artifacts with the production profile and timestamped logs.
- Added `scripts/go-no-go.ps1` to orchestrate non-destructive technical Go/No-Go checks before demo or production decisions.
- Added `scripts/demo-data-audit.ps1` to detect placeholder-looking active records before customer-facing demos.
- Added `scripts/preflight-production.ps1` to validate production environment variables, tools, MySQL connectivity, and document storage permissions before deployment.
- Added `scripts/smoke-test.ps1` to verify production health and authenticated primary pages against a running environment.
- Added `scripts/backup-production.ps1` to create timestamped MySQL and `DOCUMENTS_DIR` backup sets with a manifest and database checksum.
- Added `scripts/verify-backup.ps1` to validate backup manifests, SQL dump integrity, checksum consistency, and document backup presence without restoring over live data.
- Added `scripts/restore-backup-to-testdb.ps1` to validate backup restorability in a protected test database instead of the production database.
- Created `docs/PRODUCTION_RUNBOOK.md` for deployment, backup, restore, rollback, shutdown, and incident handling.
- Linked the runbook from `docs/PRODUCTION_ENVIRONMENT.md`.
- Linked the runbook from `docs/DATABASE_OPERATIONS.md`.
- Verified all referenced production documentation files exist.
- Added `ProductionBootstrapInitializerIT` to verify a clean `prod` database creates only company, roles, and the first OWNER/ADMIN user.
- Verified the production bootstrap remains idempotent and does not create demo customers, projects, quotations, invoices, documents, or notifications.
- Added `ProductionBootstrapInitializerTest` to verify production startup fails fast when the initial admin credentials are missing or the bootstrap password is too weak.
- Added `ProductionSecurityIT` to verify production does not expose demo login credentials or the H2 console.
- Verified production login authenticates the bootstrap user and keeps the effective session cookie configuration secure with the `LSOTOTALBOUWSESSION` name.
- Verified the public production health endpoint returns status without exposing internal health contributor names, storage paths, or document directory details.
- Added `docs/SECURITY_DATA_PROTECTION.md` to define GDPR/AVG, data protection, document security, incident response, and production hardening requirements.
- Extended `ProductionSecurityIT` to verify production security headers and CSRF rejection for state-changing requests.
- Added `ProductionDocumentStorageValidator` to fail fast when production document storage is missing, invalid, or not writable.
- Added automated coverage for production document storage validation and verified the `prod` context still starts with a writable document directory.
- Added `DocumentStorageHealthIndicator` so `/actuator/health` can report document storage availability after startup.
- Added automated coverage for document storage health states and verified the public health endpoint remains available.
- Updated production operations documentation with health check interpretation and document storage incident steps.

## Demo Readiness

The following presentation assets are prepared:

- Created `docs/DEMO_GUIDE.md` with a professional 10-15 minute demo flow.
- Included a realistic demo data preparation checklist.
- Included talking points for dashboard, customers, projects, quotations, invoices, payments, documents, operations, audit, and production readiness.
- Created `docs/GO_NO_GO_CHECKLIST.md` for final demo/deployment decision control.
- Extended the `dev` profile seed with demo documents and unread business notifications.
- Kept production bootstrap free of demo records.
- Added automated coverage for the development seed to verify demo documents, notifications, and idempotent re-runs.

## Remaining Manual QA

- Perform one final human visual pass with realistic customer/project data before demo.
- Run `scripts/demo-data-audit.ps1` against the selected demo dataset and accept or clean any findings.
- Confirm generated PDFs and uploaded demo documents open correctly during the live walkthrough.

## Production Readiness Notes

- Keep `server.servlet.session.cookie.secure=true` in real production behind HTTPS.
- Rotate bootstrap admin password after the first login.
- Store MySQL and bootstrap secrets outside the repository.
- Back up MySQL and the document storage directory together.
- The application now includes local high-risk signature checks and a scanner integration point for documents.
- Connect that scanner integration point to a production antivirus or managed malware scanning provider before operating with real sensitive document uploads.
