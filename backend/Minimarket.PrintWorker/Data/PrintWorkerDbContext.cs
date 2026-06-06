using Microsoft.EntityFrameworkCore;
using Minimarket.PrintWorker.Models;

namespace Minimarket.PrintWorker.Data;

public class PrintWorkerDbContext(DbContextOptions<PrintWorkerDbContext> options) : DbContext(options)
{
    public DbSet<PrintJob> PrintJobs => Set<PrintJob>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<PrintJob>(entity =>
        {
            entity.ToTable("TrabajosImpresion");
            entity.HasKey(x => x.Id);
            entity.Property(x => x.SaleId).HasColumnName("VentaId");
            entity.Property(x => x.SourceType).HasColumnName("TipoOrigen").HasMaxLength(30);
            entity.Property(x => x.DocumentType).HasColumnName("TipoDocumento").HasMaxLength(20);
            entity.Property(x => x.Status).HasColumnName("Estado").HasMaxLength(20);
            entity.Property(x => x.Attempts).HasColumnName("Intentos");
            entity.Property(x => x.PrinterName).HasColumnName("NombreImpresora").HasMaxLength(120);
            entity.Property(x => x.RequestedAt).HasColumnName("SolicitadoEn");
            entity.Property(x => x.StartedAt).HasColumnName("ProcesandoEn");
            entity.Property(x => x.ProcessedAt).HasColumnName("ProcesadoEn");
            entity.Property(x => x.LastError).HasColumnName("UltimoError").HasMaxLength(500);
            entity.Property(x => x.PayloadJson).HasColumnName("PayloadJson");
        });
    }
}
