# LSOTOTALBOUW - Entorno de produccion

## Objetivo

Este documento define la configuracion minima para ejecutar LSOTOTALBOUW con perfil `prod`, MySQL, Flyway y almacenamiento externo de documentos.

Para el procedimiento operativo completo de despliegue, backup, restauracion, rollback e incidencias, revisar tambien `docs/PRODUCTION_RUNBOOK.md`.

## Variables obligatorias

| Variable | Uso |
| --- | --- |
| `SPRING_PROFILES_ACTIVE=prod` | Activa la configuracion de produccion. |
| `DB_URL` | URL JDBC de MySQL. |
| `DB_USERNAME` | Usuario de base de datos. |
| `DB_PASSWORD` | Contrasena de base de datos. |
| `DOCUMENTS_DIR` | Carpeta persistente para documentos y fotos subidas. |

Ejemplo de `DB_URL`:

```text
jdbc:mysql://localhost:3306/lsototalbouw?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=false
```

## Variables recomendadas

| Variable | Valor por defecto | Uso |
| --- | --- | --- |
| `PORT` | `8080` | Puerto HTTP de la aplicacion. |
| `SESSION_TIMEOUT` | `30m` | Tiempo maximo de sesion inactiva. |
| `DOCUMENTS_MAX_SIZE_BYTES` | `10485760` | Tamano maximo por documento. |
| `LOGIN_MAX_FAILED_ATTEMPTS` | `5` | Intentos fallidos antes de bloqueo. |
| `LOGIN_LOCK_MINUTES` | `15` | Duracion del bloqueo de login. |
| `DB_POOL_MAX_SIZE` | `10` | Maximo de conexiones MySQL. |
| `DB_POOL_MIN_IDLE` | `2` | Conexiones inactivas minimas. |
| `SHUTDOWN_TIMEOUT` | `30s` | Tiempo para apagado ordenado. |

## Checklist antes de presentar o desplegar

- Ejecutar preflight de entorno con `.\scripts\preflight-production.ps1`.
- Ejecutar pruebas completas con `.\mvnw.cmd -q clean test`.
- Ejecutar integraciones restantes con `.\mvnw.cmd -q "-Dtest=MultiCompanyIsolationIT,OperationalAlertServiceIT" test`.
- Arrancar con perfil `dev` y verificar login, dashboard, facturas, pagos, documentos y notificaciones.
- Revisar `docs/PRE_MYSQL_HANDOFF.md` antes de crear la base final.
- Revisar `docs/MYSQL_SETUP.md` y preparar la plantilla `docs/mysql/CREATE_DATABASE_TEMPLATE.sql`.
- Preparar una base MySQL vacia y ejecutar Flyway con perfil `prod`.
- Confirmar que `spring.jpa.hibernate.ddl-auto=validate` no intenta modificar el esquema.
- Confirmar que `/actuator/health` responde sin exponer detalles internos.
- Verificar que `DOCUMENTS_DIR` existe, no es publico, tiene backup y aparece sano en el health check de almacenamiento de documentos.
- Configurar HTTPS en proxy o servidor frontal.
- Guardar secretos fuera del repositorio.
- Hacer backup de MySQL y documentos antes de cada despliegue.
- Ejecutar el procedimiento de `docs/PRODUCTION_RUNBOOK.md` para despliegues y rollback.

## Decisiones de seguridad

- La cookie de sesion se llama `LSOTOTALBOUWSESSION`, es `HttpOnly`, `SameSite=Strict` y en produccion se fuerza como segura.
- El perfil `prod` exige `DOCUMENTS_DIR` para evitar guardar archivos importantes en una carpeta local accidental.
- El endpoint de salud solo expone estado general y debe monitorizar tambien la disponibilidad de la carpeta de documentos.
- Hibernate valida el esquema, pero las modificaciones se hacen mediante Flyway.
- El apagado ordenado reduce riesgo de cortar peticiones o escrituras en curso durante despliegues.
