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
    FROM sys.default_constraints
    WHERE name = 'DF_Productos_StockMinimo'
)
BEGIN
    ALTER TABLE dbo.Productos
    ADD CONSTRAINT DF_Productos_StockMinimo DEFAULT (5) FOR StockMinimo;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.default_constraints
    WHERE name = 'DF_Productos_Costo'
)
BEGIN
    ALTER TABLE dbo.Productos
    ADD CONSTRAINT DF_Productos_Costo DEFAULT (0) FOR Costo;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.default_constraints
    WHERE name = 'DF_Productos_UnidadVenta'
)
BEGIN
    ALTER TABLE dbo.Productos
    ADD CONSTRAINT DF_Productos_UnidadVenta DEFAULT ('unidad') FOR UnidadVenta;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.default_constraints
    WHERE name = 'DF_Productos_UnidadCompra'
)
BEGIN
    ALTER TABLE dbo.Productos
    ADD CONSTRAINT DF_Productos_UnidadCompra DEFAULT ('unidad') FOR UnidadCompra;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.default_constraints
    WHERE name = 'DF_Productos_UnidadesPorCompra'
)
BEGIN
    ALTER TABLE dbo.Productos
    ADD CONSTRAINT DF_Productos_UnidadesPorCompra DEFAULT (1) FOR UnidadesPorCompra;
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

IF COL_LENGTH('dbo.Compras', 'Total') IS NULL
BEGIN
    ALTER TABLE dbo.Compras ADD Total DECIMAL(12,2) NULL;
END;
GO

UPDATE dbo.Compras
SET
    FechaCompra = ISNULL(FechaCompra, SYSDATETIME()),
    ProveedorId = ISNULL(ProveedorId, 1),
    UsuarioId = ISNULL(UsuarioId, 1),
    Total = ISNULL(Total, 0)
WHERE FechaCompra IS NULL OR ProveedorId IS NULL OR UsuarioId IS NULL OR Total IS NULL;
GO

ALTER TABLE dbo.Compras ALTER COLUMN FechaCompra DATETIME2 NOT NULL;
ALTER TABLE dbo.Compras ALTER COLUMN ProveedorId INT NOT NULL;
ALTER TABLE dbo.Compras ALTER COLUMN UsuarioId INT NOT NULL;
ALTER TABLE dbo.Compras ALTER COLUMN NumeroComprobante NVARCHAR(50) NULL;
ALTER TABLE dbo.Compras ALTER COLUMN Notas NVARCHAR(250) NULL;
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
    VALUES ('Administrador General', 'admin', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'admin', 1);
END;
ELSE
BEGIN
    UPDATE dbo.Usuarios
    SET PasswordHash = 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3',
        Activo = 1
    WHERE Username = 'admin';
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Usuarios WHERE Username = 'cajero')
BEGIN
    INSERT INTO dbo.Usuarios (NombreCompleto, Username, PasswordHash, Rol, Activo)
    VALUES ('Caja Principal', 'cajero', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'cajero', 1);
END;
ELSE
BEGIN
    UPDATE dbo.Usuarios
    SET PasswordHash = 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3',
        Activo = 1
    WHERE Username = 'cajero';
END;
GO

DECLARE @CategoriaAbarrotesId INT = (SELECT TOP 1 Id FROM dbo.Categorias WHERE Nombre = 'Abarrotes');
DECLARE @CategoriaBebidasId INT = (SELECT TOP 1 Id FROM dbo.Categorias WHERE Nombre = 'Bebidas');
DECLARE @CategoriaLimpiezaId INT = (SELECT TOP 1 Id FROM dbo.Categorias WHERE Nombre = 'Limpieza');
DECLARE @ProveedorInicialId INT = (SELECT TOP 1 Id FROM dbo.Proveedores WHERE Nombre = 'Distribuidora Central');

IF @ProveedorInicialId IS NULL
BEGIN
    INSERT INTO dbo.Proveedores (Nombre, NumeroDocumento, NombreContacto, Telefono, Correo, Direccion, Notas, Activo)
    VALUES ('Distribuidora Central', '20601234567', 'Rosa Medina', '987654321', 'ventas@distribuidoracentral.pe', 'Av. Principal 123', 'Proveedor inicial de referencia', 1);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Productos WHERE Sku = 'ABR-001')
BEGIN
    INSERT INTO dbo.Productos (Nombre, Sku, CodigoBarras, CodigoBarrasCompra, Descripcion, Precio, Costo, Stock, StockMinimo, FechaCaducidad, UnidadVenta, UnidadCompra, UnidadesPorCompra, Activo, CategoriaId)
    VALUES ('Arroz Superior 1Kg', 'ABR-001', '7750000000011', '7750000000012', 'Bolsa de arroz blanco', 4.50, 3.60, 80, 5, NULL, 'unidad', 'fardo', 12, 1, @CategoriaAbarrotesId);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Productos WHERE Sku = 'ABR-002')
BEGIN
    INSERT INTO dbo.Productos (Nombre, Sku, CodigoBarras, CodigoBarrasCompra, Descripcion, Precio, Costo, Stock, StockMinimo, FechaCaducidad, UnidadVenta, UnidadCompra, UnidadesPorCompra, Activo, CategoriaId)
    VALUES ('Azucar Rubia 1Kg', 'ABR-002', '7750000000021', '7750000000022', 'Azucar rubia embolsada', 4.20, 3.30, 60, 5, NULL, 'unidad', 'fardo', 10, 1, @CategoriaAbarrotesId);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Productos WHERE Sku = 'BEB-001')
BEGIN
    INSERT INTO dbo.Productos (Nombre, Sku, CodigoBarras, CodigoBarrasCompra, Descripcion, Precio, Costo, Stock, StockMinimo, FechaCaducidad, UnidadVenta, UnidadCompra, UnidadesPorCompra, Activo, CategoriaId)
    VALUES ('Gaseosa Cola 3L', 'BEB-001', '7750000000031', '7750000000032', 'Botella retornable', 9.80, 7.20, 30, 5, NULL, 'botella', 'jaba', 12, 1, @CategoriaBebidasId);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Productos WHERE Sku = 'BEB-002')
BEGIN
    INSERT INTO dbo.Productos (Nombre, Sku, CodigoBarras, CodigoBarrasCompra, Descripcion, Precio, Costo, Stock, StockMinimo, FechaCaducidad, UnidadVenta, UnidadCompra, UnidadesPorCompra, Activo, CategoriaId)
    VALUES ('Agua Mineral 625ml', 'BEB-002', '7750000000041', '7750000000042', 'Botella personal', 2.50, 1.40, 48, 5, NULL, 'botella', 'jaba', 24, 1, @CategoriaBebidasId);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Productos WHERE Sku = 'LIM-001')
BEGIN
    INSERT INTO dbo.Productos (Nombre, Sku, CodigoBarras, CodigoBarrasCompra, Descripcion, Precio, Costo, Stock, StockMinimo, FechaCaducidad, UnidadVenta, UnidadCompra, UnidadesPorCompra, Activo, CategoriaId)
    VALUES ('Detergente Floral 900g', 'LIM-001', '7750000000051', '7750000000052', 'Detergente en polvo', 8.90, 6.80, 22, 5, NULL, 'unidad', 'caja', 12, 1, @CategoriaLimpiezaId);
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
