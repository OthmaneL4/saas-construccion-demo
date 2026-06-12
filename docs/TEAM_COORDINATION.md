# LSOTOTALBOUW - Coordinacion del equipo senior

## Direccion del proyecto

El proyecto se desarrolla como un monolito modular Spring Boot MVC preparado para crecer. La prioridad inmediata es entregar una aplicacion funcional, segura, navegable y facil de ampliar para LSOTOTALBOUW.

## Agente 1 - Backend Architect

Responsable de arquitectura Java, entidades, servicios, controladores y reglas de negocio.

Decisiones activas:
- Monolito modular por dominio.
- Todas las entidades de negocio deben poder relacionarse con `CompanyAccount`.
- Controladores MVC finos, logica en servicios.
- Repositorios Spring Data JPA.
- Validaciones backend con Bean Validation.

Trabajo siguiente:
- Mantener servicios transaccionales por modulo y seguir ampliando cobertura automatizada por flujo critico.
- Preparar la conexion final a MySQL cuando el jefe confirme entorno, credenciales y politica de backup.

## Agente 2 - Frontend/UI Engineer

Responsable de Thymeleaf, Bootstrap, experiencia de usuario y diseño responsive.

Decisiones activas:
- Dashboard tipo SaaS, claro y operativo.
- Sidebar profesional para escritorio.
- Vistas de tablas limpias para gestion diaria.
- Acciones rapidas desde dashboard.

Trabajo siguiente:
- Pulir detalles visuales menores tras pruebas reales de usuario.
- Mantener filtros, busqueda, acciones y retorno contextual en pantallas financieras y operativas.
- Revisar responsive final con datos reales antes de presentacion.

## Agente 3 - Cybersecurity & Security Engineer

Responsable de Spring Security, hardening, autenticacion y proteccion de datos.

Decisiones activas:
- Login propio con Spring Security.
- BCrypt para contrasenas.
- CSRF activo.
- Cabeceras de seguridad configuradas.
- Roles base `ROLE_OWNER`, `ROLE_ADMIN`, `ROLE_FINANCE`, `ROLE_OPERATIONS`, `ROLE_INVENTORY`, `ROLE_DOCUMENTS` y `ROLE_AUDITOR`.

Trabajo siguiente:
- Mantener auditoria de eventos sensibles y accesos.
- Revisar configuracion real de HTTPS, cookies y proxy antes de produccion.
- Conectar la capa de escaneo documental existente a un antivirus o proveedor externo antes de operar con documentos reales.

## Agente 4 - Database Architect

Responsable de modelo MySQL, relaciones, indices e integridad de datos.

Decisiones activas:
- Base local H2 en modo MySQL para desarrollo rapido.
- Base real MySQL `lsototalbouw` creada y validada con Flyway.
- Entidades base con auditoria temporal.
- Estados normalizados con enums.

Trabajo siguiente:
- Mantener migraciones Flyway por versiones.
- Revisar indices de produccion con metricas reales despues de usar MySQL.
- Preparar politica final de backup, restore y retencion antes de despliegue externo.

## Agente 5 - Technical Documentation Engineer

Responsable de documentacion tecnica en ingles con estilo senior, clara para mantenimiento enterprise y revision profesional.

Decisiones activas:
- Documentar comportamiento real, no intencion generica.
- Priorizar servicios, controladores, seguridad, bootstrap, migraciones y flujos financieros.
- Escribir Javadocs concisos en ingles para clases publicas y metodos de negocio relevantes.
- Mantener documentacion operativa en `docs/` alineada con el estado real del proyecto.

Trabajo siguiente:
- Completar documentacion de servicios criticos antes de entrega.
- Preparar guia final de instalacion, operacion y demo.
- Revisar que no se documenten secretos reales ni credenciales productivas dentro del repositorio.

## Flujo de coordinacion

1. Backend define el contrato de datos.
2. Database valida relaciones e indices.
3. Security revisa superficie de ataque.
4. Frontend construye pantallas y experiencia.
5. Documentation Engineer documenta decisiones, APIs internas y operacion.
6. PM tecnico verifica compilacion, arranque y flujo de usuario.

## Estado funcional actual

Completado:
- Dashboard operativo.
- Seguridad base con perfiles `dev` y `prod`.
- Aislamiento inicial por `CompanyAccount`.
- Clientes: crear, listar, ver detalle, editar y archivar.
- Proyectos: crear, listar, ver detalle, editar y archivar.
- Presupuestos: crear, listar, ver detalle, editar y archivar.
- Facturas: crear, listar, ver detalle, editar y archivar.
- Pagos: crear, listar, ver detalle, editar y archivar; actualizan el cobrado de la factura.
- Gastos: crear, listar, ver detalle, editar y archivar.
- Materiales: crear, listar, ver detalle, editar y archivar.
- Herramientas: crear, listar, ver detalle, editar y archivar.
- Proveedores: crear, listar, ver detalle, editar y archivar.
- Calendario: crear, listar, editar y archivar eventos.
- Documentos: subir PDF/imagenes, listar, ver detalle, editar metadatos, descargar y archivar con validacion segura, escaneo local de firmas peligrosas, checksum SHA-256 de integridad, bloqueo de descargas si el archivo almacenado no coincide, auditoria y notificacion operativa de fallos de integridad.
- Partes de horas: registrar, listar, ver detalle, editar y archivar horas por proyecto con trabajador, tarifa, estado y marca facturable.
- Facturas: lineas detalladas con cantidad, precio, IVA, subtotal, total y recalculo automatico del importe.
- Presupuestos: lineas detalladas con cantidad, precio, IVA, subtotal, total y recalculo automatico del importe.
- PDF profesional de facturas y presupuestos con cabecera de empresa, cliente, lineas, totales y descarga segura.
- Conversion de partes de horas aprobados y facturables en lineas de factura, con bloqueo de duplicados y validacion de cliente/proyecto.
- Panel de rentabilidad por proyecto con presupuesto, facturado, cobrado, gastos, horas, coste estimado, margen y estado de riesgo.
- Filtros operativos en clientes, proyectos y facturas con busqueda por texto, estado y aislamiento por empresa.
- Paginacion en clientes, proyectos y facturas combinada con filtros de busqueda y estado.
- Filtros avanzados en documentos, gastos y materiales con busqueda textual, categoria, proyecto y stock bajo.
- Auditoria de acciones sensibles con usuario, empresa, modulo, accion, registro, resumen y detalle.
- Roles y permisos granulares por modulo aplicados en Spring Security.
- Gestion visual de usuarios con creacion, edicion, asignacion de roles, cambio opcional de contrasena y archivado.
- Migraciones Flyway con esquema inicial versionado.
- Politica documentada de backups, restauracion y recuperacion de documentos.
- Tests automatizados de arranque con perfil `test`, autorizacion por roles, CSRF y aislamiento multiempresa.
- Bloqueo temporal por intentos fallidos de login, reseteo en login correcto y auditoria de accesos.
- Gestion operativa de bloqueos en usuarios: estado de seguridad visible y desbloqueo manual auditado por OWNER/ADMIN.
- Notificaciones operativas con marcado como leido idempotente y objetivos seguros.
- Reclamaciones de pago con historial, PDF y auditoria.
- Cobros pendientes con prioridad, filtros seguros y acceso rapido a registro de pagos.
- Pagos con filtros, resumen operativo, precarga de importe pendiente y retorno contextual seguro.
- Auditoria filtrable por accion, modulo, usuario y fechas, con exportacion CSV y resumen visual.
- Documentacion pre-MySQL con punto de parada, variables y checklist de entrega.
- Base MySQL real `lsototalbouw` creada, con usuario de aplicacion limitado, migraciones `V1` a `V10` preparadas y bootstrap inicial de produccion.
- Notificaciones de seguridad ante bloqueo de cuentas por intentos fallidos de login, con resolucion automatica al desbloquear o iniciar sesion correctamente.

Matriz de permisos:
- `OWNER` y `ADMIN`: acceso completo.
- `FINANCE`: clientes, presupuestos, facturas, pagos, gastos y rentabilidad.
- `OPERATIONS`: clientes, proyectos, calendario, partes de horas y documentos.
- `INVENTORY`: materiales, herramientas y proveedores.
- `DOCUMENTS`: documentos.
- `AUDITOR`: auditoria y dashboard.

Pendiente para produccion avanzada:
- Ampliar tests automatizados por servicios unitarios ademas de integracion MVC.
- Conectar escaneo antivirus externo y versionado de documentos.
- Backup externo, HTTPS/proxy final y rotacion formal de secretos.

## Credenciales dev

- Email: `admin@lsototalbouw.nl`
- Contrasena: `Admin123!`

Estas credenciales son solo para el perfil `dev`. En `prod`, el primer administrador se crea con variables de entorno bootstrap y debe cambiarse/rotarse antes de operar con datos reales.
