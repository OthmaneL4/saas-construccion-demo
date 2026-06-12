# LSOTOTALBOUW - Entrega pre-MySQL

## Objetivo

Este documento marca el punto de parada antes de crear la base de datos final en MySQL. Hasta aqui el proyecto debe quedar estable, probado y preparado para que el siguiente paso sea configurar MySQL con el perfil `prod`.

## Estado antes de MySQL

Completado:

- Aplicacion Spring Boot MVC con Thymeleaf y Bootstrap.
- Seguridad con Spring Security, CSRF, roles, bloqueo por intentos fallidos y auditoria de accesos.
- Modulos principales: dashboard, clientes, proyectos, presupuestos, facturas, pagos, gastos, materiales, herramientas, proveedores, calendario, documentos, partes de horas, rentabilidad, cobros pendientes, usuarios, notificaciones y auditoria.
- Flyway activo con migraciones `V1` a `V9`.
- Perfil `dev` funcionando con H2 en modo MySQL.
- Perfil `prod` preparado para MySQL mediante variables de entorno.
- Documentos subidos fuera de la base de datos mediante `DOCUMENTS_DIR`.
- Tests automatizados de arranque, seguridad, permisos, aislamiento multiempresa y alertas operativas.

## Verificaciones realizadas

Ultima revision local:

- `.\mvnw.cmd -q test`
- `.\mvnw.cmd -q "-Dtest=MultiCompanyIsolationIT,OperationalAlertServiceIT" test`

Resultado esperado:

- `LsoTotalbouwApplicationTests`: 1 test correcto.
- `SecurityAuthorizationIT`: 60 tests correctos.
- `MultiCompanyIsolationIT`: correcto.
- `OperationalAlertServiceIT`: correcto.

## Lo que queda bloqueado hasta que entre el jefe

No crear todavia la base final en MySQL desde codigo. El siguiente paso requiere decision operativa:

- Nombre definitivo de la base de datos.
- Usuario MySQL y permisos.
- Host/puerto MySQL.
- Politica de backup.
- Ruta persistente real para `DOCUMENTS_DIR`.
- Confirmacion de entorno: local, VPS, Docker, hosting gestionado o servidor de empresa.

## Variables necesarias para el siguiente paso

Cuando se cree MySQL, el perfil `prod` necesitara:

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://HOST:3306/lsototalbouw?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=false
DB_USERNAME=usuario
DB_PASSWORD=contrasena
DOCUMENTS_DIR=C:/ruta/persistente/documentos
```

Opcionales recomendadas:

```text
PORT=8080
SESSION_TIMEOUT=30m
LOGIN_MAX_FAILED_ATTEMPTS=5
LOGIN_LOCK_MINUTES=15
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=2
SHUTDOWN_TIMEOUT=30s
```

## Checklist justo antes de crear MySQL

- Confirmar que no hay contrasenas reales en el repositorio.
- Confirmar que `docs/PRODUCTION_ENVIRONMENT.md` esta revisado.
- Confirmar que `docs/DATABASE_OPERATIONS.md` esta revisado.
- Confirmar que `docs/MYSQL_SETUP.md` esta revisado.
- Confirmar que todos los tests pasan.
- Parar cualquier servidor local que use `data/lsototalbouw.mv.db`.
- Guardar copia de seguridad de la base H2 local si contiene datos utiles.
- Decidir si los datos actuales son demo, descartables o deben migrarse manualmente.

## Checklist despues de crear MySQL

Cuando el jefe confirme MySQL:

1. Crear base vacia con charset `utf8mb4`.
2. Crear usuario de aplicacion con permisos limitados a esa base.
3. Configurar variables de entorno.
4. Arrancar con `SPRING_PROFILES_ACTIVE=prod`.
5. Verificar que Flyway aplica las migraciones.
6. Confirmar que Hibernate valida el esquema sin modificarlo.
7. Probar login, dashboard, facturas, pagos, documentos y auditoria.
8. Configurar backup de MySQL y `DOCUMENTS_DIR`.

Guia operativa: `docs/MYSQL_SETUP.md`.

## Decision de parada

La aplicacion queda preparada hasta el punto pre-MySQL. El siguiente avance tecnico no debe crear ni modificar la base final sin confirmacion del jefe.
