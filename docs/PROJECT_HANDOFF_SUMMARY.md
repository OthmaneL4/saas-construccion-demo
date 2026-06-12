# LSOTOTALBOUW - Project Handoff Summary

## Executive Status

LSOTOTALBOUW is in a release-candidate stage for a professional demo.

The application has a locally verified clean release candidate and is suitable for a controlled professional demo with the prepared local dataset. It is not yet ready for unattended real production with sensitive customer data until hosting, HTTPS, scheduled backups, monitoring, and final GDPR/AVG procedures are confirmed.

## Current Readiness

| Area | Status | Notes |
| --- | --- | --- |
| Core business workflows | Ready for final QA | Customers, projects, quotations, invoices, payments, receivables, expenses, documents, calendar, materials, tools, suppliers, users, audit, and dashboard are implemented. |
| Security baseline | Ready for final QA | Spring Security, roles, CSRF, secure cookie settings, login lockout, security headers, and audit events are implemented. |
| Production database path | Ready for controlled validation | MySQL/Flyway production profile is prepared. |
| Documents | Ready for controlled validation | External document storage through `DOCUMENTS_DIR`, startup validation, health check, upload validation, local security scanning, SHA-256 integrity checks, and audit trail are implemented. |
| Automated tests | Ready for CI validation | Production, security, navigation, document health, bootstrap, and demo seed tests are available. |
| Operations tooling | Ready for final QA | Preflight, backup, verify backup, restore test, build release, verify release, start release, smoke test, and Go/No-Go scripts are available. |
| Demo readiness | Ready for controlled demo | Demo data audit, authenticated smoke test, and visual walkthrough have passed locally. |
| Real production readiness | Pending infrastructure | Requires hosting, HTTPS, scheduled backups, monitoring, secrets handling, and final GDPR/AVG decisions. |

## Latest Verified Release Candidate

The latest clean release candidate was built and verified on 2026-06-12:

| Item | Value |
| --- | --- |
| Release path | `releases/20260612_125712-clean-release-candidate` |
| Git revision | `f49b4830d591e638dc8c009ab0118157c83597df` |
| Jar | `lsototalbouw-0.0.1-SNAPSHOT.jar` |
| Jar SHA-256 | `B743C83BBC404AC3EF5F6F3F83DA55B818176838622704DFBD84F451BC7C1B39` |
| Tests skipped | `false` |
| Git dirty | `false` |

Validation completed:

- Maven `clean package` completed with tests enabled.
- `verify-release.ps1` passed with `-RequireTests`.
- The release jar started successfully on a local port.
- `smoke-test.ps1` passed for health, login, dashboard, core business modules, documents, audit, users, settings, and notifications.
- Visual walkthrough passed for authenticated desktop and mobile Dashboard/Auditor checks, including sidebar scrolling and audit filter layout.

## Main Validation Flow

Use this sequence before an important demo or production-like presentation:

```powershell
.\mvnw.cmd -q clean test
.\scripts\preflight-production.ps1
.\scripts\backup-production.ps1 -Label "predeploy"
.\scripts\verify-backup.ps1 -BackupSetPath "backups/YYYYMMDD_HHMMSS-predeploy" -RequireDocuments
.\scripts\restore-backup-to-testdb.ps1 -BackupSetPath "backups/YYYYMMDD_HHMMSS-predeploy"
.\scripts\build-release.ps1 -Label "release-candidate"
.\scripts\verify-release.ps1 -ReleasePath "releases/YYYYMMDD_HHMMSS-release-candidate" -RequireTests
.\scripts\start-release.ps1 -ReleasePath "releases/YYYYMMDD_HHMMSS-release-candidate" -RequireTests
.\scripts\smoke-test.ps1 -BaseUrl "http://localhost:8080"
.\scripts\go-no-go.ps1 -FullTests -ReleasePath "releases/YYYYMMDD_HHMMSS-release-candidate" -BaseUrl "http://localhost:8080" -RequireReleaseTests
```

If PowerShell blocks script execution, run scripts through:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\SCRIPT_NAME.ps1
```

## Demo Acceptance Criteria

The project can be shown as a professional demo when:

- CI is green or local full tests pass.
- The selected release verifies successfully with `-RequireTests`.
- The application starts with the intended profile and database.
- Smoke test passes.
- Demo data audit passes or all findings are intentionally accepted.
- A realistic demo dataset is present.
- The sidebar and main screens are visually reviewed.
- No private passwords, database credentials, or real customer data are visible.
- The business flow from quotation to invoice to payment can be demonstrated.
- Documents open/download through the application.
- Audit entries are visible for sensitive actions.

## Production Acceptance Criteria

The project can move toward real production only when:

- HTTPS is configured and verified.
- `server.servlet.session.cookie.secure=true` remains enabled.
- MySQL runs with a dedicated application user, not `root`.
- Secrets are stored outside Git and outside shared documentation.
- Backups are scheduled and retention is defined.
- A restore test has been completed successfully.
- `DOCUMENTS_DIR` is persistent, private, backed up, and monitored.
- Health checks are monitored externally.
- Logs are collected and reviewed.
- GDPR/AVG retention, export, deletion, and incident procedures are approved.
- A rollback artifact and matching database/document backup are available.

## Important Documents

- `docs/GO_NO_GO_CHECKLIST.md`: final decision checklist.
- `docs/FINAL_QA_CHECKLIST.md`: technical QA history and coverage.
- `docs/DEMO_GUIDE.md`: presentation flow and demo data.
- `docs/PRODUCTION_RUNBOOK.md`: deploy, backup, restore, rollback, and incident operations.
- `docs/PRODUCTION_ENVIRONMENT.md`: required production variables.
- `docs/DATABASE_OPERATIONS.md`: Flyway, MySQL, backup, and restore operations.
- `docs/SECURITY_DATA_PROTECTION.md`: security hardening and GDPR/AVG requirements.
- `docs/MYSQL_SETUP.md`: MySQL setup guidance.
- `docs/PORTFOLIO_DEMO.md`: public portfolio demo scope, credentials, deployment options, and LinkedIn/GitHub publishing plan.

## Current Open Items

- Execute the full Go/No-Go flow against the intended MySQL environment.
- Review all screens manually with realistic data.
- Confirm final demo dataset.
- Publish the controlled portfolio demo if the goal is recruiter/company visibility before a real customer launch.
- Decide final hosting model for real production.
- Configure HTTPS and domain.
- Configure scheduled backups and restore testing.
- Decide monitoring and alerting approach.
- Review GDPR/AVG procedures with the business owner.

## Handoff Decision

Recommended next decision:

- For demo: proceed after realistic demo data review and final visual walkthrough.
- For production: pause after demo and complete infrastructure/security operations before entering real sensitive data.
