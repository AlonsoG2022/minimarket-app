# Minimarket API Java

Backend alternativo en Java para reemplazar `backend/Minimarket.Api` sin cambiar el frontend actual.

## Stack

- Java 21 LTS
- Spring Boot 3.3
- Spring Web
- Spring Data JPA
- SQL Server JDBC Driver
- Maven

## Endpoints implementados

- `POST /api/auth/login`
- `GET /api/categories`
- `GET /api/products`
- `GET /api/products/{id}`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`
- `GET /api/users`
- `POST /api/users`
- `GET /api/sales`
- `GET /api/sales/{id}`
- `POST /api/sales`
- `GET /api/reports/dashboard`
- `GET /api/reports/sales-summary`

## Configuracion

Edita `src/main/resources/application.properties` y completa la conexion a SQL Server.

Ejemplo con autenticacion SQL:

```properties
spring.datasource.url=jdbc:sqlserver://localhost;databaseName=MinimarketDb;encrypt=true;trustServerCertificate=true
spring.datasource.username=minimarket_user
spring.datasource.password=Minimarket123!
```

## Ejecutar

```powershell
cd backend-java\minimarket-api-java
mvn spring-boot:run
```

La API corre por defecto en:

```text
http://localhost:5211
```

## Nota

La meta de este backend es mantener el mismo contrato HTTP que el backend .NET actual para que el frontend Angular pueda cambiar de proveedor de API con el menor impacto posible.
