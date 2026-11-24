/*
    Preparacion idempotente de plataforma Miroo
    -------------------------------------------
    - Conserva intacta la base operacional MinimarketDb.
    - Crea MirooCore para registrar negocios/tenants.
    - Crea MirooFutureBusiness vacia para un negocio futuro.
    - No crea tablas operacionales en MirooFutureBusiness.
    - No elimina datos, tablas ni bases existentes.

    Ejecutar con un usuario administrador de SQL Server.
*/

USE master;
GO

IF DB_ID('MirooCore') IS NULL
BEGIN
    CREATE DATABASE MirooCore;
END;
GO

IF DB_ID('MirooFutureBusiness') IS NULL
BEGIN
    CREATE DATABASE MirooFutureBusiness;
END;
GO

USE MirooCore;
GO

SET NOCOUNT ON;
GO

IF OBJECT_ID('dbo.Tenants', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Tenants
    (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Tenants PRIMARY KEY,
        Code VARCHAR(50) NOT NULL,
        Name NVARCHAR(150) NOT NULL,
        Slug VARCHAR(100) NOT NULL,
        DatabaseName VARCHAR(128) NOT NULL,
        IsActive BIT NOT NULL CONSTRAINT DF_Tenants_IsActive DEFAULT (1),
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Tenants_CreatedAt DEFAULT (SYSUTCDATETIME()),
        UpdatedAt DATETIME2 NULL
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.Tenants') AND name = 'UX_Tenants_Code')
BEGIN
    CREATE UNIQUE INDEX UX_Tenants_Code ON dbo.Tenants(Code);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.Tenants') AND name = 'UX_Tenants_Slug')
BEGIN
    CREATE UNIQUE INDEX UX_Tenants_Slug ON dbo.Tenants(Slug);
END;
GO

-- Market11 identifica al negocio; su base operacional actual conserva el nombre MinimarketDb.
IF NOT EXISTS (SELECT 1 FROM dbo.Tenants WHERE Slug = 'market11')
BEGIN
    INSERT INTO dbo.Tenants (Code, Name, Slug, DatabaseName, IsActive)
    VALUES ('MARKET11', N'Market 11', 'market11', 'MinimarketDb', 1);
END;
ELSE
BEGIN
    UPDATE dbo.Tenants
    SET Code = 'MARKET11',
        Name = N'Market 11',
        DatabaseName = 'MinimarketDb',
        UpdatedAt = SYSUTCDATETIME()
    WHERE Slug = 'market11';
END;
GO

-- Se registra el siguiente negocio, pero queda inactivo hasta que tenga esquema operacional propio.
IF NOT EXISTS (SELECT 1 FROM dbo.Tenants WHERE Slug = 'future-business')
BEGIN
    INSERT INTO dbo.Tenants (Code, Name, Slug, DatabaseName, IsActive)
    VALUES ('FUTURE_BUSINESS', N'Negocio futuro', 'future-business', 'MirooFutureBusiness', 0);
END;
GO

-- Reutiliza el login local existente solo para leer el registro de tenants durante las pruebas.
-- En Azure se reemplazara por Managed Identity o un usuario de minimo privilegio.
IF SUSER_ID('minimarket_user') IS NOT NULL
   AND DATABASE_PRINCIPAL_ID('minimarket_user') IS NULL
BEGIN
    CREATE USER minimarket_user FOR LOGIN minimarket_user;
END;
GO

IF DATABASE_PRINCIPAL_ID('minimarket_user') IS NOT NULL
   AND NOT EXISTS
   (
       SELECT 1
       FROM sys.database_role_members drm
       INNER JOIN sys.database_principals rolep ON rolep.principal_id = drm.role_principal_id
       INNER JOIN sys.database_principals memberp ON memberp.principal_id = drm.member_principal_id
       WHERE rolep.name = 'db_datareader'
         AND memberp.name = 'minimarket_user'
   )
BEGIN
    ALTER ROLE db_datareader ADD MEMBER minimarket_user;
END;
GO

PRINT 'MirooCore y MirooFutureBusiness quedaron preparados. MinimarketDb no fue modificada.';
GO
