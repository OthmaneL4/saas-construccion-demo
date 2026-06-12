# LSOTOTALBOUW - Production Runbook

## Purpose

This runbook defines the operational procedure to deploy, verify, back up, restore, and troubleshoot LSOTOTALBOUW in a production-like environment.

It complements:

- `docs/PROJECT_HANDOFF_SUMMARY.md`
- `docs/PRODUCTION_ENVIRONMENT.md`
- `docs/MYSQL_SETUP.md`
- `docs/DATABASE_OPERATIONS.md`
- `docs/FINAL_QA_CHECKLIST.md`
- `docs/GO_NO_GO_CHECKLIST.md`
- `docs/SECURITY_DATA_PROTECTION.md`

## Production Components

The production application depends on four persistent components:

- Application artifact built from the current Git revision.
- MySQL database `lsototalbouw`.
- External document storage configured through `DOCUMENTS_DIR`.
- Environment variables containing database, bootstrap, security, and runtime settings.

MySQL and `DOCUMENTS_DIR` must be backed up together because document metadata lives in MySQL while the physical files live on disk.

## Standard Deployment Procedure

1. Confirm the Git revision or release package to deploy.
2. Confirm the latest GitHub Actions CI run is green for that revision.
3. Run the production preflight:

```powershell
.\scripts\preflight-production.ps1
```

If local PowerShell execution policy blocks project scripts, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\preflight-production.ps1
```

For a limited local check without MySQL client tools, use `-SkipMySqlConnection -SkipMySqlToolsCheck`. Do not use those flags for a real production Go/No-Go decision.

4. Confirm no manual database changes are pending outside Flyway.
5. Create a verified pre-deployment backup:

```powershell
.\scripts\backup-production.ps1 -Label "predeploy"
```

The backup script creates a timestamped backup set containing:

- MySQL dump.
- `DOCUMENTS_DIR` copy.
- Manifest with database, document, checksum, and Git revision metadata.

6. Confirm the generated backup set exists under `backups/` and includes `manifest.json`.

7. Verify the backup package before continuing:

```powershell
.\scripts\verify-backup.ps1 -BackupSetPath "backups/YYYYMMDD_HHMMSS-predeploy" -RequireDocuments
```

If local PowerShell execution policy blocks project scripts, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-backup.ps1 -BackupSetPath "backups/YYYYMMDD_HHMMSS-predeploy" -RequireDocuments
```

Manual fallback for database dump:

```powershell
mysqldump --single-transaction --routines --triggers --set-gtid-purged=OFF -h $env:DB_HOST -u $env:DB_USERNAME -p lsototalbouw > backups/lsototalbouw_predeploy_YYYYMMDD_HHMM.sql
```

Manual fallback for documents:

```powershell
robocopy $env:DOCUMENTS_DIR backups/documents_YYYYMMDD_HHMM /E
```

8. Build the release artifact:

```powershell
.\scripts\build-release.ps1 -Label "release-candidate"
```

Confirm the generated release directory contains:

- `lsototalbouw-*.jar`
- `release-manifest.json`

Verify the release artifact before starting it:

```powershell
.\scripts\verify-release.ps1 -ReleasePath "releases/YYYYMMDD_HHMMSS-release-candidate" -RequireTests
```

9. Start with the production profile:

```powershell
.\scripts\start-release.ps1 -ReleasePath "releases/YYYYMMDD_HHMMSS-release-candidate" -RequireTests
```

The script validates the release package, enforces the `prod` profile, refuses `DB_USERNAME=root`, checks `DOCUMENTS_DIR`, and writes a timestamped log file under `logs/`.

10. Verify startup logs:

- Flyway migrations completed successfully.
- Hibernate schema validation passed.
- No stack traces during startup.
- Application started on the expected port.

11. Verify health:

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing
```

Expected result: HTTP `200` with status `UP`.

12. Run the technical Go/No-Go helper:

```powershell
.\scripts\go-no-go.ps1 -FullTests -ReleasePath "releases/YYYYMMDD_HHMMSS-release-candidate" -BaseUrl "http://localhost:8080" -RequireReleaseTests
```

## Health Check Interpretation

The `/actuator/health` endpoint is public for infrastructure monitoring, but production must keep detailed health data hidden from unauthenticated users.

Operational expectations:

- HTTP `200` and status `UP`: the application is reachable and all registered health contributors are healthy.
- HTTP non-`200` or status `DOWN`: remove the instance from traffic and investigate before accepting new user actions.

The application includes a document storage health contributor. It checks that `DOCUMENTS_DIR` is a real writable directory by creating and deleting a temporary probe file.

If document storage is unhealthy:

1. Stop new document uploads by taking the instance out of traffic.
2. Confirm the configured `DOCUMENTS_DIR` value.
3. Confirm the path exists and is a directory.
4. Confirm the Java process user has read, write, and delete permissions.
5. Confirm the disk or mounted volume is available and not full.
6. Compare the document directory with the latest backup if files appear missing.
7. Restart the application only after the storage path is healthy again.

Do not change document metadata in MySQL while the physical document directory is unavailable unless an incident lead has approved the recovery plan.

## Post-Deployment Smoke Test

Run the automated smoke test first:

```powershell
.\scripts\smoke-test.ps1 -BaseUrl "http://localhost:8080"
```

The script uses `APP_BOOTSTRAP_ADMIN_EMAIL` and `APP_BOOTSTRAP_ADMIN_PASSWORD` by default for authenticated route checks. You can also pass `-Username` and `-Password` explicitly for a controlled demo account.

Run these checks with an admin account:

- Login works.
- Dashboard loads.
- Customers list loads.
- Projects list loads.
- Invoices list loads.
- Payments list loads.
- Documents list loads.
- Audit list loads.
- Upload an allowed test PDF, then archive it.
- Create a small test payment against a test invoice, then archive it.

After smoke testing, confirm audit events exist for:

- Login.
- Document upload/archive.
- Payment create/archive.
- Company settings update if settings were changed.

## Graceful Shutdown

The production profile enables graceful shutdown:

```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=${SHUTDOWN_TIMEOUT:30s}
```

Preferred shutdown procedure:

1. Stop sending new traffic to the app in the proxy/load balancer.
2. Stop the Java process normally.
3. Wait for shutdown completion.
4. Confirm no Spring Boot Java process remains.

Avoid killing the process forcibly unless the application is stuck and a backup/recovery decision has been made.

## Rollback Procedure

Use rollback when a deployment causes startup failure, data corruption symptoms, or critical business flow failure.

1. Stop the application.
2. Restore the previous application artifact from the matching `releases/` directory and confirm its `release-manifest.json`.
3. If no new production data was written, restore MySQL from the pre-deployment backup.
4. Restore `DOCUMENTS_DIR` from the matching document backup.
5. Start the previous artifact with the same environment variables.
6. Verify `/actuator/health`.
7. Run the post-deployment smoke test.
8. Record the incident and root cause before attempting a new deployment.

Do not restore a database backup over newer real production data without explicitly accepting data loss.

Before approving a release or customer-facing demo, complete `docs/GO_NO_GO_CHECKLIST.md`.

## Restore Procedure

Use this for disaster recovery or controlled restore testing.

For a non-destructive restore test, restore the backup into a dedicated test database:

```powershell
.\scripts\restore-backup-to-testdb.ps1 -BackupSetPath "backups/YYYYMMDD_HHMMSS-predeploy"
```

This script refuses to restore into `lsototalbouw`. By default it restores into `lsototalbouw_restore_test` and copies documents into a temporary restore-test directory.

If local PowerShell execution policy blocks project scripts, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\restore-backup-to-testdb.ps1 -BackupSetPath "backups/YYYYMMDD_HHMMSS-predeploy"
```

1. Stop the application.
2. Create a safety copy of the current MySQL database and `DOCUMENTS_DIR`.
3. Restore MySQL:

```powershell
mysql -h $env:DB_HOST -u $env:DB_USERNAME -p lsototalbouw < backups/lsototalbouw_restore_point.sql
```

4. Restore the matching document directory:

```powershell
robocopy backups/documents_restore_point $env:DOCUMENTS_DIR /MIR
```

5. Start the application.
6. Verify Flyway history:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

7. Verify login, dashboard, invoices, documents, and audit.

## Incident Checklist

When production has an issue, collect these facts before changing data:

- Exact time and timezone.
- Affected user and role.
- Affected module.
- URL or action attempted.
- Error shown in UI.
- Relevant application logs.
- MySQL availability.
- Disk space for `DOCUMENTS_DIR`.
- Latest Flyway version in `flyway_schema_history`.
- Whether recent deployments or migrations occurred.

## Operational Rules

- Never run the application with MySQL `root`.
- Never store real passwords in Git.
- Never edit an already-applied Flyway migration.
- Always add new schema changes as a new `V__` migration.
- Always back up MySQL and `DOCUMENTS_DIR` together.
- Keep `server.servlet.session.cookie.secure=true` in real HTTPS production.
- Rotate the bootstrap admin password after first login.
- Test restore at least monthly.
- Review `docs/SECURITY_DATA_PROTECTION.md` before using real personal, financial, or document data.
