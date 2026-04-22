/*
    Script de actualizacion segura para MinimarketDb
    ------------------------------------------------
    Objetivo:
    - NO elimina tablas ni datos existentes.
    - Crea la base si no existe.
    - Crea tablas si no existen.
    - Agrega columnas faltantes con ALTER TABLE.
    - Ajusta tipos / nulabilidad de columnas actuales.
    - Crea restricciones, indices y llaves foraneas si faltan.
    - Inserta datos base solo si no existen.

    Recomendacion:
    - Ejecutar primero en QA o en una copia de respaldo.
    - Guardar este script como base para futuras evoluciones del esquema.
*/

IF DB_ID('MinimarketDb') IS NULL
BEGIN
    CREATE DATABASE MinimarketDb;
END;
GO

USE MinimarketDb;
GO

SET NOCOUNT ON;
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

IF NOT EXISTS
(
    SELECT 1
    FROM sys.default_constraints
    WHERE name = 'DF_Categorias_Activo'
)
BEGIN
    ALTER TABLE dbo.Categorias
    ADD CONSTRAINT DF_Categorias_Activo DEFAULT (1) FOR Activo;
END;
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
        Descripcion NVARCHAR(250) NULL,
        Precio DECIMAL(10,2) NOT NULL,
        Stock INT NOT NULL,
        StockMinimo INT NOT NULL,
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
    Stock = ISNULL(Stock, 0),
    StockMinimo = ISNULL(StockMinimo, 0),
    Activo = ISNULL(Activo, 1),
    CategoriaId = ISNULL(CategoriaId, 1)
WHERE
    Nombre IS NULL
    OR Sku IS NULL
    OR Precio IS NULL
    OR Stock IS NULL
    OR StockMinimo IS NULL
    OR Activo IS NULL
    OR CategoriaId IS NULL;
GO

ALTER TABLE dbo.Productos ALTER COLUMN Nombre NVARCHAR(150) NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN Sku NVARCHAR(30) NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN Descripcion NVARCHAR(250) NULL;
ALTER TABLE dbo.Productos ALTER COLUMN Precio DECIMAL(10,2) NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN Stock INT NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN StockMinimo INT NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN Activo BIT NOT NULL;
ALTER TABLE dbo.Productos ALTER COLUMN CategoriaId INT NOT NULL;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.default_constraints
    WHERE name = 'DF_Productos_Activo'
)
BEGIN
    ALTER TABLE dbo.Productos
    ADD CONSTRAINT DF_Productos_Activo DEFAULT (1) FOR Activo;
END;
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

IF NOT EXISTS
(
    SELECT 1
    FROM sys.default_constraints
    WHERE name = 'DF_Usuarios_Activo'
)
BEGIN
    ALTER TABLE dbo.Usuarios
    ADD CONSTRAINT DF_Usuarios_Activo DEFAULT (1) FOR Activo;
END;
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
   VENTAS
   ========================================================= */
IF OBJECT_ID('dbo.Ventas', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Ventas
    (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Ventas PRIMARY KEY,
        FechaVenta DATETIME2 NOT NULL CONSTRAINT DF_Ventas_FechaVenta DEFAULT (SYSDATETIME()),
        UsuarioId INT NOT NULL,
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

IF COL_LENGTH('dbo.Ventas', 'Total') IS NULL
BEGIN
    ALTER TABLE dbo.Ventas ADD Total DECIMAL(10,2) NULL;
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

UPDATE dbo.Ventas
SET
    FechaVenta = ISNULL(FechaVenta, SYSDATETIME()),
    Total = ISNULL(Total, 0),
    MetodoPago = ISNULL(MetodoPago, 'Efectivo')
WHERE
    FechaVenta IS NULL
    OR Total IS NULL
    OR MetodoPago IS NULL;
GO

ALTER TABLE dbo.Ventas ALTER COLUMN FechaVenta DATETIME2 NOT NULL;
ALTER TABLE dbo.Ventas ALTER COLUMN UsuarioId INT NOT NULL;
ALTER TABLE dbo.Ventas ALTER COLUMN Total DECIMAL(10,2) NOT NULL;
ALTER TABLE dbo.Ventas ALTER COLUMN MetodoPago NVARCHAR(30) NOT NULL;
ALTER TABLE dbo.Ventas ALTER COLUMN Notas NVARCHAR(250) NULL;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.default_constraints
    WHERE name = 'DF_Ventas_FechaVenta'
)
BEGIN
    ALTER TABLE dbo.Ventas
    ADD CONSTRAINT DF_Ventas_FechaVenta DEFAULT (SYSDATETIME()) FOR FechaVenta;
END;
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
    VALUES ('Administrador General', 'admin', '3eb3fe66b31e3b4d10fa70b5cad49c7112294af6ae4e476a1c405155d45aa121', 'admin', 1);
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Usuarios WHERE Username = 'cajero')
BEGIN
    INSERT INTO dbo.Usuarios (NombreCompleto, Username, PasswordHash, Rol, Activo)
    VALUES ('Caja Principal', 'cajero', '3eaf7440a5899bf27c390f75b4bbf3be0fc200cc3d5908f01e210d39f3995b18', 'cajero', 1);
END;
GO

DECLARE @CategoriaAbarrotesId INT = (SELECT TOP 1 Id FROM dbo.Categorias WHERE Nombre = 'Abarrotes');
DECLARE @CategoriaBebidasId INT = (SELECT TOP 1 Id FROM dbo.Categorias WHERE Nombre = 'Bebidas');
DECLARE @CategoriaLimpiezaId INT = (SELECT TOP 1 Id FROM dbo.Categorias WHERE Nombre = 'Limpieza');

IF NOT EXISTS (SELECT 1 FROM dbo.Productos WHERE Sku = 'ABR-001')
BEGIN
    INSERT INTO dbo.Productos (Nombre, Sku, Descripcion, Precio, Stock, StockMinimo, Activo, CategoriaId)
    VALUES ('Arroz Superior 1Kg', 'ABR-001', 'Bolsa de arroz blanco', 4.50, 80, 20, 1, @CategoriaAbarrotesId);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Productos WHERE Sku = 'ABR-002')
BEGIN
    INSERT INTO dbo.Productos (Nombre, Sku, Descripcion, Precio, Stock, StockMinimo, Activo, CategoriaId)
    VALUES ('Azucar Rubia 1Kg', 'ABR-002', 'Azucar rubia embolsada', 4.20, 60, 15, 1, @CategoriaAbarrotesId);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Productos WHERE Sku = 'BEB-001')
BEGIN
    INSERT INTO dbo.Productos (Nombre, Sku, Descripcion, Precio, Stock, StockMinimo, Activo, CategoriaId)
    VALUES ('Gaseosa Cola 3L', 'BEB-001', 'Botella retornable', 9.80, 30, 10, 1, @CategoriaBebidasId);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Productos WHERE Sku = 'BEB-002')
BEGIN
    INSERT INTO dbo.Productos (Nombre, Sku, Descripcion, Precio, Stock, StockMinimo, Activo, CategoriaId)
    VALUES ('Agua Mineral 625ml', 'BEB-002', 'Botella personal', 2.50, 48, 12, 1, @CategoriaBebidasId);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Productos WHERE Sku = 'LIM-001')
BEGIN
    INSERT INTO dbo.Productos (Nombre, Sku, Descripcion, Precio, Stock, StockMinimo, Activo, CategoriaId)
    VALUES ('Detergente Floral 900g', 'LIM-001', 'Detergente en polvo', 8.90, 22, 8, 1, @CategoriaLimpiezaId);
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

PRINT 'Actualizacion segura de MinimarketDb finalizada correctamente.';
GO
