/*
    ================================================================
    LIMPIEZA DE DATOS - Minimarket
    ================================================================
    Deja la base lista para: 1) sincronizar el catalogo del proveedor
    y 2) importar tu Excel, sin productos repetidos.

    BORRA:
      - Ventas y su detalle, trabajos de impresion
      - Compras y su detalle
      - Caja (sesiones y movimientos)
      - Historico ProveedorProducto
      - Productos
      - Categorias

    CONSERVA (NO se tocan):
      - Proveedores  (incluido Coca-Cola)
      - Usuarios     (para poder seguir entrando a la app)
      - ConfiguracionEmpresa (datos de empresa, tema, etc.)

    Si tambien quieres borrar Usuarios o Configuracion, descomenta
    las lineas marcadas al final (NO recomendado).

    *** ES DESTRUCTIVO: borra datos. Haz respaldo si dudas. ***
    Ejecutar con sqlcmd o SSMS sobre MinimarketDb.
    ================================================================
*/

USE MinimarketDb;
GO

SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    -- Orden hijo -> padre para respetar las llaves foraneas.
    DELETE FROM dbo.TrabajosImpresion;
    DELETE FROM dbo.DetalleVenta;
    DELETE FROM dbo.Ventas;
    DELETE FROM dbo.DetalleCompra;
    DELETE FROM dbo.Compras;
    DELETE FROM dbo.ProveedorProducto;
    DELETE FROM dbo.CajaMovimientos;
    DELETE FROM dbo.CajaSesiones;
    DELETE FROM dbo.Productos;
    DELETE FROM dbo.Categorias;

    -- Reinicia los contadores de Id para que todo arranque desde 1.
    DBCC CHECKIDENT ('dbo.TrabajosImpresion', RESEED, 0);
    DBCC CHECKIDENT ('dbo.DetalleVenta',      RESEED, 0);
    DBCC CHECKIDENT ('dbo.Ventas',            RESEED, 0);
    DBCC CHECKIDENT ('dbo.DetalleCompra',     RESEED, 0);
    DBCC CHECKIDENT ('dbo.Compras',           RESEED, 0);
    DBCC CHECKIDENT ('dbo.ProveedorProducto', RESEED, 0);
    DBCC CHECKIDENT ('dbo.CajaMovimientos',   RESEED, 0);
    DBCC CHECKIDENT ('dbo.CajaSesiones',      RESEED, 0);
    DBCC CHECKIDENT ('dbo.Productos',         RESEED, 0);
    DBCC CHECKIDENT ('dbo.Categorias',        RESEED, 0);

    -- ---- OPCIONAL (NO recomendado): borrar tambien usuarios / configuracion ----
    -- DELETE FROM dbo.Usuarios;             -- perderias el login admin/cajero
    -- DELETE FROM dbo.ConfiguracionEmpresa; -- perderias datos de empresa y tema

    COMMIT TRANSACTION;
    PRINT 'Limpieza completada correctamente.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    PRINT 'ERROR en la limpieza. No se borro nada (rollback).';
    THROW;
END CATCH;
GO

-- Verificacion: deben quedar en 0, excepto Proveedores/Usuarios/ConfiguracionEmpresa.
SELECT 'Productos' AS Tabla, COUNT(*) AS Filas FROM dbo.Productos
UNION ALL SELECT 'Categorias', COUNT(*) FROM dbo.Categorias
UNION ALL SELECT 'Ventas', COUNT(*) FROM dbo.Ventas
UNION ALL SELECT 'DetalleVenta', COUNT(*) FROM dbo.DetalleVenta
UNION ALL SELECT 'Compras', COUNT(*) FROM dbo.Compras
UNION ALL SELECT 'ProveedorProducto', COUNT(*) FROM dbo.ProveedorProducto
UNION ALL SELECT 'CajaSesiones', COUNT(*) FROM dbo.CajaSesiones
UNION ALL SELECT 'Proveedores (se conserva)', COUNT(*) FROM dbo.Proveedores
UNION ALL SELECT 'Usuarios (se conserva)', COUNT(*) FROM dbo.Usuarios;
GO
