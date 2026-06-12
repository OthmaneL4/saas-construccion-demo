# LSOTOTALBOUW - plantilla de variables de entorno para perfil prod
# Cambiar los valores entre <...> antes de usar.
# No guardar este archivo con contrasenas reales.

$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:mysql://localhost:3306/lsototalbouw?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=false"
$env:DB_USERNAME = "<APP_DB_USER>"
$env:DB_PASSWORD = "<APP_DB_PASSWORD>"
$env:DOCUMENTS_DIR = "C:/lsototalbouw/documents"

$env:APP_BOOTSTRAP_COMPANY_NAME = "LSOTOTALBOUW"
$env:APP_BOOTSTRAP_COMPANY_EMAIL = "info@lsototalbouw.nl"
$env:APP_BOOTSTRAP_ADMIN_FULL_NAME = "LSOTOTALBOUW Owner"
$env:APP_BOOTSTRAP_ADMIN_EMAIL = "admin@lsototalbouw.nl"
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = "<STRONG_INITIAL_ADMIN_PASSWORD>"

$env:PORT = "8080"
$env:SESSION_TIMEOUT = "30m"
$env:LOGIN_MAX_FAILED_ATTEMPTS = "5"
$env:LOGIN_LOCK_MINUTES = "15"
$env:DB_POOL_MAX_SIZE = "10"
$env:DB_POOL_MIN_IDLE = "2"
$env:SHUTDOWN_TIMEOUT = "30s"

Write-Host "Variables prod preparadas para LSOTOTALBOUW. Revisa que no haya secretos reales guardados en el repositorio."
