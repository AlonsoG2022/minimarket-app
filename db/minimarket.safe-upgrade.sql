/*
    Script unico recomendado para instalacion o actualizacion de MinimarketDb
    -----------------------------------------------------------------------
    Objetivo:
    - NO elimina tablas ni datos existentes.
    - Crea la base si no existe.
    - Crea o actualiza el login SQL `minimarket_user`.
    - Crea el usuario de base de datos y le asigna `db_owner` si falta.
    - Crea tablas si no existen.
    - Agrega columnas faltantes con ALTER TABLE.
    - Ajusta tipos / nulabilidad de columnas actuales.
    - Crea restricciones, indices y llaves foraneas si faltan.
    - Inserta datos base solo si no existen.

    Nota:
    - Este es el script recomendado para despliegues y upgrades.
    - `minimarket.sql` queda solo para reseteo completo en desarrollo.
*/

USE master;
GO

IF DB_ID('MinimarketDb') IS NULL
BEGIN
    CREATE DATABASE MinimarketDb;
END;
GO

BEGIN TRY
    IF NOT EXISTS
    (
        SELECT 1
        FROM sys.server_principals
        WHERE name = 'minimarket_user'
    )
    BEGIN
        CREATE LOGIN minimarket_user
        WITH PASSWORD = 'Minimarket123!',
             CHECK_POLICY = OFF,
             CHECK_EXPIRATION = OFF;
    END;
    ELSE
    BEGIN
        ALTER LOGIN minimarket_user
        WITH PASSWORD = 'Minimarket123!',
             CHECK_POLICY = OFF,
             CHECK_EXPIRATION = OFF;
    END;
END TRY
BEGIN CATCH
    PRINT 'Aviso: no se pudo crear o actualizar el login minimarket_user (requiere permisos de servidor). '
        + 'Si el login ya existe, puedes ignorar este aviso de forma segura. '
        + 'Para gestionarlo, ejecuta el script conectado como sa o un login administrador.';
END CATCH;
GO

USE MinimarketDb;
GO

SET NOCOUNT ON;
GO

IF DATABASE_PRINCIPAL_ID('minimarket_user') IS NULL
BEGIN
    CREATE USER minimarket_user FOR LOGIN minimarket_user;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.database_role_members drm
    INNER JOIN sys.database_principals rolep ON rolep.principal_id = drm.role_principal_id
    INNER JOIN sys.database_principals memberp ON memberp.principal_id = drm.member_principal_id
    WHERE rolep.name = 'db_owner'
      AND memberp.name = 'minimarket_user'
)
BEGIN
    ALTER ROLE db_owner ADD MEMBER minimarket_user;
END;
GO

CREATE OR ALTER PROCEDURE dbo.usp_EnsureDefaultConstraint
    @SchemaName SYSNAME,
    @TableName SYSNAME,
    @ColumnName SYSNAME,
    @ConstraintName SYSNAME,
    @Definition NVARCHAR(4000)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @QualifiedTable NVARCHAR(517) = QUOTENAME(@SchemaName) + N'.' + QUOTENAME(@TableName);
    DECLARE @ObjectId INT = OBJECT_ID(@QualifiedTable);

    IF @ObjectId IS NULL
    BEGIN
        RETURN;
    END;

    IF NOT EXISTS
    (
        SELECT 1
        FROM sys.default_constraints dc
        INNER JOIN sys.columns c
            ON c.object_id = dc.parent_object_id
           AND c.column_id = dc.parent_column_id
        WHERE dc.parent_object_id = @ObjectId
          AND c.name = @ColumnName
    )
    BEGIN
        DECLARE @Sql NVARCHAR(MAX) =
            N'ALTER TABLE ' + @QualifiedTable
            + N' ADD CONSTRAINT ' + QUOTENAME(@ConstraintName)
            + N' DEFAULT ' + @Definition
            + N' FOR ' + QUOTENAME(@ColumnName) + N';';

        EXEC sp_executesql @Sql;
    END;
END;
GO

/* =========================================================
   CATEGORIAS
   ========================================================= */
IF OBJECT_ID('dbo.Categorias', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Categorias
    (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Categorias PRIMARY KEY,
        Nombre NVARCHAR(100) NOT NULL,
        Descripcion NVARCHAR(250) NULL,
        Activo BIT NOT NULL CONSTRAINT DF_Categorias_Activo DEFAULT (1)
    );
END;
GO

IF COL_LENGTH('dbo.Categorias', 'Nombre') IS NULL
BEGIN
    ALTER TABLE dbo.Categorias ADD Nombre NVARCHAR(100) NULL;
END;
GO

IF COL_LENGTH('dbo.Categorias', 'Descripcion') IS NULL
BEGIN
    ALTER TABLE dbo.Categorias ADD Descripcion NVARCHAR(250) NULL;
END;
GO

IF COL_LENGTH('dbo.Categorias', 'Activo') IS NULL
BEGIN
    ALTER TABLE dbo.Categorias ADD Activo BIT NULL;
END;
GO

UPDATE dbo.Categorias
SET
    Nombre = ISNULL(Nombre, ''),
    Activo = ISNULL(Activo, 1)
WHERE Nombre IS NULL OR Activo IS NULL;
GO

ALTER TABLE dbo.Categorias ALTER COLUMN Nombre NVARCHAR(100) NOT NULL;
ALTER TABLE dbo.Categorias ALTER COLUMN Descripcion NVARCHAR(250) NULL;
ALTER TABLE dbo.Categorias ALTER COLUMN Activo BIT NOT NULL;
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'Categorias', 'Activo', 'DF_Categorias_Activo', '(1)';
GO

/* =========================================================
   PRODUCTOS
   ========================================================= */
IF OBJECT_ID('dbo.Productos', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Productos
    (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Productos PRIMARY KEY,
        Nombre NVARCHAR(150) NOT NULL,
        Sku NVARCHAR(30) NOT NULL,
        CodigoBarras NVARCHAR(50) NULL,
        CodigoBarrasCompra NVARCHAR(50) NULL,
        Descripcion NVARCHAR(250) NULL,
        Precio DECIMAL(10,2) NOT NULL,
        Costo DECIMAL(10,2) NOT NULL CONSTRAINT DF_Productos_Costo DEFAULT (0),
        Stock INT NOT NULL,
        StockMinimo INT NOT NULL CONSTRAINT DF_Productos_StockMinimo DEFAULT (5),
        UnidadVenta NVARCHAR(30) NOT NULL CONSTRAINT DF_Productos_UnidadVenta DEFAULT ('unidad'),
        UnidadCompra NVARCHAR(30) NOT NULL CONSTRAINT DF_Productos_UnidadCompra DEFAULT ('unidad'),
        UnidadesPorCompra INT NOT NULL CONSTRAINT DF_Productos_UnidadesPorCompra DEFAULT (1),
        Activo BIT NOT NULL CONSTRAINT DF_Productos_Activo DEFAULT (1),
        CategoriaId INT NOT NULL
    );
END;
GO

IF COL_LENGTH('dbo.Productos', 'Nombre') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD Nombre NVARCHAR(150) NULL;
END;
GO

IF COL_LENGTH('dbo.Productos', 'Sku') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD Sku NVARCHAR(30) NULL;
END;
GO

IF COL_LENGTH('dbo.Productos', 'CodigoBarras') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD CodigoBarras NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH('dbo.Productos', 'CodigoBarrasCompra') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD CodigoBarrasCompra NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH('dbo.Productos', 'Descripcion') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD Descripcion NVARCHAR(250) NULL;
END;
GO

IF COL_LENGTH('dbo.Productos', 'Precio') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD Precio DECIMAL(10,2) NULL;
END;
GO

IF COL_LENGTH('dbo.Productos', 'Costo') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD Costo DECIMAL(10,2) NULL;
END;
GO

IF COL_LENGTH('dbo.Productos', 'Stock') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD Stock INT NULL;
END;
GO

IF COL_LENGTH('dbo.Productos', 'StockMinimo') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD StockMinimo INT NULL;
END;
GO

IF COL_LENGTH('dbo.Productos', 'FechaCaducidad') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD FechaCaducidad DATE NULL;
END;
GO

IF COL_LENGTH('dbo.Productos', 'UnidadVenta') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD UnidadVenta NVARCHAR(30) NULL;
END;
GO

IF COL_LENGTH('dbo.Productos', 'UnidadCompra') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD UnidadCompra NVARCHAR(30) NULL;
END;
GO

IF COL_LENGTH('dbo.Productos', 'UnidadesPorCompra') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD UnidadesPorCompra INT NULL;
END;
GO

IF COL_LENGTH('dbo.Productos', 'Activo') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD Activo BIT NULL;
END;
GO

IF COL_LENGTH('dbo.Productos', 'CategoriaId') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD CategoriaId INT NULL;
END;
GO

UPDATE dbo.Productos
SET
    Nombre = ISNULL(Nombre, ''),
    Sku = ISNULL(Sku, CONCAT('SKU-', Id)),
    Precio = ISNULL(Precio, 0),
    Costo = ISNULL(Costo, 0),
    Stock = ISNULL(Stock, 0),
    StockMinimo = ISNULL(StockMinimo, 5),
    UnidadVenta = ISNULL(NULLIF(UnidadVenta, ''), 'unidad'),
    UnidadCompra = ISNULL(NULLIF(UnidadCompra, ''), 'unidad'),
    UnidadesPorCompra = ISNULL(UnidadesPorCompra, 1),
    Activo = ISNULL(Activo, 1),
    CategoriaId = ISNULL(CategoriaId, 1)
WHERE
    Nombre IS NULL
    OR Sku IS NULL
    OR Precio IS NULL
    OR Costo IS NULL
    OR Stock IS NULL
    OR StockMinimo IS NULL
    OR UnidadVenta IS NULL
    OR UnidadCompra IS NULL
    OR UnidadesPorCompra IS NULL
    OR Activo IS NULL
    OR CategoriaId IS NULL;
GO

UPDATE dbo.Productos
SET StockMinimo = 5
WHERE StockMinimo <> 5;
GO


IF  EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UQ_Productos_Sku'
      AND object_id = OBJECT_ID('dbo.Productos')
)
BEGIN
    DROP INDEX UQ_Productos_Sku ON dbo.Productos;
END;
GO

IF EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UQ_Productos_CodigoBarras'
      AND object_id = OBJECT_ID('dbo.Productos')
)
BEGIN
    DROP INDEX UQ_Productos_CodigoBarras ON dbo.Productos;
END;
GO

IF  EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UQ_Productos_CodigoBarrasCompra'
      AND object_id = OBJECT_ID('dbo.Productos')
)
BEGIN
    DROP INDEX UQ_Productos_CodigoBarrasCompra ON dbo.Productos;
END;
GO

ALTER TABLE dbo.Productos ALTER COLUMN Nombre NVARCHAR(150) NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN Sku NVARCHAR(30) NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN CodigoBarras NVARCHAR(50) NULL;
ALTER TABLE dbo.Productos ALTER COLUMN CodigoBarrasCompra NVARCHAR(50) NULL;
ALTER TABLE dbo.Productos ALTER COLUMN Descripcion NVARCHAR(250) NULL;
ALTER TABLE dbo.Productos ALTER COLUMN Precio DECIMAL(10,2) NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN Costo DECIMAL(10,2) NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN Stock INT NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN StockMinimo INT NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN FechaCaducidad DATE NULL;
ALTER TABLE dbo.Productos ALTER COLUMN UnidadVenta NVARCHAR(30) NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN UnidadCompra NVARCHAR(30) NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN UnidadesPorCompra INT NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN Activo BIT NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN CategoriaId INT NOT NULL;
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'Productos', 'Activo', 'DF_Productos_Activo', '(1)';
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'Productos', 'StockMinimo', 'DF_Productos_StockMinimo', '(5)';
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'Productos', 'Costo', 'DF_Productos_Costo', '(0)';
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'Productos', 'UnidadVenta', 'DF_Productos_UnidadVenta', '(''unidad'')';
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'Productos', 'UnidadCompra', 'DF_Productos_UnidadCompra', '(''unidad'')';
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'Productos', 'UnidadesPorCompra', 'DF_Productos_UnidadesPorCompra', '(1)';
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UQ_Productos_Sku'
      AND object_id = OBJECT_ID('dbo.Productos')
)
BEGIN
    CREATE UNIQUE INDEX UQ_Productos_Sku ON dbo.Productos(Sku);
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UQ_Productos_CodigoBarras'
      AND object_id = OBJECT_ID('dbo.Productos')
)
BEGIN
    CREATE UNIQUE INDEX UQ_Productos_CodigoBarras ON dbo.Productos(CodigoBarras) WHERE CodigoBarras IS NOT NULL;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UQ_Productos_CodigoBarrasCompra'
      AND object_id = OBJECT_ID('dbo.Productos')
)
BEGIN
    CREATE UNIQUE INDEX UQ_Productos_CodigoBarrasCompra ON dbo.Productos(CodigoBarrasCompra) WHERE CodigoBarrasCompra IS NOT NULL;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_Productos_Categorias'
)
BEGIN
    ALTER TABLE dbo.Productos
    ADD CONSTRAINT FK_Productos_Categorias
        FOREIGN KEY (CategoriaId) REFERENCES dbo.Categorias(Id);
END;
GO

/* =========================================================
   USUARIOS
   ========================================================= */
IF OBJECT_ID('dbo.Usuarios', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Usuarios
    (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Usuarios PRIMARY KEY,
        NombreCompleto NVARCHAR(120) NOT NULL,
        Username NVARCHAR(50) NOT NULL,
        PasswordHash NVARCHAR(256) NOT NULL,
        Rol NVARCHAR(20) NOT NULL,
        Activo BIT NOT NULL CONSTRAINT DF_Usuarios_Activo DEFAULT (1)
    );
END;
GO

IF COL_LENGTH('dbo.Usuarios', 'NombreCompleto') IS NULL
BEGIN
    ALTER TABLE dbo.Usuarios ADD NombreCompleto NVARCHAR(120) NULL;
END;
GO

IF COL_LENGTH('dbo.Usuarios', 'Username') IS NULL
BEGIN
    ALTER TABLE dbo.Usuarios ADD Username NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH('dbo.Usuarios', 'PasswordHash') IS NULL
BEGIN
    ALTER TABLE dbo.Usuarios ADD PasswordHash NVARCHAR(256) NULL;
END;
GO

IF COL_LENGTH('dbo.Usuarios', 'Rol') IS NULL
BEGIN
    ALTER TABLE dbo.Usuarios ADD Rol NVARCHAR(20) NULL;
END;
GO

IF COL_LENGTH('dbo.Usuarios', 'Activo') IS NULL
BEGIN
    ALTER TABLE dbo.Usuarios ADD Activo BIT NULL;
END;
GO

UPDATE dbo.Usuarios
SET
    NombreCompleto = ISNULL(NombreCompleto, ''),
    Username = ISNULL(Username, CONCAT('user', Id)),
    PasswordHash = ISNULL(PasswordHash, ''),
    Rol = ISNULL(Rol, 'cajero'),
    Activo = ISNULL(Activo, 1)
WHERE
    NombreCompleto IS NULL
    OR Username IS NULL
    OR PasswordHash IS NULL
    OR Rol IS NULL
    OR Activo IS NULL;
GO

ALTER TABLE dbo.Usuarios ALTER COLUMN NombreCompleto NVARCHAR(120) NOT NULL;
ALTER TABLE dbo.Usuarios ALTER COLUMN Username NVARCHAR(50) NOT NULL;
ALTER TABLE dbo.Usuarios ALTER COLUMN PasswordHash NVARCHAR(256) NOT NULL;
ALTER TABLE dbo.Usuarios ALTER COLUMN Rol NVARCHAR(20) NOT NULL;
ALTER TABLE dbo.Usuarios ALTER COLUMN Activo BIT NOT NULL;
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'Usuarios', 'Activo', 'DF_Usuarios_Activo', '(1)';
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UQ_Usuarios_Username'
      AND object_id = OBJECT_ID('dbo.Usuarios')
)
BEGIN
    CREATE UNIQUE INDEX UQ_Usuarios_Username ON dbo.Usuarios(Username);
END;
GO

/* =========================================================
   PROVEEDORES
   ========================================================= */
IF OBJECT_ID('dbo.Proveedores', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Proveedores
    (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Proveedores PRIMARY KEY,
        Nombre NVARCHAR(150) NOT NULL,
        NumeroDocumento NVARCHAR(30) NULL,
        NombreContacto NVARCHAR(120) NULL,
        Telefono NVARCHAR(30) NULL,
        Correo NVARCHAR(120) NULL,
        Direccion NVARCHAR(250) NULL,
        Notas NVARCHAR(250) NULL,
        Activo BIT NOT NULL CONSTRAINT DF_Proveedores_Activo DEFAULT (1)
    );
END;
GO

IF COL_LENGTH('dbo.Proveedores', 'Nombre') IS NULL
BEGIN
    ALTER TABLE dbo.Proveedores ADD Nombre NVARCHAR(150) NULL;
END;
GO

IF COL_LENGTH('dbo.Proveedores', 'NumeroDocumento') IS NULL
BEGIN
    ALTER TABLE dbo.Proveedores ADD NumeroDocumento NVARCHAR(30) NULL;
END;
GO

IF COL_LENGTH('dbo.Proveedores', 'NombreContacto') IS NULL
BEGIN
    ALTER TABLE dbo.Proveedores ADD NombreContacto NVARCHAR(120) NULL;
END;
GO

IF COL_LENGTH('dbo.Proveedores', 'Telefono') IS NULL
BEGIN
    ALTER TABLE dbo.Proveedores ADD Telefono NVARCHAR(30) NULL;
END;
GO

IF COL_LENGTH('dbo.Proveedores', 'Correo') IS NULL
BEGIN
    ALTER TABLE dbo.Proveedores ADD Correo NVARCHAR(120) NULL;
END;
GO

IF COL_LENGTH('dbo.Proveedores', 'Direccion') IS NULL
BEGIN
    ALTER TABLE dbo.Proveedores ADD Direccion NVARCHAR(250) NULL;
END;
GO

IF COL_LENGTH('dbo.Proveedores', 'Notas') IS NULL
BEGIN
    ALTER TABLE dbo.Proveedores ADD Notas NVARCHAR(250) NULL;
END;
GO

IF COL_LENGTH('dbo.Proveedores', 'Activo') IS NULL
BEGIN
    ALTER TABLE dbo.Proveedores ADD Activo BIT NULL;
END;
GO

UPDATE dbo.Proveedores
SET
    Nombre = ISNULL(Nombre, ''),
    Activo = ISNULL(Activo, 1)
WHERE Nombre IS NULL OR Activo IS NULL;
GO

ALTER TABLE dbo.Proveedores ALTER COLUMN Nombre NVARCHAR(150) NOT NULL;
ALTER TABLE dbo.Proveedores ALTER COLUMN NumeroDocumento NVARCHAR(30) NULL;
ALTER TABLE dbo.Proveedores ALTER COLUMN NombreContacto NVARCHAR(120) NULL;
ALTER TABLE dbo.Proveedores ALTER COLUMN Telefono NVARCHAR(30) NULL;
ALTER TABLE dbo.Proveedores ALTER COLUMN Correo NVARCHAR(120) NULL;
ALTER TABLE dbo.Proveedores ALTER COLUMN Direccion NVARCHAR(250) NULL;
ALTER TABLE dbo.Proveedores ALTER COLUMN Notas NVARCHAR(250) NULL;
ALTER TABLE dbo.Proveedores ALTER COLUMN Activo BIT NOT NULL;
GO

/* =========================================================
   CAJA SESIONES
   ========================================================= */
IF OBJECT_ID('dbo.CajaSesiones', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.CajaSesiones
    (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CajaSesiones PRIMARY KEY,
        UsuarioId INT NOT NULL,
        FechaApertura DATETIME2 NOT NULL CONSTRAINT DF_CajaSesiones_FechaApertura DEFAULT (SYSDATETIME()),
        FechaCierre DATETIME2 NULL,
        MontoInicial DECIMAL(10,2) NOT NULL,
        MontoEsperadoCierre DECIMAL(10,2) NULL,
        MontoContadoCierre DECIMAL(10,2) NULL,
        Diferencia DECIMAL(10,2) NULL,
        Estado NVARCHAR(20) NOT NULL CONSTRAINT DF_CajaSesiones_Estado DEFAULT ('abierta'),
        Notas NVARCHAR(250) NULL
    );
END;
GO

IF COL_LENGTH('dbo.CajaSesiones', 'UsuarioId') IS NULL
BEGIN
    ALTER TABLE dbo.CajaSesiones ADD UsuarioId INT NULL;
END;
GO

IF COL_LENGTH('dbo.CajaSesiones', 'FechaApertura') IS NULL
BEGIN
    ALTER TABLE dbo.CajaSesiones ADD FechaApertura DATETIME2 NULL;
END;
GO

IF COL_LENGTH('dbo.CajaSesiones', 'FechaCierre') IS NULL
BEGIN
    ALTER TABLE dbo.CajaSesiones ADD FechaCierre DATETIME2 NULL;
END;
GO

IF COL_LENGTH('dbo.CajaSesiones', 'MontoInicial') IS NULL
BEGIN
    ALTER TABLE dbo.CajaSesiones ADD MontoInicial DECIMAL(10,2) NULL;
END;
GO

IF COL_LENGTH('dbo.CajaSesiones', 'MontoEsperadoCierre') IS NULL
BEGIN
    ALTER TABLE dbo.CajaSesiones ADD MontoEsperadoCierre DECIMAL(10,2) NULL;
END;
GO

IF COL_LENGTH('dbo.CajaSesiones', 'MontoContadoCierre') IS NULL
BEGIN
    ALTER TABLE dbo.CajaSesiones ADD MontoContadoCierre DECIMAL(10,2) NULL;
END;
GO

IF COL_LENGTH('dbo.CajaSesiones', 'Diferencia') IS NULL
BEGIN
    ALTER TABLE dbo.CajaSesiones ADD Diferencia DECIMAL(10,2) NULL;
END;
GO

IF COL_LENGTH('dbo.CajaSesiones', 'Estado') IS NULL
BEGIN
    ALTER TABLE dbo.CajaSesiones ADD Estado NVARCHAR(20) NULL;
END;
GO

IF COL_LENGTH('dbo.CajaSesiones', 'Notas') IS NULL
BEGIN
    ALTER TABLE dbo.CajaSesiones ADD Notas NVARCHAR(250) NULL;
END;
GO

UPDATE dbo.CajaSesiones
SET
    UsuarioId = ISNULL(UsuarioId, 1),
    FechaApertura = ISNULL(FechaApertura, SYSDATETIME()),
    MontoInicial = ISNULL(MontoInicial, 0),
    Estado = ISNULL(NULLIF(Estado, ''), 'abierta')
WHERE UsuarioId IS NULL OR FechaApertura IS NULL OR MontoInicial IS NULL OR Estado IS NULL;
GO

ALTER TABLE dbo.CajaSesiones ALTER COLUMN UsuarioId INT NOT NULL;
ALTER TABLE dbo.CajaSesiones ALTER COLUMN FechaApertura DATETIME2 NOT NULL;
ALTER TABLE dbo.CajaSesiones ALTER COLUMN FechaCierre DATETIME2 NULL;
ALTER TABLE dbo.CajaSesiones ALTER COLUMN MontoInicial DECIMAL(10,2) NOT NULL;
ALTER TABLE dbo.CajaSesiones ALTER COLUMN MontoEsperadoCierre DECIMAL(10,2) NULL;
ALTER TABLE dbo.CajaSesiones ALTER COLUMN MontoContadoCierre DECIMAL(10,2) NULL;
ALTER TABLE dbo.CajaSesiones ALTER COLUMN Diferencia DECIMAL(10,2) NULL;
ALTER TABLE dbo.CajaSesiones ALTER COLUMN Estado NVARCHAR(20) NOT NULL;
ALTER TABLE dbo.CajaSesiones ALTER COLUMN Notas NVARCHAR(250) NULL;
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'CajaSesiones', 'FechaApertura', 'DF_CajaSesiones_FechaApertura', '(SYSDATETIME())';
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'CajaSesiones', 'Estado', 'DF_CajaSesiones_Estado', '(''abierta'')';
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_CajaSesiones_Usuarios')
BEGIN
    ALTER TABLE dbo.CajaSesiones
    ADD CONSTRAINT FK_CajaSesiones_Usuarios FOREIGN KEY (UsuarioId) REFERENCES dbo.Usuarios(Id);
END;
GO

/* =========================================================
   CAJA MOVIMIENTOS
   ========================================================= */
IF OBJECT_ID('dbo.CajaMovimientos', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.CajaMovimientos
    (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CajaMovimientos PRIMARY KEY,
        CajaSesionId INT NOT NULL,
        FechaMovimiento DATETIME2 NOT NULL CONSTRAINT DF_CajaMovimientos_FechaMovimiento DEFAULT (SYSDATETIME()),
        Tipo NVARCHAR(30) NOT NULL,
        Monto DECIMAL(10,2) NOT NULL,
        Descripcion NVARCHAR(250) NULL,
        TipoReferencia NVARCHAR(30) NULL,
        ReferenciaId INT NULL
    );
END;
GO

IF COL_LENGTH('dbo.CajaMovimientos', 'CajaSesionId') IS NULL
BEGIN
    ALTER TABLE dbo.CajaMovimientos ADD CajaSesionId INT NULL;
END;
GO

IF COL_LENGTH('dbo.CajaMovimientos', 'FechaMovimiento') IS NULL
BEGIN
    ALTER TABLE dbo.CajaMovimientos ADD FechaMovimiento DATETIME2 NULL;
END;
GO

IF COL_LENGTH('dbo.CajaMovimientos', 'Tipo') IS NULL
BEGIN
    ALTER TABLE dbo.CajaMovimientos ADD Tipo NVARCHAR(30) NULL;
END;
GO

IF COL_LENGTH('dbo.CajaMovimientos', 'Monto') IS NULL
BEGIN
    ALTER TABLE dbo.CajaMovimientos ADD Monto DECIMAL(10,2) NULL;
END;
GO

IF COL_LENGTH('dbo.CajaMovimientos', 'Descripcion') IS NULL
BEGIN
    ALTER TABLE dbo.CajaMovimientos ADD Descripcion NVARCHAR(250) NULL;
END;
GO

IF COL_LENGTH('dbo.CajaMovimientos', 'TipoReferencia') IS NULL
BEGIN
    ALTER TABLE dbo.CajaMovimientos ADD TipoReferencia NVARCHAR(30) NULL;
END;
GO

IF COL_LENGTH('dbo.CajaMovimientos', 'ReferenciaId') IS NULL
BEGIN
    ALTER TABLE dbo.CajaMovimientos ADD ReferenciaId INT NULL;
END;
GO

UPDATE dbo.CajaMovimientos
SET
    CajaSesionId = ISNULL(CajaSesionId, 0),
    FechaMovimiento = ISNULL(FechaMovimiento, SYSDATETIME()),
    Tipo = ISNULL(NULLIF(Tipo, ''), 'ingreso'),
    Monto = ISNULL(Monto, 0)
WHERE CajaSesionId IS NULL OR FechaMovimiento IS NULL OR Tipo IS NULL OR Monto IS NULL;
GO

ALTER TABLE dbo.CajaMovimientos ALTER COLUMN CajaSesionId INT NOT NULL;
ALTER TABLE dbo.CajaMovimientos ALTER COLUMN FechaMovimiento DATETIME2 NOT NULL;
ALTER TABLE dbo.CajaMovimientos ALTER COLUMN Tipo NVARCHAR(30) NOT NULL;
ALTER TABLE dbo.CajaMovimientos ALTER COLUMN Monto DECIMAL(10,2) NOT NULL;
ALTER TABLE dbo.CajaMovimientos ALTER COLUMN Descripcion NVARCHAR(250) NULL;
ALTER TABLE dbo.CajaMovimientos ALTER COLUMN TipoReferencia NVARCHAR(30) NULL;
ALTER TABLE dbo.CajaMovimientos ALTER COLUMN ReferenciaId INT NULL;
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'CajaMovimientos', 'FechaMovimiento', 'DF_CajaMovimientos_FechaMovimiento', '(SYSDATETIME())';
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_CajaMovimientos_CajaSesiones')
BEGIN
    ALTER TABLE dbo.CajaMovimientos
    ADD CONSTRAINT FK_CajaMovimientos_CajaSesiones FOREIGN KEY (CajaSesionId) REFERENCES dbo.CajaSesiones(Id) ON DELETE CASCADE;
END;
GO

/* =========================================================
   COMPRAS
   ========================================================= */
IF OBJECT_ID('dbo.Compras', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Compras
    (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Compras PRIMARY KEY,
        FechaCompra DATETIME2 NOT NULL CONSTRAINT DF_Compras_FechaCompra DEFAULT (SYSDATETIME()),
        ProveedorId INT NOT NULL,
        UsuarioId INT NOT NULL,
        NumeroComprobante NVARCHAR(50) NULL,
        Notas NVARCHAR(250) NULL,
        SubTotal DECIMAL(12,2) NOT NULL CONSTRAINT DF_Compras_SubTotal DEFAULT (0),
        Igv DECIMAL(12,2) NOT NULL CONSTRAINT DF_Compras_Igv DEFAULT (0),
        Total DECIMAL(12,2) NOT NULL
    );
END;
GO

IF COL_LENGTH('dbo.Compras', 'FechaCompra') IS NULL
BEGIN
    ALTER TABLE dbo.Compras ADD FechaCompra DATETIME2 NULL;
END;
GO

IF COL_LENGTH('dbo.Compras', 'ProveedorId') IS NULL
BEGIN
    ALTER TABLE dbo.Compras ADD ProveedorId INT NULL;
END;
GO

IF COL_LENGTH('dbo.Compras', 'UsuarioId') IS NULL
BEGIN
    ALTER TABLE dbo.Compras ADD UsuarioId INT NULL;
END;
GO

IF COL_LENGTH('dbo.Compras', 'NumeroComprobante') IS NULL
BEGIN
    ALTER TABLE dbo.Compras ADD NumeroComprobante NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH('dbo.Compras', 'Notas') IS NULL
BEGIN
    ALTER TABLE dbo.Compras ADD Notas NVARCHAR(250) NULL;
END;
GO

IF COL_LENGTH('dbo.Compras', 'SubTotal') IS NULL
BEGIN
    ALTER TABLE dbo.Compras ADD SubTotal DECIMAL(12,2) NULL;
END;
GO

IF COL_LENGTH('dbo.Compras', 'Igv') IS NULL
BEGIN
    ALTER TABLE dbo.Compras ADD Igv DECIMAL(12,2) NULL;
END;
GO

IF COL_LENGTH('dbo.Compras', 'Total') IS NULL
BEGIN
    ALTER TABLE dbo.Compras ADD Total DECIMAL(12,2) NULL;
END;
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'Compras', 'SubTotal', 'DF_Compras_SubTotal', '(0)';
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'Compras', 'Igv', 'DF_Compras_Igv', '(0)';
GO

UPDATE dbo.Compras
SET
    FechaCompra = ISNULL(FechaCompra, SYSDATETIME()),
    ProveedorId = ISNULL(ProveedorId, 1),
    UsuarioId = ISNULL(UsuarioId, 1),
    SubTotal = ISNULL(SubTotal, ROUND(ISNULL(Total, 0) / 1.18, 2)),
    Igv = ISNULL(Igv, ROUND(ISNULL(Total, 0) - ROUND(ISNULL(Total, 0) / 1.18, 2), 2)),
    Total = ISNULL(Total, 0)
WHERE FechaCompra IS NULL OR ProveedorId IS NULL OR UsuarioId IS NULL OR SubTotal IS NULL OR Igv IS NULL OR Total IS NULL;
GO

ALTER TABLE dbo.Compras ALTER COLUMN FechaCompra DATETIME2 NOT NULL;
ALTER TABLE dbo.Compras ALTER COLUMN ProveedorId INT NOT NULL;
ALTER TABLE dbo.Compras ALTER COLUMN UsuarioId INT NOT NULL;
ALTER TABLE dbo.Compras ALTER COLUMN NumeroComprobante NVARCHAR(50) NULL;
ALTER TABLE dbo.Compras ALTER COLUMN Notas NVARCHAR(250) NULL;
ALTER TABLE dbo.Compras ALTER COLUMN SubTotal DECIMAL(12,2) NOT NULL;
ALTER TABLE dbo.Compras ALTER COLUMN Igv DECIMAL(12,2) NOT NULL;
ALTER TABLE dbo.Compras ALTER COLUMN Total DECIMAL(12,2) NOT NULL;
GO

IF OBJECT_ID('dbo.DetalleCompra', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.DetalleCompra
    (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_DetalleCompra PRIMARY KEY,
        CompraId INT NOT NULL,
        ProductoId INT NOT NULL,
        CantidadPaquetes INT NOT NULL,
        UnidadesPorPaquete INT NOT NULL,
        TotalUnidades INT NOT NULL,
        CostoPaquete DECIMAL(10,2) NOT NULL,
        CostoUnitario DECIMAL(10,2) NOT NULL,
        Subtotal DECIMAL(12,2) NOT NULL,
        UnidadCompra NVARCHAR(30) NOT NULL,
        CodigoBarrasLeido NVARCHAR(50) NULL
    );
END;
GO

IF COL_LENGTH('dbo.DetalleCompra', 'CompraId') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleCompra ADD CompraId INT NULL;
END;
GO

IF COL_LENGTH('dbo.DetalleCompra', 'ProductoId') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleCompra ADD ProductoId INT NULL;
END;
GO

IF COL_LENGTH('dbo.DetalleCompra', 'CantidadPaquetes') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleCompra ADD CantidadPaquetes INT NULL;
END;
GO

IF COL_LENGTH('dbo.DetalleCompra', 'UnidadesPorPaquete') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleCompra ADD UnidadesPorPaquete INT NULL;
END;
GO

IF COL_LENGTH('dbo.DetalleCompra', 'TotalUnidades') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleCompra ADD TotalUnidades INT NULL;
END;
GO

IF COL_LENGTH('dbo.DetalleCompra', 'CostoPaquete') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleCompra ADD CostoPaquete DECIMAL(10,2) NULL;
END;
GO

IF COL_LENGTH('dbo.DetalleCompra', 'CostoUnitario') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleCompra ADD CostoUnitario DECIMAL(10,2) NULL;
END;
GO

IF COL_LENGTH('dbo.DetalleCompra', 'Subtotal') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleCompra ADD Subtotal DECIMAL(12,2) NULL;
END;
GO

IF COL_LENGTH('dbo.DetalleCompra', 'UnidadCompra') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleCompra ADD UnidadCompra NVARCHAR(30) NULL;
END;
GO

IF COL_LENGTH('dbo.DetalleCompra', 'CodigoBarrasLeido') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleCompra ADD CodigoBarrasLeido NVARCHAR(50) NULL;
END;
GO

UPDATE dbo.DetalleCompra
SET
    CompraId = ISNULL(CompraId, 0),
    ProductoId = ISNULL(ProductoId, 0),
    CantidadPaquetes = ISNULL(CantidadPaquetes, 0),
    UnidadesPorPaquete = ISNULL(UnidadesPorPaquete, 1),
    TotalUnidades = ISNULL(TotalUnidades, 0),
    CostoPaquete = ISNULL(CostoPaquete, 0),
    CostoUnitario = ISNULL(CostoUnitario, 0),
    Subtotal = ISNULL(Subtotal, 0),
    UnidadCompra = ISNULL(NULLIF(UnidadCompra, ''), 'unidad')
WHERE
    CompraId IS NULL
    OR ProductoId IS NULL
    OR CantidadPaquetes IS NULL
    OR UnidadesPorPaquete IS NULL
    OR TotalUnidades IS NULL
    OR CostoPaquete IS NULL
    OR CostoUnitario IS NULL
    OR Subtotal IS NULL
    OR UnidadCompra IS NULL;
GO

ALTER TABLE dbo.DetalleCompra ALTER COLUMN CompraId INT NOT NULL;
ALTER TABLE dbo.DetalleCompra ALTER COLUMN ProductoId INT NOT NULL;
ALTER TABLE dbo.DetalleCompra ALTER COLUMN CantidadPaquetes INT NOT NULL;
ALTER TABLE dbo.DetalleCompra ALTER COLUMN UnidadesPorPaquete INT NOT NULL;
ALTER TABLE dbo.DetalleCompra ALTER COLUMN TotalUnidades INT NOT NULL;
ALTER TABLE dbo.DetalleCompra ALTER COLUMN CostoPaquete DECIMAL(10,2) NOT NULL;
ALTER TABLE dbo.DetalleCompra ALTER COLUMN CostoUnitario DECIMAL(10,2) NOT NULL;
ALTER TABLE dbo.DetalleCompra ALTER COLUMN Subtotal DECIMAL(12,2) NOT NULL;
ALTER TABLE dbo.DetalleCompra ALTER COLUMN UnidadCompra NVARCHAR(30) NOT NULL;
ALTER TABLE dbo.DetalleCompra ALTER COLUMN CodigoBarrasLeido NVARCHAR(50) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Compras_Proveedores')
BEGIN
    ALTER TABLE dbo.Compras
    ADD CONSTRAINT FK_Compras_Proveedores FOREIGN KEY (ProveedorId) REFERENCES dbo.Proveedores(Id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Compras_Usuarios')
BEGIN
    ALTER TABLE dbo.Compras
    ADD CONSTRAINT FK_Compras_Usuarios FOREIGN KEY (UsuarioId) REFERENCES dbo.Usuarios(Id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_DetalleCompra_Compras')
BEGIN
    ALTER TABLE dbo.DetalleCompra
    ADD CONSTRAINT FK_DetalleCompra_Compras FOREIGN KEY (CompraId) REFERENCES dbo.Compras(Id) ON DELETE CASCADE;
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_DetalleCompra_Productos')
BEGIN
    ALTER TABLE dbo.DetalleCompra
    ADD CONSTRAINT FK_DetalleCompra_Productos FOREIGN KEY (ProductoId) REFERENCES dbo.Productos(Id);
END;
GO

/* =========================================================
   VENTAS
   ========================================================= */
IF OBJECT_ID('dbo.Ventas', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Ventas
    (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Ventas PRIMARY KEY,
        FechaVenta DATETIME2 NOT NULL CONSTRAINT DF_Ventas_FechaVenta DEFAULT (SYSDATETIME()),
        UsuarioId INT NOT NULL,
        CajaSesionId INT NULL,
        SubTotal DECIMAL(10,2) NOT NULL CONSTRAINT DF_Ventas_SubTotal DEFAULT (0),
        Igv DECIMAL(10,2) NOT NULL CONSTRAINT DF_Ventas_Igv DEFAULT (0),
        Total DECIMAL(10,2) NOT NULL,
        MetodoPago NVARCHAR(30) NOT NULL,
        Notas NVARCHAR(250) NULL
    );
END;
GO

IF COL_LENGTH('dbo.Ventas', 'FechaVenta') IS NULL
BEGIN
    ALTER TABLE dbo.Ventas ADD FechaVenta DATETIME2 NULL;
END;
GO

IF COL_LENGTH('dbo.Ventas', 'UsuarioId') IS NULL
BEGIN
    ALTER TABLE dbo.Ventas ADD UsuarioId INT NULL;
END;
GO

IF COL_LENGTH('dbo.Ventas', 'SubTotal') IS NULL
BEGIN
    ALTER TABLE dbo.Ventas ADD SubTotal DECIMAL(10,2) NULL;
END;
GO

IF COL_LENGTH('dbo.Ventas', 'Igv') IS NULL
BEGIN
    ALTER TABLE dbo.Ventas ADD Igv DECIMAL(10,2) NULL;
END;
GO

IF COL_LENGTH('dbo.Ventas', 'Total') IS NULL
BEGIN
    ALTER TABLE dbo.Ventas ADD Total DECIMAL(10,2) NULL;
END;
GO

IF COL_LENGTH('dbo.Ventas', 'CajaSesionId') IS NULL
BEGIN
    ALTER TABLE dbo.Ventas ADD CajaSesionId INT NULL;
END;
GO

IF COL_LENGTH('dbo.Ventas', 'MetodoPago') IS NULL
BEGIN
    ALTER TABLE dbo.Ventas ADD MetodoPago NVARCHAR(30) NULL;
END;
GO

IF COL_LENGTH('dbo.Ventas', 'Notas') IS NULL
BEGIN
    ALTER TABLE dbo.Ventas ADD Notas NVARCHAR(250) NULL;
END;
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'Ventas', 'SubTotal', 'DF_Ventas_SubTotal', '(0)';
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'Ventas', 'Igv', 'DF_Ventas_Igv', '(0)';
GO

UPDATE dbo.Ventas
SET
    FechaVenta = ISNULL(FechaVenta, SYSDATETIME()),
    SubTotal = ISNULL(SubTotal, ROUND(ISNULL(Total, 0) / 1.18, 2)),
    Igv = ISNULL(Igv, ROUND(ISNULL(Total, 0) - ROUND(ISNULL(Total, 0) / 1.18, 2), 2)),
    Total = ISNULL(Total, 0),
    MetodoPago = ISNULL(MetodoPago, 'Efectivo')
WHERE
    FechaVenta IS NULL
    OR SubTotal IS NULL
    OR Igv IS NULL
    OR Total IS NULL
    OR MetodoPago IS NULL;
GO

ALTER TABLE dbo.Ventas ALTER COLUMN FechaVenta DATETIME2 NOT NULL;
ALTER TABLE dbo.Ventas ALTER COLUMN UsuarioId INT NOT NULL;
ALTER TABLE dbo.Ventas ALTER COLUMN CajaSesionId INT NULL;
ALTER TABLE dbo.Ventas ALTER COLUMN SubTotal DECIMAL(10,2) NOT NULL;
ALTER TABLE dbo.Ventas ALTER COLUMN Igv DECIMAL(10,2) NOT NULL;
ALTER TABLE dbo.Ventas ALTER COLUMN Total DECIMAL(10,2) NOT NULL;
ALTER TABLE dbo.Ventas ALTER COLUMN MetodoPago NVARCHAR(30) NOT NULL;
ALTER TABLE dbo.Ventas ALTER COLUMN Notas NVARCHAR(250) NULL;
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'Ventas', 'FechaVenta', 'DF_Ventas_FechaVenta', '(SYSDATETIME())';
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_Ventas_Usuarios'
)
BEGIN
    ALTER TABLE dbo.Ventas
    ADD CONSTRAINT FK_Ventas_Usuarios
        FOREIGN KEY (UsuarioId) REFERENCES dbo.Usuarios(Id);
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_Ventas_CajaSesiones'
)
BEGIN
    ALTER TABLE dbo.Ventas
    ADD CONSTRAINT FK_Ventas_CajaSesiones
        FOREIGN KEY (CajaSesionId) REFERENCES dbo.CajaSesiones(Id);
END;
GO

/* =========================================================
   DETALLE VENTA
   ========================================================= */
IF OBJECT_ID('dbo.DetalleVenta', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.DetalleVenta
    (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_DetalleVenta PRIMARY KEY,
        VentaId INT NOT NULL,
        ProductoId INT NOT NULL,
        Cantidad INT NOT NULL,
        PrecioUnitario DECIMAL(10,2) NOT NULL,
        Subtotal DECIMAL(10,2) NOT NULL
    );
END;
GO

IF COL_LENGTH('dbo.DetalleVenta', 'VentaId') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleVenta ADD VentaId INT NULL;
END;
GO

IF COL_LENGTH('dbo.DetalleVenta', 'ProductoId') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleVenta ADD ProductoId INT NULL;
END;
GO

IF COL_LENGTH('dbo.DetalleVenta', 'Cantidad') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleVenta ADD Cantidad INT NULL;
END;
GO

IF COL_LENGTH('dbo.DetalleVenta', 'PrecioUnitario') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleVenta ADD PrecioUnitario DECIMAL(10,2) NULL;
END;
GO

IF COL_LENGTH('dbo.DetalleVenta', 'Subtotal') IS NULL
BEGIN
    ALTER TABLE dbo.DetalleVenta ADD Subtotal DECIMAL(10,2) NULL;
END;
GO

UPDATE dbo.DetalleVenta
SET
    Cantidad = ISNULL(Cantidad, 0),
    PrecioUnitario = ISNULL(PrecioUnitario, 0),
    Subtotal = ISNULL(Subtotal, 0)
WHERE
    Cantidad IS NULL
    OR PrecioUnitario IS NULL
    OR Subtotal IS NULL;
GO

ALTER TABLE dbo.DetalleVenta ALTER COLUMN VentaId INT NOT NULL;
ALTER TABLE dbo.DetalleVenta ALTER COLUMN ProductoId INT NOT NULL;
ALTER TABLE dbo.DetalleVenta ALTER COLUMN Cantidad INT NOT NULL;
ALTER TABLE dbo.DetalleVenta ALTER COLUMN PrecioUnitario DECIMAL(10,2) NOT NULL;
ALTER TABLE dbo.DetalleVenta ALTER COLUMN Subtotal DECIMAL(10,2) NOT NULL;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_DetalleVenta_Ventas'
)
BEGIN
    ALTER TABLE dbo.DetalleVenta
    ADD CONSTRAINT FK_DetalleVenta_Ventas
        FOREIGN KEY (VentaId) REFERENCES dbo.Ventas(Id) ON DELETE CASCADE;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_DetalleVenta_Productos'
)
BEGIN
    ALTER TABLE dbo.DetalleVenta
    ADD CONSTRAINT FK_DetalleVenta_Productos
        FOREIGN KEY (ProductoId) REFERENCES dbo.Productos(Id);
END;
GO

/* =========================================================
   TRABAJOS DE IMPRESION
   ========================================================= */
IF OBJECT_ID('dbo.TrabajosImpresion', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.TrabajosImpresion
    (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_TrabajosImpresion PRIMARY KEY,
        VentaId INT NULL,
        TipoOrigen NVARCHAR(30) NOT NULL CONSTRAINT DF_TrabajosImpresion_TipoOrigen DEFAULT ('sale'),
        TipoDocumento NVARCHAR(20) NOT NULL CONSTRAINT DF_TrabajosImpresion_TipoDocumento DEFAULT ('ticket'),
        Estado NVARCHAR(20) NOT NULL CONSTRAINT DF_TrabajosImpresion_Estado DEFAULT ('pendiente'),
        Intentos INT NOT NULL CONSTRAINT DF_TrabajosImpresion_Intentos DEFAULT (0),
        NombreImpresora NVARCHAR(120) NULL,
        SolicitadoEn DATETIME2 NOT NULL CONSTRAINT DF_TrabajosImpresion_SolicitadoEn DEFAULT (SYSDATETIME()),
        ProcesandoEn DATETIME2 NULL,
        ProcesadoEn DATETIME2 NULL,
        UltimoError NVARCHAR(500) NULL,
        PayloadJson NVARCHAR(MAX) NOT NULL
    );
END;
GO

IF COL_LENGTH('dbo.TrabajosImpresion', 'VentaId') IS NULL
BEGIN
    ALTER TABLE dbo.TrabajosImpresion ADD VentaId INT NULL;
END;
GO

IF COL_LENGTH('dbo.TrabajosImpresion', 'TipoOrigen') IS NULL
BEGIN
    ALTER TABLE dbo.TrabajosImpresion ADD TipoOrigen NVARCHAR(30) NULL;
END;
GO

IF COL_LENGTH('dbo.TrabajosImpresion', 'TipoDocumento') IS NULL
BEGIN
    ALTER TABLE dbo.TrabajosImpresion ADD TipoDocumento NVARCHAR(20) NULL;
END;
GO

IF COL_LENGTH('dbo.TrabajosImpresion', 'Estado') IS NULL
BEGIN
    ALTER TABLE dbo.TrabajosImpresion ADD Estado NVARCHAR(20) NULL;
END;
GO

IF COL_LENGTH('dbo.TrabajosImpresion', 'Intentos') IS NULL
BEGIN
    ALTER TABLE dbo.TrabajosImpresion ADD Intentos INT NULL;
END;
GO

IF COL_LENGTH('dbo.TrabajosImpresion', 'NombreImpresora') IS NULL
BEGIN
    ALTER TABLE dbo.TrabajosImpresion ADD NombreImpresora NVARCHAR(120) NULL;
END;
GO

IF COL_LENGTH('dbo.TrabajosImpresion', 'SolicitadoEn') IS NULL
BEGIN
    ALTER TABLE dbo.TrabajosImpresion ADD SolicitadoEn DATETIME2 NULL;
END;
GO

IF COL_LENGTH('dbo.TrabajosImpresion', 'ProcesandoEn') IS NULL
BEGIN
    ALTER TABLE dbo.TrabajosImpresion ADD ProcesandoEn DATETIME2 NULL;
END;
GO

IF COL_LENGTH('dbo.TrabajosImpresion', 'ProcesadoEn') IS NULL
BEGIN
    ALTER TABLE dbo.TrabajosImpresion ADD ProcesadoEn DATETIME2 NULL;
END;
GO

IF COL_LENGTH('dbo.TrabajosImpresion', 'UltimoError') IS NULL
BEGIN
    ALTER TABLE dbo.TrabajosImpresion ADD UltimoError NVARCHAR(500) NULL;
END;
GO

IF COL_LENGTH('dbo.TrabajosImpresion', 'PayloadJson') IS NULL
BEGIN
    ALTER TABLE dbo.TrabajosImpresion ADD PayloadJson NVARCHAR(MAX) NULL;
END;
GO

UPDATE dbo.TrabajosImpresion
SET
    TipoOrigen = ISNULL(NULLIF(TipoOrigen, ''), 'sale'),
    TipoDocumento = ISNULL(NULLIF(TipoDocumento, ''), 'ticket'),
    Estado = ISNULL(NULLIF(Estado, ''), 'pendiente'),
    Intentos = ISNULL(Intentos, 0),
    SolicitadoEn = ISNULL(SolicitadoEn, SYSDATETIME()),
    PayloadJson = ISNULL(PayloadJson, N'{}')
WHERE
    TipoOrigen IS NULL
    OR TipoDocumento IS NULL
    OR Estado IS NULL
    OR Intentos IS NULL
    OR SolicitadoEn IS NULL
    OR PayloadJson IS NULL;
GO

ALTER TABLE dbo.TrabajosImpresion ALTER COLUMN VentaId INT NULL;
ALTER TABLE dbo.TrabajosImpresion ALTER COLUMN TipoOrigen NVARCHAR(30) NOT NULL;
ALTER TABLE dbo.TrabajosImpresion ALTER COLUMN TipoDocumento NVARCHAR(20) NOT NULL;
ALTER TABLE dbo.TrabajosImpresion ALTER COLUMN Estado NVARCHAR(20) NOT NULL;
ALTER TABLE dbo.TrabajosImpresion ALTER COLUMN Intentos INT NOT NULL;
ALTER TABLE dbo.TrabajosImpresion ALTER COLUMN NombreImpresora NVARCHAR(120) NULL;
ALTER TABLE dbo.TrabajosImpresion ALTER COLUMN SolicitadoEn DATETIME2 NOT NULL;
ALTER TABLE dbo.TrabajosImpresion ALTER COLUMN ProcesandoEn DATETIME2 NULL;
ALTER TABLE dbo.TrabajosImpresion ALTER COLUMN ProcesadoEn DATETIME2 NULL;
ALTER TABLE dbo.TrabajosImpresion ALTER COLUMN UltimoError NVARCHAR(500) NULL;
ALTER TABLE dbo.TrabajosImpresion ALTER COLUMN PayloadJson NVARCHAR(MAX) NOT NULL;
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'TrabajosImpresion', 'TipoOrigen', 'DF_TrabajosImpresion_TipoOrigen', '(''sale'')';
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'TrabajosImpresion', 'TipoDocumento', 'DF_TrabajosImpresion_TipoDocumento', '(''ticket'')';
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'TrabajosImpresion', 'Estado', 'DF_TrabajosImpresion_Estado', '(''pendiente'')';
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'TrabajosImpresion', 'Intentos', 'DF_TrabajosImpresion_Intentos', '(0)';
GO

EXEC dbo.usp_EnsureDefaultConstraint 'dbo', 'TrabajosImpresion', 'SolicitadoEn', 'DF_TrabajosImpresion_SolicitadoEn', '(SYSDATETIME())';
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_TrabajosImpresion_Ventas'
)
BEGIN
    ALTER TABLE dbo.TrabajosImpresion
    ADD CONSTRAINT FK_TrabajosImpresion_Ventas
        FOREIGN KEY (VentaId) REFERENCES dbo.Ventas(Id) ON DELETE CASCADE;
END;
GO

/* =========================================================
   CONFIGURACION EMPRESA
   ========================================================= */
IF OBJECT_ID('dbo.ConfiguracionEmpresa', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ConfiguracionEmpresa
    (
        Id              INT           NOT NULL CONSTRAINT PK_ConfiguracionEmpresa PRIMARY KEY,
        NombreComercial NVARCHAR(150) NOT NULL CONSTRAINT DF_ConfigEmpresa_NombreComercial DEFAULT ('Minimarket'),
        RazonSocial     NVARCHAR(200) NOT NULL CONSTRAINT DF_ConfigEmpresa_RazonSocial     DEFAULT ('Minimarket Casa'),
        Ruc             NVARCHAR(20)  NOT NULL CONSTRAINT DF_ConfigEmpresa_Ruc             DEFAULT ('RUC por definir'),
        Direccion       NVARCHAR(250) NOT NULL CONSTRAINT DF_ConfigEmpresa_Direccion       DEFAULT ('Direccion por definir'),
        Telefono        NVARCHAR(50)  NOT NULL CONSTRAINT DF_ConfigEmpresa_Telefono        DEFAULT ('Telefono por definir'),
        Eslogan         NVARCHAR(250) NOT NULL CONSTRAINT DF_ConfigEmpresa_Eslogan         DEFAULT ('Abarrotes y mas'),
        TituloDocumento NVARCHAR(100) NOT NULL CONSTRAINT DF_ConfigEmpresa_TituloDocumento DEFAULT ('Ticket de venta'),
        EtiquetaCliente NVARCHAR(100) NOT NULL CONSTRAINT DF_ConfigEmpresa_EtiquetaCliente DEFAULT ('Consumidor final'),
        PiePagina1      NVARCHAR(150) NOT NULL CONSTRAINT DF_ConfigEmpresa_PiePagina1      DEFAULT ('Gracias por su compra'),
        PiePagina2      NVARCHAR(150) NOT NULL CONSTRAINT DF_ConfigEmpresa_PiePagina2      DEFAULT ('Vuelva pronto')
    );
END;
GO

IF COL_LENGTH('dbo.ConfiguracionEmpresa', 'NombreComercial') IS NULL
BEGIN
    ALTER TABLE dbo.ConfiguracionEmpresa ADD NombreComercial NVARCHAR(150) NOT NULL
        CONSTRAINT DF_ConfigEmpresa_NombreComercial DEFAULT ('Minimarket');
END;
GO

IF COL_LENGTH('dbo.ConfiguracionEmpresa', 'RazonSocial') IS NULL
BEGIN
    ALTER TABLE dbo.ConfiguracionEmpresa ADD RazonSocial NVARCHAR(200) NOT NULL
        CONSTRAINT DF_ConfigEmpresa_RazonSocial DEFAULT ('Minimarket Casa');
END;
GO

IF COL_LENGTH('dbo.ConfiguracionEmpresa', 'Ruc') IS NULL
BEGIN
    ALTER TABLE dbo.ConfiguracionEmpresa ADD Ruc NVARCHAR(20) NOT NULL
        CONSTRAINT DF_ConfigEmpresa_Ruc DEFAULT ('RUC por definir');
END;
GO

IF COL_LENGTH('dbo.ConfiguracionEmpresa', 'Direccion') IS NULL
BEGIN
    ALTER TABLE dbo.ConfiguracionEmpresa ADD Direccion NVARCHAR(250) NOT NULL
        CONSTRAINT DF_ConfigEmpresa_Direccion DEFAULT ('Direccion por definir');
END;
GO

IF COL_LENGTH('dbo.ConfiguracionEmpresa', 'Telefono') IS NULL
BEGIN
    ALTER TABLE dbo.ConfiguracionEmpresa ADD Telefono NVARCHAR(50) NOT NULL
        CONSTRAINT DF_ConfigEmpresa_Telefono DEFAULT ('Telefono por definir');
END;
GO

IF COL_LENGTH('dbo.ConfiguracionEmpresa', 'Eslogan') IS NULL
BEGIN
    ALTER TABLE dbo.ConfiguracionEmpresa ADD Eslogan NVARCHAR(250) NOT NULL
        CONSTRAINT DF_ConfigEmpresa_Eslogan DEFAULT ('Abarrotes y mas');
END;
GO

IF COL_LENGTH('dbo.ConfiguracionEmpresa', 'TituloDocumento') IS NULL
BEGIN
    ALTER TABLE dbo.ConfiguracionEmpresa ADD TituloDocumento NVARCHAR(100) NOT NULL
        CONSTRAINT DF_ConfigEmpresa_TituloDocumento DEFAULT ('Ticket de venta');
END;
GO

IF COL_LENGTH('dbo.ConfiguracionEmpresa', 'EtiquetaCliente') IS NULL
BEGIN
    ALTER TABLE dbo.ConfiguracionEmpresa ADD EtiquetaCliente NVARCHAR(100) NOT NULL
        CONSTRAINT DF_ConfigEmpresa_EtiquetaCliente DEFAULT ('Consumidor final');
END;
GO

IF COL_LENGTH('dbo.ConfiguracionEmpresa', 'PiePagina1') IS NULL
BEGIN
    ALTER TABLE dbo.ConfiguracionEmpresa ADD PiePagina1 NVARCHAR(150) NOT NULL
        CONSTRAINT DF_ConfigEmpresa_PiePagina1 DEFAULT ('Gracias por su compra');
END;
GO

IF COL_LENGTH('dbo.ConfiguracionEmpresa', 'PiePagina2') IS NULL
BEGIN
    ALTER TABLE dbo.ConfiguracionEmpresa ADD PiePagina2 NVARCHAR(150) NOT NULL
        CONSTRAINT DF_ConfigEmpresa_PiePagina2 DEFAULT ('Vuelva pronto');
END;
GO

/* =========================================================
   DATOS BASE
   ========================================================= */
IF NOT EXISTS (SELECT 1 FROM dbo.Categorias WHERE Nombre = 'Abarrotes')
BEGIN
    INSERT INTO dbo.Categorias (Nombre, Descripcion, Activo)
    VALUES ('Abarrotes', 'Productos de uso diario', 1);
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Categorias WHERE Nombre = 'Bebidas')
BEGIN
    INSERT INTO dbo.Categorias (Nombre, Descripcion, Activo)
    VALUES ('Bebidas', 'Gaseosas, aguas y jugos', 1);
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Categorias WHERE Nombre = 'Limpieza')
BEGIN
    INSERT INTO dbo.Categorias (Nombre, Descripcion, Activo)
    VALUES ('Limpieza', 'Articulos de limpieza', 1);
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Usuarios WHERE Username = 'admin')
BEGIN
    INSERT INTO dbo.Usuarios (NombreCompleto, Username, PasswordHash, Rol, Activo)
    VALUES ('Administrador General', 'admin', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'admin', 1);
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Usuarios WHERE Username = 'cajero')
BEGIN
    INSERT INTO dbo.Usuarios (NombreCompleto, Username, PasswordHash, Rol, Activo)
    VALUES ('Caja Principal', 'cajero', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'cajero', 1);
END;
GO

/* =========================================================
   VISTA DE REPORTE
   ========================================================= */
CREATE OR ALTER VIEW dbo.vw_ReporteVentasDiarias
AS
SELECT
    CAST(FechaVenta AS DATE) AS Fecha,
    COUNT(*) AS CantidadVentas,
    SUM(Total) AS TotalVendido
FROM dbo.Ventas
GROUP BY CAST(FechaVenta AS DATE);
GO

IF NOT EXISTS (SELECT 1 FROM dbo.ConfiguracionEmpresa WHERE Id = 1)
BEGIN
    INSERT INTO dbo.ConfiguracionEmpresa
        (Id, NombreComercial, RazonSocial, Ruc, Direccion, Telefono, Eslogan, TituloDocumento, EtiquetaCliente, PiePagina1, PiePagina2)
    VALUES
        (1, 'Minimarket', 'Minimarket Casa', 'RUC por definir', 'Direccion por definir', 'Telefono por definir',
         'Abarrotes y mas', 'Ticket de venta', 'Consumidor final', 'Gracias por su compra', 'Vuelva pronto');
END;
GO

PRINT 'Actualizacion segura de MinimarketDb finalizada correctamente.';
GO
