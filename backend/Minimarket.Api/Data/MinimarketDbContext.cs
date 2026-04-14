using Microsoft.EntityFrameworkCore;
using Minimarket.Api.Models;

namespace Minimarket.Api.Data;

public class MinimarketDbContext(DbContextOptions<MinimarketDbContext> options) : DbContext(options)
{
    public DbSet<Category> Categories => Set<Category>();
    public DbSet<Product> Products => Set<Product>();
    public DbSet<User> Users => Set<User>();
    public DbSet<Sale> Sales => Set<Sale>();
    public DbSet<SaleDetail> SaleDetails => Set<SaleDetail>();

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
            entity.Property(x => x.Description).HasColumnName("Descripcion").HasMaxLength(250);
            entity.Property(x => x.Price).HasColumnName("Precio").HasColumnType("decimal(10,2)");
            entity.Property(x => x.MinimumStock).HasColumnName("StockMinimo");
            entity.Property(x => x.IsActive).HasColumnName("Activo");
            entity.Property(x => x.CategoryId).HasColumnName("CategoriaId");
            entity.HasIndex(x => x.Sku).IsUnique();
            entity.HasOne(x => x.Category)
                .WithMany(x => x.Products)
                .HasForeignKey(x => x.CategoryId)
                .OnDelete(DeleteBehavior.Restrict);
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

        modelBuilder.Entity<Category>().HasData(
            new Category { Id = 1, Name = "Abarrotes", Description = "Productos de uso diario", IsActive = true },
            new Category { Id = 2, Name = "Bebidas", Description = "Gaseosas, aguas y jugos", IsActive = true },
            new Category { Id = 3, Name = "Limpieza", Description = "Articulos de limpieza", IsActive = true });

        modelBuilder.Entity<Product>().HasData(
            new Product { Id = 1, Name = "Arroz Superior 1Kg", Sku = "ABR-001", Description = "Bolsa de arroz blanco", Price = 4.50m, Stock = 80, MinimumStock = 20, CategoryId = 1, IsActive = true },
            new Product { Id = 2, Name = "Azucar Rubia 1Kg", Sku = "ABR-002", Description = "Azucar rubia embolsada", Price = 4.20m, Stock = 60, MinimumStock = 15, CategoryId = 1, IsActive = true },
            new Product { Id = 3, Name = "Gaseosa Cola 3L", Sku = "BEB-001", Description = "Botella retornable", Price = 9.80m, Stock = 30, MinimumStock = 10, CategoryId = 2, IsActive = true },
            new Product { Id = 4, Name = "Agua Mineral 625ml", Sku = "BEB-002", Description = "Botella personal", Price = 2.50m, Stock = 48, MinimumStock = 12, CategoryId = 2, IsActive = true },
            new Product { Id = 5, Name = "Detergente Floral 900g", Sku = "LIM-001", Description = "Detergente en polvo", Price = 8.90m, Stock = 22, MinimumStock = 8, CategoryId = 3, IsActive = true });

        modelBuilder.Entity<User>().HasData(
            new User { Id = 1, FullName = "Administrador General", Username = "admin", PasswordHash = "3eb3fe66b31e3b4d10fa70b5cad49c7112294af6ae4e476a1c405155d45aa121", Role = "admin", IsActive = true },
            new User { Id = 2, FullName = "Caja Principal", Username = "cajero", PasswordHash = "3eaf7440a5899bf27c390f75b4bbf3be0fc200cc3d5908f01e210d39f3995b18", Role = "cajero", IsActive = true });
    }
}
