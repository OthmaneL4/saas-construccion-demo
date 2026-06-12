# LSOTOTALBOUW - Operacion de base de datos

## Objetivo

La base de datos queda versionada con Flyway para que cada cambio de esquema sea trazable, revisable y repetible en desarrollo, pruebas y produccion.

Para ejecutar backups, restauraciones y rollbacks de forma operativa, usar este documento junto con `docs/PRODUCTION_RUNBOOK.md`.

## Migraciones

- Las migraciones viven en `src/main/resources/db/migration`.
- La migracion inicial es `V1__initial_schema.sql`.
- `V2__login_attempt_lockout.sql` anade control de intentos fallidos de login y bloqueo temporal de usuarios.
- `V3__audit_action_as_varchar.sql` normaliza `audit_logs.action` como texto para permitir nuevas acciones de auditoria.
- Las migraciones posteriores amplian documentos, pagos, facturas, notificaciones, auditoria y datos operativos.
- No se debe editar una migracion ya aplicada en produccion. Para nuevos cambios se crea una nueva version: `V10__descripcion.sql`, `V11__descripcion.sql`, etc.
- En produccion `spring.jpa.hibernate.ddl-auto=validate`: Hibernate valida el esquema, pero no lo cambia automaticamente.
- En desarrollo se mantiene H2 en modo MySQL y `spring.flyway.baseline-on-migrate=true` para adoptar bases locales ya existentes sin bloquear el arranque.

## Politica de backups MySQL

Frecuencia recomendada:

- Diario: conservar 14 copias.
- Semanal: conservar 8 copias.
- Mensual: conservar 12 copias.
- Antes de cada despliegue: backup manual verificado.

Comando base:

```powershell
.\scripts\backup-production.ps1 -Label "manual"
```

El script crea un paquete con:

- Dump MySQL.
- Copia de `DOCUMENTS_DIR`.
- `manifest.json` con checksum SHA-256 del dump, origen de documentos y revision Git si esta disponible.

Verificacion del backup:

```powershell
.\scripts\verify-backup.ps1 -BackupSetPath "backups/YYYYMMDD_HHMMSS-manual" -RequireDocuments
```

Prueba de restauracion no destructiva:

```powershell
.\scripts\restore-backup-to-testdb.ps1 -BackupSetPath "backups/YYYYMMDD_HHMMSS-manual"
```

El script restaura por defecto en `lsototalbouw_restore_test` y se niega a restaurar sobre `lsototalbouw`.

Comando manual de respaldo si el script no esta disponible:

```powershell
mysqldump --single-transaction --routines --triggers --set-gtid-purged=OFF -h $env:DB_HOST -u $env:DB_USERNAME -p lsototalbouw > lsototalbouw_backup.sql
```

Restauracion:

```powershell
mysql -h $env:DB_HOST -u $env:DB_USERNAME -p lsototalbouw < lsototalbouw_backup.sql
```

## Documentos y adjuntos

Los documentos subidos no viven dentro de MySQL; se guardan en `app.upload.documents-dir`. El backup completo debe incluir:

- Dump MySQL.
- Carpeta de documentos configurada en `DOCUMENTS_DIR`.
- Version exacta de la aplicacion desplegada.
- Variables de entorno no secretas necesarias para reproducir el entorno.

En produccion, la carpeta `DOCUMENTS_DIR` se valida al arrancar y se monitoriza desde `/actuator/health`. Si el health check de documentos cae, tratarlo como una incidencia operativa porque MySQL puede seguir funcionando aunque los archivos fisicos no esten disponibles.

## Desarrollo local H2

Para desarrollo, la base local se guarda en `data/lsototalbouw.mv.db`. Antes de hacer pruebas destructivas:

1. Parar la aplicacion.
2. Guardar una copia de `data/lsototalbouw.mv.db`.
3. Ejecutar las pruebas o migraciones.
4. Restaurar la copia si hace falta.

## Verificaciones obligatorias

- Probar una restauracion completa al menos una vez al mes.
- Validar que `flyway_schema_history` contiene las migraciones esperadas.
- Confirmar que el backup de documentos corresponde al mismo momento que el dump de MySQL.
- Revisar permisos de lectura/escritura de la carpeta de documentos.
- Verificar que el health check de documentos esta `UP` despues de restauraciones o cambios de volumen.
- No guardar contrasenas de base de datos dentro del repositorio.
- Revisar `docs/PRODUCTION_ENVIRONMENT.md` antes de activar el perfil `prod`.
- Revisar `docs/PRODUCTION_RUNBOOK.md` antes de cada despliegue real.

## Recuperacion recomendada

1. Detener la aplicacion.
2. Restaurar MySQL desde el dump elegido.
3. Restaurar la carpeta de documentos del mismo punto temporal.
4. Arrancar la aplicacion con el mismo perfil.
5. Verificar login, dashboard, clientes, facturas, documentos y auditoria.
6. Revisar logs de arranque y `flyway_schema_history`.
