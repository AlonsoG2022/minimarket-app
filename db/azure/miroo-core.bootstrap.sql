/*
    Bootstrap idempotente para MirooCore en Azure SQL Database
    -----------------------------------------------------------
    Ejecutar conectado exclusivamente a la base MirooCore.

    - No crea ni elimina bases de datos.
    - No modifica MinimarketDb.
    - No crea logins ni usuarios de servidor.
    - Crea/actualiza el registro de tenants de la plataforma.
*/

SET NOCOUNT ON;
GO

IF DB_NAME() <> N'MirooCore'
BEGIN
    THROW 50000, 'Este script debe ejecutarse conectado a la base MirooCore.', 1;
END;
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

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.Tenants')
      AND name = 'UX_Tenants_Code'
)
BEGIN
    CREATE UNIQUE INDEX UX_Tenants_Code ON dbo.Tenants(Code);
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.Tenants')
      AND name = 'UX_Tenants_Slug'
)
BEGIN
    CREATE UNIQUE INDEX UX_Tenants_Slug ON dbo.Tenants(Slug);
END;
GO

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

IF NOT EXISTS (SELECT 1 FROM dbo.Tenants WHERE Slug = 'future-business')
BEGIN
    INSERT INTO dbo.Tenants (Code, Name, Slug, DatabaseName, IsActive)
    VALUES ('FUTURE_BUSINESS', N'Negocio futuro', 'future-business', 'MirooFutureBusiness', 0);
END;
GO

PRINT 'MirooCore quedo preparado. MinimarketDb y MirooFutureBusiness no fueron modificadas.';
GO
