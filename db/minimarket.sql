/*
    Script destructivo de reseteo para desarrollo
    ---------------------------------------------
    Uso recomendado:
    - Solo para desarrollo cuando quieras recrear toda la base desde cero.
    - NO usar en produccion.

    Para instalaciones y upgrades normales usa:
    - `db/minimarket.safe-upgrade.sql`
*/

IF DB_ID('MinimarketDb') IS NULL
BEGIN
    CREATE DATABASE MinimarketDb;
END;
GO

USE MinimarketDb;
GO

IF OBJECT_ID('dbo.DetalleVenta', 'U') IS NOT NULL DROP TABLE dbo.DetalleVenta;
IF OBJECT_ID('dbo.TrabajosImpresion', 'U') IS NOT NULL DROP TABLE dbo.TrabajosImpresion;
IF OBJECT_ID('dbo.Ventas', 'U') IS NOT NULL DROP TABLE dbo.Ventas;
IF OBJECT_ID('dbo.CajaMovimientos', 'U') IS NOT NULL DROP TABLE dbo.CajaMovimientos;
IF OBJECT_ID('dbo.CajaSesiones', 'U') IS NOT NULL DROP TABLE dbo.CajaSesiones;
IF OBJECT_ID('dbo.DetalleCompra', 'U') IS NOT NULL DROP TABLE dbo.DetalleCompra;
IF OBJECT_ID('dbo.Compras', 'U') IS NOT NULL DROP TABLE dbo.Compras;
IF OBJECT_ID('dbo.Proveedores', 'U') IS NOT NULL DROP TABLE dbo.Proveedores;
IF OBJECT_ID('dbo.Productos', 'U') IS NOT NULL DROP TABLE dbo.Productos;
IF OBJECT_ID('dbo.Usuarios', 'U') IS NOT NULL DROP TABLE dbo.Usuarios;
IF OBJECT_ID('dbo.Categorias', 'U') IS NOT NULL DROP TABLE dbo.Categorias;
GO

CREATE TABLE dbo.Categorias
(
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Nombre NVARCHAR(100) NOT NULL,
    Descripcion NVARCHAR(250) NULL,
    Activo BIT NOT NULL DEFAULT 1
);
GO

CREATE TABLE dbo.Productos
(
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Nombre NVARCHAR(150) NOT NULL,
    Sku NVARCHAR(30) NOT NULL,
    CodigoBarras NVARCHAR(50) NULL,
    CodigoBarrasCompra NVARCHAR(50) NULL,
    Descripcion NVARCHAR(250) NULL,
    Precio DECIMAL(10,2) NOT NULL,
    Costo DECIMAL(10,2) NOT NULL DEFAULT 0,
    Stock INT NOT NULL,
    StockMinimo INT NOT NULL DEFAULT 5,
    FechaCaducidad DATE NULL,
    UnidadVenta NVARCHAR(30) NOT NULL DEFAULT 'unidad',
    UnidadCompra NVARCHAR(30) NOT NULL DEFAULT 'unidad',
    UnidadesPorCompra INT NOT NULL DEFAULT 1,
    Activo BIT NOT NULL DEFAULT 1,
    CategoriaId INT NOT NULL,
    CONSTRAINT UQ_Productos_Sku UNIQUE (Sku),
    CONSTRAINT UQ_Productos_CodigoBarras UNIQUE (CodigoBarras),
    CONSTRAINT UQ_Productos_CodigoBarrasCompra UNIQUE (CodigoBarrasCompra),
    CONSTRAINT FK_Productos_Categorias FOREIGN KEY (CategoriaId) REFERENCES dbo.Categorias(Id)
);
GO

CREATE TABLE dbo.Usuarios
(
    Id INT IDENTITY(1,1) PRIMARY KEY,
    NombreCompleto NVARCHAR(120) NOT NULL,
    Username NVARCHAR(50) NOT NULL,
    PasswordHash NVARCHAR(256) NOT NULL,
    Rol NVARCHAR(20) NOT NULL,
    Activo BIT NOT NULL DEFAULT 1,
    CONSTRAINT UQ_Usuarios_Username UNIQUE (Username)
);
GO

CREATE TABLE dbo.Proveedores
(
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Nombre NVARCHAR(150) NOT NULL,
    NumeroDocumento NVARCHAR(30) NULL,
    NombreContacto NVARCHAR(120) NULL,
    Telefono NVARCHAR(30) NULL,
    Correo NVARCHAR(120) NULL,
    Direccion NVARCHAR(250) NULL,
    Notas NVARCHAR(250) NULL,
    Activo BIT NOT NULL DEFAULT 1
);
GO

CREATE TABLE dbo.CajaSesiones
(
    Id INT IDENTITY(1,1) PRIMARY KEY,
    UsuarioId INT NOT NULL,
    FechaApertura DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    FechaCierre DATETIME2 NULL,
    MontoInicial DECIMAL(10,2) NOT NULL,
    MontoEsperadoCierre DECIMAL(10,2) NULL,
    MontoContadoCierre DECIMAL(10,2) NULL,
    Diferencia DECIMAL(10,2) NULL,
    Estado NVARCHAR(20) NOT NULL DEFAULT 'abierta',
    Notas NVARCHAR(250) NULL,
    CONSTRAINT FK_CajaSesiones_Usuarios FOREIGN KEY (UsuarioId) REFERENCES dbo.Usuarios(Id)
);
GO

CREATE TABLE dbo.CajaMovimientos
(
    Id INT IDENTITY(1,1) PRIMARY KEY,
    CajaSesionId INT NOT NULL,
    FechaMovimiento DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    Tipo NVARCHAR(30) NOT NULL,
    Monto DECIMAL(10,2) NOT NULL,
    Descripcion NVARCHAR(250) NULL,
    TipoReferencia NVARCHAR(30) NULL,
    ReferenciaId INT NULL,
    CONSTRAINT FK_CajaMovimientos_CajaSesiones FOREIGN KEY (CajaSesionId) REFERENCES dbo.CajaSesiones(Id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.Ventas
(
    Id INT IDENTITY(1,1) PRIMARY KEY,
    FechaVenta DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UsuarioId INT NOT NULL,
    CajaSesionId INT NULL,
    Total DECIMAL(10,2) NOT NULL,
    MetodoPago NVARCHAR(30) NOT NULL,
    Notas NVARCHAR(250) NULL,
    CONSTRAINT FK_Ventas_Usuarios FOREIGN KEY (UsuarioId) REFERENCES dbo.Usuarios(Id),
    CONSTRAINT FK_Ventas_CajaSesiones FOREIGN KEY (CajaSesionId) REFERENCES dbo.CajaSesiones(Id)
);
GO

CREATE TABLE dbo.DetalleVenta
(
    Id INT IDENTITY(1,1) PRIMARY KEY,
    VentaId INT NOT NULL,
    ProductoId INT NOT NULL,
    Cantidad INT NOT NULL,
    PrecioUnitario DECIMAL(10,2) NOT NULL,
    Subtotal DECIMAL(10,2) NOT NULL,
    CONSTRAINT FK_DetalleVenta_Ventas FOREIGN KEY (VentaId) REFERENCES dbo.Ventas(Id) ON DELETE CASCADE,
    CONSTRAINT FK_DetalleVenta_Productos FOREIGN KEY (ProductoId) REFERENCES dbo.Productos(Id)
);
GO

CREATE TABLE dbo.TrabajosImpresion
(
    Id INT IDENTITY(1,1) PRIMARY KEY,
    VentaId INT NULL,
    TipoOrigen NVARCHAR(30) NOT NULL CONSTRAINT DF_TrabajosImpresion_TipoOrigen DEFAULT ('sale'),
    TipoDocumento NVARCHAR(20) NOT NULL CONSTRAINT DF_TrabajosImpresion_TipoDocumento DEFAULT ('ticket'),
    Estado NVARCHAR(20) NOT NULL CONSTRAINT DF_TrabajosImpresion_Estado DEFAULT ('pendiente'),
    Intentos INT NOT NULL CONSTRAINT DF_TrabajosImpresion_Intentos DEFAULT (0),
    NombreImpresora NVARCHAR(120) NULL,
    SolicitadoEn DATETIME2 NOT NULL CONSTRAINT DF_TrabajosImpresion_SolicitadoEn DEFAULT SYSDATETIME(),
    ProcesandoEn DATETIME2 NULL,
    ProcesadoEn DATETIME2 NULL,
    UltimoError NVARCHAR(500) NULL,
    PayloadJson NVARCHAR(MAX) NOT NULL,
    CONSTRAINT FK_TrabajosImpresion_Ventas FOREIGN KEY (VentaId) REFERENCES dbo.Ventas(Id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.Compras
(
    Id INT IDENTITY(1,1) PRIMARY KEY,
    FechaCompra DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    ProveedorId INT NOT NULL,
    UsuarioId INT NOT NULL,
    NumeroComprobante NVARCHAR(50) NULL,
    Notas NVARCHAR(250) NULL,
    Total DECIMAL(12,2) NOT NULL,
    CONSTRAINT FK_Compras_Proveedores FOREIGN KEY (ProveedorId) REFERENCES dbo.Proveedores(Id),
    CONSTRAINT FK_Compras_Usuarios FOREIGN KEY (UsuarioId) REFERENCES dbo.Usuarios(Id)
);
GO

CREATE TABLE dbo.DetalleCompra
(
    Id INT IDENTITY(1,1) PRIMARY KEY,
    CompraId INT NOT NULL,
    ProductoId INT NOT NULL,
    CantidadPaquetes INT NOT NULL,
    UnidadesPorPaquete INT NOT NULL,
    TotalUnidades INT NOT NULL,
    CostoPaquete DECIMAL(10,2) NOT NULL,
    CostoUnitario DECIMAL(10,2) NOT NULL,
    Subtotal DECIMAL(12,2) NOT NULL,
    UnidadCompra NVARCHAR(30) NOT NULL,
    CodigoBarrasLeido NVARCHAR(50) NULL,
    CONSTRAINT FK_DetalleCompra_Compras FOREIGN KEY (CompraId) REFERENCES dbo.Compras(Id) ON DELETE CASCADE,
    CONSTRAINT FK_DetalleCompra_Productos FOREIGN KEY (ProductoId) REFERENCES dbo.Productos(Id)
);
GO

INSERT INTO dbo.Categorias (Nombre, Descripcion, Activo)
VALUES
('Abarrotes', 'Productos de uso diario', 1),
('Bebidas', 'Gaseosas, aguas y jugos', 1),
('Limpieza', 'Articulos de limpieza', 1);
GO

INSERT INTO dbo.Productos (Nombre, Sku, CodigoBarras, CodigoBarrasCompra, Descripcion, Precio, Costo, Stock, StockMinimo, FechaCaducidad, UnidadVenta, UnidadCompra, UnidadesPorCompra, Activo, CategoriaId)
VALUES
('Arroz Superior 1Kg', 'ABR-001', '7750000000011', '7750000000012', 'Bolsa de arroz blanco', 4.50, 3.60, 80, 5, NULL, 'unidad', 'fardo', 12, 1, 1),
('Azucar Rubia 1Kg', 'ABR-002', '7750000000021', '7750000000022', 'Azucar rubia embolsada', 4.20, 3.30, 60, 5, NULL, 'unidad', 'fardo', 10, 1, 1),
('Gaseosa Cola 3L', 'BEB-001', '7750000000031', '7750000000032', 'Botella retornable', 9.80, 7.20, 30, 5, NULL, 'botella', 'jaba', 12, 1, 2),
('Agua Mineral 625ml', 'BEB-002', '7750000000041', '7750000000042', 'Botella personal', 2.50, 1.40, 48, 5, NULL, 'botella', 'jaba', 24, 1, 2),
('Detergente Floral 900g', 'LIM-001', '7750000000051', '7750000000052', 'Detergente en polvo', 8.90, 6.80, 22, 5, NULL, 'unidad', 'caja', 12, 1, 3);
GO

INSERT INTO dbo.Usuarios (NombreCompleto, Username, PasswordHash, Rol, Activo)
VALUES
('Administrador General', 'admin', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'admin', 1),
('Caja Principal', 'cajero', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'cajero', 1);
GO

INSERT INTO dbo.Proveedores (Nombre, NumeroDocumento, NombreContacto, Telefono, Correo, Direccion, Notas, Activo)
VALUES
('Distribuidora Central', '20601234567', 'Rosa Medina', '987654321', 'ventas@distribuidoracentral.pe', 'Av. Principal 123', 'Proveedor inicial de referencia', 1);
GO

INSERT INTO dbo.Ventas (FechaVenta, UsuarioId, Total, MetodoPago, Notas)
VALUES
(DATEADD(DAY, -2, SYSDATETIME()), 2, 22.90, 'Efectivo', 'Venta de prueba'),
(DATEADD(DAY, -1, SYSDATETIME()), 2, 16.50, 'Yape', 'Venta de prueba');
GO

INSERT INTO dbo.DetalleVenta (VentaId, ProductoId, Cantidad, PrecioUnitario, Subtotal)
VALUES
(1, 1, 2, 4.50, 9.00),
(1, 4, 2, 2.50, 5.00),
(1, 5, 1, 8.90, 8.90),
(2, 2, 1, 4.20, 4.20),
(2, 3, 1, 9.80, 9.80),
(2, 4, 1, 2.50, 2.50);
GO

UPDATE p
SET p.Stock = p.Stock - v.TotalCantidad
FROM dbo.Productos p
INNER JOIN
(
    SELECT ProductoId, SUM(Cantidad) AS TotalCantidad
    FROM dbo.DetalleVenta
    GROUP BY ProductoId
) v ON v.ProductoId = p.Id;
GO

CREATE OR ALTER VIEW dbo.vw_ReporteVentasDiarias
AS
SELECT
    CAST(FechaVenta AS DATE) AS Fecha,
    COUNT(*) AS CantidadVentas,
    SUM(Total) AS TotalVendido
FROM dbo.Ventas
GROUP BY CAST(FechaVenta AS DATE);
GO

SELECT * FROM dbo.vw_ReporteVentasDiarias ORDER BY Fecha;
GO
