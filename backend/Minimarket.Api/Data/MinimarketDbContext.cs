using Microsoft.EntityFrameworkCore;
using Minimarket.Api.Models;

namespace Minimarket.Api.Data;

public class MinimarketDbContext(DbContextOptions<MinimarketDbContext> options) : DbContext(options)
{
    public DbSet<Category> Categories => Set<Category>();
    public DbSet<Product> Products => Set<Product>();
    public DbSet<Supplier> Suppliers => Set<Supplier>();
    public DbSet<User> Users => Set<User>();
    public DbSet<Sale> Sales => Set<Sale>();
    public DbSet<SaleDetail> SaleDetails => Set<SaleDetail>();
    public DbSet<Purchase> Purchases => Set<Purchase>();
    public DbSet<PurchaseDetail> PurchaseDetails => Set<PurchaseDetail>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Category>(entity =>
        {
            entity.ToTable("Categorias");
            entity.HasKey(x => x.Id);
            entity.Property(x => x.Name).HasColumnName("Nombre").HasMaxLength(100).IsRequired();
            entity.Property(x => x.Description).HasColumnName("Descripcion").HasMaxLength(250);
            entity.Property(x => x.IsActive).HasColumnName("Activo");
        });

        modelBuilder.Entity<Product>(entity =>
        {
            entity.ToTable("Productos");
            entity.HasKey(x => x.Id);
            entity.Property(x => x.Name).HasColumnName("Nombre").HasMaxLength(150).IsRequired();
            entity.Property(x => x.Sku).HasMaxLength(30).IsRequired();
            entity.Property(x => x.Barcode).HasColumnName("CodigoBarras").HasMaxLength(50);
            entity.Property(x => x.PurchaseBarcode).HasColumnName("CodigoBarrasCompra").HasMaxLength(50);
            entity.Property(x => x.Description).HasColumnName("Descripcion").HasMaxLength(250);
            entity.Property(x => x.Price).HasColumnName("Precio").HasColumnType("decimal(10,2)");
            entity.Property(x => x.Cost).HasColumnName("Costo").HasColumnType("decimal(10,2)");
            entity.Property(x => x.MinimumStock).HasColumnName("StockMinimo");
            entity.Property(x => x.SalesUnitName).HasColumnName("UnidadVenta").HasMaxLength(30).IsRequired();
            entity.Property(x => x.PurchaseUnitName).HasColumnName("UnidadCompra").HasMaxLength(30).IsRequired();
            entity.Property(x => x.UnitsPerPurchaseUnit).HasColumnName("UnidadesPorCompra");
            entity.Property(x => x.IsActive).HasColumnName("Activo");
            entity.Property(x => x.CategoryId).HasColumnName("CategoriaId");
            entity.HasIndex(x => x.Sku).IsUnique();
            entity.HasIndex(x => x.Barcode).IsUnique();
            entity.HasIndex(x => x.PurchaseBarcode).IsUnique();
            entity.HasOne(x => x.Category)
                .WithMany(x => x.Products)
                .HasForeignKey(x => x.CategoryId)
                .OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<Supplier>(entity =>
        {
            entity.ToTable("Proveedores");
            entity.HasKey(x => x.Id);
            entity.Property(x => x.Name).HasColumnName("Nombre").HasMaxLength(150).IsRequired();
            entity.Property(x => x.DocumentNumber).HasColumnName("NumeroDocumento").HasMaxLength(30);
            entity.Property(x => x.ContactName).HasColumnName("NombreContacto").HasMaxLength(120);
            entity.Property(x => x.Phone).HasColumnName("Telefono").HasMaxLength(30);
            entity.Property(x => x.Email).HasColumnName("Correo").HasMaxLength(120);
            entity.Property(x => x.Address).HasColumnName("Direccion").HasMaxLength(250);
            entity.Property(x => x.Notes).HasColumnName("Notas").HasMaxLength(250);
            entity.Property(x => x.IsActive).HasColumnName("Activo");
        });

        modelBuilder.Entity<User>(entity =>
        {
            entity.ToTable("Usuarios");
            entity.HasKey(x => x.Id);
            entity.Property(x => x.FullName).HasColumnName("NombreCompleto").HasMaxLength(120).IsRequired();
            entity.Property(x => x.Username).HasMaxLength(50).IsRequired();
            entity.Property(x => x.PasswordHash).HasMaxLength(256).IsRequired();
            entity.Property(x => x.Role).HasColumnName("Rol").HasMaxLength(20).IsRequired();
            entity.Property(x => x.IsActive).HasColumnName("Activo");
            entity.HasIndex(x => x.Username).IsUnique();
        });

        modelBuilder.Entity<Sale>(entity =>
        {
            entity.ToTable("Ventas");
            entity.HasKey(x => x.Id);
            entity.Property(x => x.SaleDate).HasColumnName("FechaVenta");
            entity.Property(x => x.UserId).HasColumnName("UsuarioId");
            entity.Property(x => x.Total).HasColumnType("decimal(10,2)");
            entity.Property(x => x.PaymentMethod).HasColumnName("MetodoPago").HasMaxLength(30).IsRequired();
            entity.Property(x => x.Notes).HasColumnName("Notas").HasMaxLength(250);
            entity.HasOne(x => x.User)
                .WithMany(x => x.Sales)
                .HasForeignKey(x => x.UserId)
                .OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<SaleDetail>(entity =>
        {
            entity.ToTable("DetalleVenta");
            entity.HasKey(x => x.Id);
            entity.Property(x => x.SaleId).HasColumnName("VentaId");
            entity.Property(x => x.ProductId).HasColumnName("ProductoId");
            entity.Property(x => x.Quantity).HasColumnName("Cantidad");
            entity.Property(x => x.UnitPrice).HasColumnName("PrecioUnitario").HasColumnType("decimal(10,2)");
            entity.Property(x => x.Subtotal).HasColumnType("decimal(10,2)");
            entity.HasOne(x => x.Sale)
                .WithMany(x => x.Details)
                .HasForeignKey(x => x.SaleId)
                .OnDelete(DeleteBehavior.Cascade);
            entity.HasOne(x => x.Product)
                .WithMany(x => x.SaleDetails)
                .HasForeignKey(x => x.ProductId)
                .OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<Purchase>(entity =>
        {
            entity.ToTable("Compras");
            entity.HasKey(x => x.Id);
            entity.Property(x => x.PurchaseDate).HasColumnName("FechaCompra");
            entity.Property(x => x.SupplierId).HasColumnName("ProveedorId");
            entity.Property(x => x.UserId).HasColumnName("UsuarioId");
            entity.Property(x => x.InvoiceNumber).HasColumnName("NumeroComprobante").HasMaxLength(50);
            entity.Property(x => x.Notes).HasColumnName("Notas").HasMaxLength(250);
            entity.Property(x => x.Total).HasColumnType("decimal(12,2)");
            entity.HasOne(x => x.Supplier)
                .WithMany(x => x.Purchases)
                .HasForeignKey(x => x.SupplierId)
                .OnDelete(DeleteBehavior.Restrict);
            entity.HasOne(x => x.User)
                .WithMany()
                .HasForeignKey(x => x.UserId)
                .OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<PurchaseDetail>(entity =>
        {
            entity.ToTable("DetalleCompra");
            entity.HasKey(x => x.Id);
            entity.Property(x => x.PurchaseId).HasColumnName("CompraId");
            entity.Property(x => x.ProductId).HasColumnName("ProductoId");
            entity.Property(x => x.PackageQuantity).HasColumnName("CantidadPaquetes");
            entity.Property(x => x.UnitsPerPackage).HasColumnName("UnidadesPorPaquete");
            entity.Property(x => x.TotalUnits).HasColumnName("TotalUnidades");
            entity.Property(x => x.PackageCost).HasColumnName("CostoPaquete").HasColumnType("decimal(10,2)");
            entity.Property(x => x.UnitCost).HasColumnName("CostoUnitario").HasColumnType("decimal(10,2)");
            entity.Property(x => x.Subtotal).HasColumnType("decimal(12,2)");
            entity.Property(x => x.PurchaseUnitName).HasColumnName("UnidadCompra").HasMaxLength(30).IsRequired();
            entity.Property(x => x.BarcodeSnapshot).HasColumnName("CodigoBarrasLeido").HasMaxLength(50);
            entity.HasOne(x => x.Purchase)
                .WithMany(x => x.Details)
                .HasForeignKey(x => x.PurchaseId)
                .OnDelete(DeleteBehavior.Cascade);
            entity.HasOne(x => x.Product)
                .WithMany(x => x.PurchaseDetails)
                .HasForeignKey(x => x.ProductId)
                .OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<Category>().HasData(
            new Category { Id = 1, Name = "Abarrotes", Description = "Productos de uso diario", IsActive = true },
            new Category { Id = 2, Name = "Bebidas", Description = "Gaseosas, aguas y jugos", IsActive = true },
            new Category { Id = 3, Name = "Limpieza", Description = "Articulos de limpieza", IsActive = true });

        modelBuilder.Entity<Product>().HasData(
            new Product { Id = 1, Name = "Arroz Superior 1Kg", Sku = "ABR-001", Description = "Bolsa de arroz blanco", Price = 4.50m, Cost = 3.60m, Stock = 80, MinimumStock = 5, SalesUnitName = "unidad", PurchaseUnitName = "fardo", UnitsPerPurchaseUnit = 12, CategoryId = 1, IsActive = true },
            new Product { Id = 2, Name = "Azucar Rubia 1Kg", Sku = "ABR-002", Description = "Azucar rubia embolsada", Price = 4.20m, Cost = 3.30m, Stock = 60, MinimumStock = 5, SalesUnitName = "unidad", PurchaseUnitName = "fardo", UnitsPerPurchaseUnit = 10, CategoryId = 1, IsActive = true },
            new Product { Id = 3, Name = "Gaseosa Cola 3L", Sku = "BEB-001", Description = "Botella retornable", Price = 9.80m, Cost = 7.20m, Stock = 30, MinimumStock = 5, SalesUnitName = "botella", PurchaseUnitName = "jaba", UnitsPerPurchaseUnit = 12, CategoryId = 2, IsActive = true },
            new Product { Id = 4, Name = "Agua Mineral 625ml", Sku = "BEB-002", Description = "Botella personal", Price = 2.50m, Cost = 1.40m, Stock = 48, MinimumStock = 5, SalesUnitName = "botella", PurchaseUnitName = "jaba", UnitsPerPurchaseUnit = 24, CategoryId = 2, IsActive = true },
            new Product { Id = 5, Name = "Detergente Floral 900g", Sku = "LIM-001", Description = "Detergente en polvo", Price = 8.90m, Cost = 6.80m, Stock = 22, MinimumStock = 5, SalesUnitName = "unidad", PurchaseUnitName = "caja", UnitsPerPurchaseUnit = 12, CategoryId = 3, IsActive = true });

        modelBuilder.Entity<Supplier>().HasData(
            new Supplier
            {
                Id = 1,
                Name = "Distribuidora Central",
                DocumentNumber = "20601234567",
                ContactName = "Rosa Medina",
                Phone = "987654321",
                Email = "ventas@distribuidoracentral.pe",
                Address = "Av. Principal 123",
                Notes = "Proveedor inicial de referencia",
                IsActive = true
            });

        modelBuilder.Entity<User>().HasData(
            new User { Id = 1, FullName = "Administrador General", Username = "admin", PasswordHash = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3", Role = "admin", IsActive = true },
            new User { Id = 2, FullName = "Caja Principal", Username = "cajero", PasswordHash = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3", Role = "cajero", IsActive = true });
    }
}
