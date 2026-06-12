# LSOTOTALBOUW - Go/No-Go Checklist

## Purpose

This checklist defines the final decision gate before a customer-facing demo, production-like presentation, or controlled production deployment.

Use it to decide whether the application is ready to show or release. If any blocking item fails, the decision is `NO-GO` until the issue is fixed or explicitly accepted by the project owner.

For the current delivery status and handoff summary, review `docs/PROJECT_HANDOFF_SUMMARY.md`.

## Decision Roles

- Project Owner: approves the final go/no-go decision.
- Technical Lead: confirms build, tests, startup, security, and operational readiness.
- Demo Lead: confirms the presentation flow and demo data.
- Operations Lead: confirms database, documents, backups, and rollback readiness.

## Blocking Go/No-Go Criteria

The decision is `NO-GO` if any of these are true:

- The application does not compile.
- Required production environment variables are missing.
- Flyway migrations fail.
- Hibernate schema validation fails in `prod`.
- Login fails for the intended admin/demo user.
- `/actuator/health` is not `UP`.
- `DOCUMENTS_DIR` is missing, not writable, or not backed up.
- Any primary page returns a Spring error page.
- Document upload/download fails.
- Quotation-to-invoice conversion fails.
- Payment registration or confirmation fails.
- Audit logging for sensitive actions is missing.
- Real passwords, database credentials, or private customer data would be visible during the demo.
- There is no rollback or restore plan for the target environment.

## Required Commands

Run before a release candidate or important presentation:

```powershell
.\mvnw.cmd -q clean test
.\scripts\build-release.ps1 -Label "release-candidate"
.\scripts\verify-release.ps1 -ReleasePath "releases/YYYYMMDD_HHMMSS-release-candidate" -RequireTests
.\scripts\start-release.ps1 -ReleasePath "releases/YYYYMMDD_HHMMSS-release-candidate" -RequireTests
```

After the application is running, execute the technical Go/No-Go helper:

```powershell
.\scripts\go-no-go.ps1 -FullTests -ReleasePath "releases/YYYYMMDD_HHMMSS-release-candidate" -BaseUrl "http://localhost:8080" -RequireReleaseTests
```

For a limited local check that only compiles and verifies a release package:

```powershell
.\scripts\go-no-go.ps1 -ReleasePath "releases/YYYYMMDD_HHMMSS-release-candidate" -SkipPreflight -SkipSmokeTest
```

If time is limited, run at minimum:

```powershell
.\mvnw.cmd -q -DskipTests compile
.\mvnw.cmd -q "-Dtest=ProductionBootstrapInitializerIT,ProductionSecurityIT,AuthenticatedNavigationIT,DataInitializerIT" test
```

Expected result:

- Build exits with code `0`.
- No compilation errors.
- No test failures.
- A release directory exists under `releases/`.
- `release-manifest.json` exists and includes a SHA-256 checksum for the jar.
- Release verification passes with `-RequireTests`.
- The selected release can be started through `scripts/start-release.ps1`.

## Continuous Integration Gate

The repository includes a GitHub Actions workflow at `.github/workflows/ci.yml`.

Before a customer-facing demo or deployment decision:

- Confirm the latest CI run is green for the target branch or pull request.
- Confirm the CI run executed both compile and test steps.
- Treat a failed or missing CI run as `NO-GO` unless the project owner explicitly accepts the risk.

## Environment Verification

Confirm these values before a production-like run:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DOCUMENTS_DIR`
- `APP_BOOTSTRAP_ADMIN_EMAIL`
- `APP_BOOTSTRAP_ADMIN_PASSWORD`

Confirm these rules:

- `.\scripts\preflight-production.ps1` passes for the target environment.
- `DB_USERNAME` is not `root`.
- Secrets are not stored in Git.
- `DOCUMENTS_DIR` is outside the application artifact directory.
- `DOCUMENTS_DIR` is included in the backup plan.
- HTTPS is configured at the proxy/front server for real production.
- `docs/SECURITY_DATA_PROTECTION.md` has been reviewed for the target environment.

## Technical Smoke Test

For a repeatable technical check, run the smoke-test script against the running application:

```powershell
.\scripts\smoke-test.ps1 -BaseUrl "http://localhost:8080"
```

If only the public health endpoint must be checked:

```powershell
.\scripts\smoke-test.ps1 -BaseUrl "http://localhost:8080" -SkipAuthenticatedRoutes
```

Verify these pages with an authenticated admin user:

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

Expected result:

- Every page loads without a Spring error page.
- Sidebar navigation is usable.
- Tables remain readable with realistic data.
- No broken static assets.

## Business Flow Smoke Test

Complete one short business flow:

1. Open an existing customer.
2. Open a linked project.
3. Open a quotation.
4. Generate or preview the quotation PDF.
5. Convert the quotation to an invoice, or open an already converted invoice if duplicate conversion protection is being demonstrated.
6. Register or review a payment.
7. Open receivables.
8. Open a linked document.
9. Open audit logs and confirm sensitive actions are traceable.

Expected result:

- Totals are understandable.
- Payment status is clear.
- Document links are available.
- Audit entries exist for sensitive actions.

## Demo Readiness

Before presenting:

- Use realistic demo data.
- Avoid records named `test`, `QA`, `dummy`, or `error`.
- Run `.\scripts\demo-data-audit.ps1` and resolve findings before presenting.
- Confirm generated PDFs open.
- Confirm notifications show useful examples.
- Confirm browser zoom is 100%.
- Confirm no private secrets are visible in browser tabs, terminal output, documentation, or environment scripts.
- Confirm the demo path fits the available meeting time.

## Operational Readiness

Before deployment:

- Run `.\scripts\backup-production.ps1 -Label "predeploy"` or confirm an equivalent verified backup set exists.
- Run `.\scripts\verify-backup.ps1 -BackupSetPath "backups/YYYYMMDD_HHMMSS-predeploy" -RequireDocuments`.
- Confirm latest MySQL backup exists.
- Confirm matching `DOCUMENTS_DIR` backup exists.
- Confirm backup `manifest.json` exists and references the expected database and document backup.
- Confirm restore procedure is documented.
- Confirm a non-destructive restore test has been executed with `.\scripts\restore-backup-to-testdb.ps1` for the selected backup.
- Confirm rollback artifact is available.
- Confirm previous release artifact and `release-manifest.json` are available for rollback.
- Confirm `/actuator/health` returns `UP`.
- Confirm logs do not contain startup exceptions.
- Confirm document storage health is operational.

## Final Decision

Use this table for the final decision:

| Area | Status | Owner | Notes |
| --- | --- | --- | --- |
| Build and tests | PASS locally | Technical Lead | Clean release candidate built with tests enabled: `releases/20260612_125712-clean-release-candidate`. |
| Continuous integration | Pending external confirmation | Technical Lead | Confirm latest CI run before external handoff. |
| Database and migrations | PASS locally | Technical Lead | Flyway validated 10 migrations during release build and smoke validation. |
| Security and login | PASS locally | Technical Lead | Smoke test login succeeded with the development admin user; production credentials still require environment confirmation. |
| Data protection and GDPR/AVG | Pending owner review | Project Owner | Procedures must be approved before real sensitive data is entered. |
| Documents and storage | PASS locally | Technical Lead | Document storage health is exposed through `/actuator/health`; checksum and local security scanning are implemented. |
| Core business flows | PASS smoke | Technical Lead | Primary modules loaded without Spring error pages in authenticated smoke test. |
| Demo data | PASS locally | Demo Lead | Demo data audit, authenticated smoke test, and visual walkthrough passed locally. |
| Backup and rollback | Pending target environment | Operations Lead | Scripts and runbook are prepared; execute against the selected MySQL/DOCUMENTS_DIR environment. |

Final decision:

- `GO`: all blocking criteria pass in the target environment.
- `NO-GO`: at least one blocking criterion fails.
- `GO WITH ACCEPTED RISK`: only non-blocking issues remain and the project owner explicitly accepts them.

Current technical decision for 2026-06-12:

- `GO FOR PROFESSIONAL DEMO PREPARATION`.
- `NO-GO FOR UNATTENDED REAL PRODUCTION` until HTTPS, hosting, scheduled backups, monitoring, secrets rotation, GDPR/AVG procedures, and target-environment restore testing are confirmed.
