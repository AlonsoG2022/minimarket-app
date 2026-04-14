IF DB_ID('MinimarketDb') IS NULL
BEGIN
    CREATE DATABASE MinimarketDb;
END;
GO

USE MinimarketDb;
GO

IF OBJECT_ID('dbo.DetalleVenta', 'U') IS NOT NULL DROP TABLE dbo.DetalleVenta;
IF OBJECT_ID('dbo.Ventas', 'U') IS NOT NULL DROP TABLE dbo.Ventas;
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
    Descripcion NVARCHAR(250) NULL,
    Precio DECIMAL(10,2) NOT NULL,
    Stock INT NOT NULL,
    StockMinimo INT NOT NULL,
    Activo BIT NOT NULL DEFAULT 1,
    CategoriaId INT NOT NULL,
    CONSTRAINT UQ_Productos_Sku UNIQUE (Sku),
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

CREATE TABLE dbo.Ventas
(
    Id INT IDENTITY(1,1) PRIMARY KEY,
    FechaVenta DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UsuarioId INT NOT NULL,
    Total DECIMAL(10,2) NOT NULL,
    MetodoPago NVARCHAR(30) NOT NULL,
    Notas NVARCHAR(250) NULL,
    CONSTRAINT FK_Ventas_Usuarios FOREIGN KEY (UsuarioId) REFERENCES dbo.Usuarios(Id)
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

INSERT INTO dbo.Categorias (Nombre, Descripcion, Activo)
VALUES
('Abarrotes', 'Productos de uso diario', 1),
('Bebidas', 'Gaseosas, aguas y jugos', 1),
('Limpieza', 'Articulos de limpieza', 1);
GO

INSERT INTO dbo.Productos (Nombre, Sku, Descripcion, Precio, Stock, StockMinimo, Activo, CategoriaId)
VALUES
('Arroz Superior 1Kg', 'ABR-001', 'Bolsa de arroz blanco', 4.50, 80, 20, 1, 1),
('Azucar Rubia 1Kg', 'ABR-002', 'Azucar rubia embolsada', 4.20, 60, 15, 1, 1),
('Gaseosa Cola 3L', 'BEB-001', 'Botella retornable', 9.80, 30, 10, 1, 2),
('Agua Mineral 625ml', 'BEB-002', 'Botella personal', 2.50, 48, 12, 1, 2),
('Detergente Floral 900g', 'LIM-001', 'Detergente en polvo', 8.90, 22, 8, 1, 3);
GO

INSERT INTO dbo.Usuarios (NombreCompleto, Username, PasswordHash, Rol, Activo)
VALUES
('Administrador General', 'admin', '3eb3fe66b31e3b4d10fa70b5cad49c7112294af6ae4e476a1c405155d45aa121', 'admin', 1),
('Caja Principal', 'cajero', '3eaf7440a5899bf27c390f75b4bbf3be0fc200cc3d5908f01e210d39f3995b18', 'cajero', 1);
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
