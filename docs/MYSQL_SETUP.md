# LSOTOTALBOUW - Creacion de MySQL

## Objetivo

Preparar la base de datos final de LSOTOTALBOUW sin exponer secretos y dejando que Flyway cree el esquema de aplicacion.

## Decision previa

Antes de ejecutar nada, confirmar:

- Host de MySQL.
- Puerto de MySQL.
- Nombre de base: recomendado `lsototalbouw`.
- Usuario de aplicacion.
- Contrasena fuerte.
- Ruta persistente para documentos (`DOCUMENTS_DIR`).
- Si el despliegue sera local, VPS, Docker o servidor gestionado.

## Crear base y usuario

Usar la plantilla:

```text
docs/mysql/CREATE_DATABASE_TEMPLATE.sql
```

Cambiar:

- `<APP_DB_USER>` por el usuario real.
- `<APP_DB_HOST>` por el origen permitido. Para local puede ser `localhost`; para servidor puede ser una IP concreta.
- `<APP_DB_PASSWORD>` por una contrasena fuerte.

Ejecutar como administrador MySQL:

```powershell
mysql -u root -p < docs/mysql/CREATE_DATABASE_TEMPLATE.sql
```

En este equipo el cliente tambien puede ejecutarse con ruta absoluta si no esta en el PATH:

```powershell
& "C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -u root -p < docs/mysql/CREATE_DATABASE_TEMPLATE.sql
```

No guardar la plantilla con contrasenas reales despues de ejecutarla.

## Variables para arrancar la aplicacion

Ejemplo local:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DB_URL="jdbc:mysql://localhost:3306/lsototalbouw?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=false"
$env:DB_USERNAME="<APP_DB_USER>"
$env:DB_PASSWORD="<APP_DB_PASSWORD>"
$env:DOCUMENTS_DIR="C:/lsototalbouw/documents"
$env:APP_BOOTSTRAP_ADMIN_EMAIL="admin@lsototalbouw.nl"
$env:APP_BOOTSTRAP_ADMIN_PASSWORD="<STRONG_INITIAL_ADMIN_PASSWORD>"
```

Tambien existe una plantilla PowerShell:

```text
docs/mysql/SET_PROD_ENV_TEMPLATE.ps1
```

Arranque:

```powershell
.\mvnw.cmd -DskipTests spring-boot:run
```

## Que debe pasar en el primer arranque

- Flyway detecta una base vacia.
- Flyway aplica las migraciones `V1` a `V9`.
- Hibernate valida el esquema con `spring.jpa.hibernate.ddl-auto=validate`.
- Si la tabla `users` esta vacia, el bootstrap de produccion crea los roles base, la cuenta `LSOTOTALBOUW` y el primer usuario OWNER/ADMIN usando variables de entorno.
- La aplicacion arranca en el puerto configurado.
- `/login` responde correctamente.

El bootstrap de produccion no usa credenciales demo. Si `users` esta vacia y faltan
`APP_BOOTSTRAP_ADMIN_EMAIL` o `APP_BOOTSTRAP_ADMIN_PASSWORD`, el arranque falla a proposito.

## Verificaciones despues del primer arranque

En MySQL:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

En la aplicacion:

- Login.
- Dashboard.
- Clientes.
- Facturas.
- Pagos.
- Documentos.
- Auditoria.

Comprobacion rapida del bootstrap:

```sql
SELECT COUNT(*) AS companies_count FROM company_accounts;
SELECT COUNT(*) AS users_count FROM users;
SELECT COUNT(*) AS roles_count FROM roles;
SELECT email, full_name FROM users;
```

## Seguridad

- No usar usuario `root` para la aplicacion.
- No guardar `DB_PASSWORD` en el repositorio.
- Usar un host permitido lo mas restrictivo posible.
- Activar HTTPS en el proxy o servidor frontal.
- Hacer backup de MySQL y `DOCUMENTS_DIR` juntos.

## Rollback inicial

Si el primer arranque falla antes de usar datos reales:

1. Parar la aplicacion.
2. Revisar el error de Flyway.
3. Corregir la migracion o configuracion.
4. Si la base no contiene datos reales, borrar y recrear la base.
5. Arrancar de nuevo.

No borrar una base que ya contenga datos reales sin backup verificado.
