# Como arrancar LSOTOTALBOUW

## Carpeta correcta

Abre siempre esta carpeta como proyecto:

```text
C:\Users\Othmane\Desktop\ProyectoOmar
```

Dentro deben verse estos archivos:

```text
pom.xml
mvnw.cmd
src
data
docs
```

## Arrancar desde terminal

En PowerShell:

```powershell
cd C:\Users\Othmane\Desktop\ProyectoOmar
.\mvnw.cmd spring-boot:run
```

Despues abre:

```text
http://localhost:8080/login
```

Credenciales demo:

```text
Email: admin@lsototalbouw.nl
Contrasena: Admin123!
```

## Si IntelliJ da errores

Lo recomendable es abrir el proyecto desde `pom.xml` o desde la carpeta `ProyectoOmar`, pero asegurandote de que IntelliJ lo importa como Maven.

Pasos:

1. Cierra IntelliJ.
2. Abre IntelliJ.
3. File > Open.
4. Selecciona `C:\Users\Othmane\Desktop\ProyectoOmar\pom.xml`.
5. Elige abrirlo como proyecto Maven.

La clase principal correcta es:

```text
com.lsototalbouw.LsoTotalbouwApplication
```

## Si el puerto 8080 esta ocupado

Puedes parar procesos Java desde PowerShell:

```powershell
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
```

Y despues volver a arrancar:

```powershell
.\mvnw.cmd spring-boot:run
```
