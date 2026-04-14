# Minimarket Central

Aplicacion web full-stack para un minimarket con Angular, ASP.NET Core Web API y SQL Server.

## Estructura

- `src/`: frontend Angular con dashboard, gestion de productos, formulario de ventas y reportes.
- `backend/Minimarket.Api/`: API REST en .NET 9 con arquitectura por capas.
- `db/minimarket.sql`: script SQL completo para crear la base y cargar datos iniciales.

## Funcionalidades incluidas

- CRUD de productos.
- Control de stock minimo y stock actual.
- Registro de ventas con detalle.
- Usuarios con roles `admin` y `cajero`.
- Reporte diario de ventas y dashboard de resumen.

## Arquitectura backend

- `Controllers`: exponen endpoints REST.
- `Services`: concentran reglas de negocio.
- `Repositories`: acceden a Entity Framework.
- `Data`: contiene `MinimarketDbContext`.
- `Models`: entidades principales.
- `DTOs`: contratos de entrada y salida para la API.

## Base de datos

Tienes dos formas de crearla:

1. Manual con script SQL
   Ejecuta `db/minimarket.sql` en SQL Server Management Studio.

2. Automatica con Entity Framework
   La API usa `Database.EnsureCreated()` al iniciar y crea la base si no existe.

Cadena de conexion actual en `backend/Minimarket.Api/appsettings.json`:

```json
"Server=localhost;Database=MinimarketDb;Trusted_Connection=True;TrustServerCertificate=True;"
```

Si tu instancia de SQL Server usa otro nombre, ajusta esa cadena.

## Credenciales iniciales

- `admin / Admin123!`
- `cajero / Caja123!`

## Ejecucion paso a paso

### 1. Backend

Desde `backend/Minimarket.Api` ejecuta:

```bash
dotnet restore
dotnet run
```

La API quedara en:

```text
http://localhost:5210
```

Endpoints principales:

- `GET /api/products`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`
- `GET /api/categories`
- `GET /api/users`
- `POST /api/auth/login`
- `GET /api/sales`
- `POST /api/sales`
- `GET /api/reports/dashboard`
- `GET /api/reports/sales-summary`

### 2. Frontend

Desde la raiz del proyecto ejecuta:

```bash
npm install
npm start
```

Luego abre:

```text
http://localhost:4200
```

El frontend consume la API desde `http://localhost:5210/api`.

## Despliegue en otra maquina

Para una maquina de uso final, puedes publicar todo como una sola aplicacion:

### 1. Compilar Angular

Desde la raiz:

```bash
npm run build
```

### 2. Copiar el frontend compilado al backend

Copia el contenido de:

```text
dist/minimarket1-app/browser
```

hacia:

```text
backend/Minimarket.Api/wwwroot/browser
```

### 3. Publicar el backend

Desde `backend/Minimarket.Api`:

```bash
dotnet publish -c Release -o .\publish
```

### 4. Preparar la maquina destino

Instala:

- SQL Server o SQL Server Express
- .NET 9 Hosting Bundle o .NET 9 Runtime

### 5. Crear la base de datos

Ejecuta el script:

```text
db/minimarket.sql
```

Si el servidor SQL de la maquina destino no es `localhost`, ajusta la cadena de conexion en `appsettings.json`.

### 6. Copiar la publicacion

Copia el contenido de:

```text
backend/Minimarket.Api/publish
```

a la maquina destino, por ejemplo:

```text
C:\Apps\Minimarket
```

### 7. Ejecutar la aplicacion

En la maquina destino:

```powershell
cd C:\Apps\Minimarket
.\Minimarket.Api.exe
```

Luego abre:

```text
http://localhost:5210
```

La misma aplicacion servira:

- frontend Angular
- API REST

Ya no necesitas correr `ng serve` en la maquina final.

## Explicacion breve del frontend

- `DashboardComponent`: muestra ventas del dia, total de transacciones, conteo de productos y stock bajo.
- `ProductListComponent`: formulario y tabla para crear, editar y eliminar productos.
- `SalesFormComponent`: formulario con detalle dinamico de venta y calculo de total.
- `ReportsComponent`: filtro por fechas para consultar el reporte diario de ventas.
- `core/services`: concentra la comunicacion HTTP con la API.

## Verificacion realizada

- `dotnet build` en `backend/Minimarket.Api`
- `npm run build` en la raiz del proyecto

Ambos procesos compilaron correctamente el 7 de abril de 2026.
