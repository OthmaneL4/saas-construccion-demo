-- LSOTOTALBOUW - plantilla segura para crear la base MySQL
-- Ejecutar como usuario administrador de MySQL.
-- Cambiar los valores entre <...> antes de ejecutar.
-- No guardar contrasenas reales en este archivo.

CREATE DATABASE IF NOT EXISTS lsototalbouw
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS '<APP_DB_USER>'@'<APP_DB_HOST>' IDENTIFIED BY '<APP_DB_PASSWORD>';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON lsototalbouw.*
    TO '<APP_DB_USER>'@'<APP_DB_HOST>';

FLUSH PRIVILEGES;

-- Verificacion rapida:
-- SHOW GRANTS FOR '<APP_DB_USER>'@'<APP_DB_HOST>';
-- SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME
-- FROM information_schema.SCHEMATA
-- WHERE SCHEMA_NAME = 'lsototalbouw';
