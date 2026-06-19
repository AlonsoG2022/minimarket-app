using Minimarket.Api.DTOs;
using Minimarket.Api.Models;

namespace Minimarket.Api.Mapping;


public static class MappingExtensions
{
    public static CompanyDto ToDto(this Company company) =>
        new(
            company.Id,
            company.BusinessName,
            company.LegalName,
            company.TaxId,
            company.AddressLine,
            company.Phone,
            company.Tagline,
            company.DocumentTitle,
            company.CustomerLabel,
            company.FooterLine1,
            company.FooterLine2,
            company.ShowTicketPreview,
            company.MinimumStock,
            company.Theme);

    public static ProductDto ToDto(this Product product) =>
        new(
            product.Id,
            product.Name,
            product.ShortName,
            product.Sku,
            product.Barcode,
            product.PurchaseBarcode,
            product.Description,
            product.Price,
            product.Cost,
            product.Stock,
            product.MinimumStock,
            product.ExpirationDate?.ToString("yyyy-MM-dd"),
            product.SalesUnitName,
            product.PurchaseUnitName,
            product.UnitsPerPurchaseUnit,
            product.IsActive,
            product.CategoryId,
            product.Category?.Name ?? string.Empty);

    public static CategoryDto ToDto(this Category category) =>
        new(category.Id, category.Name, category.Description, category.IsActive);

    public static UserDto ToDto(this User user) =>
        new(user.Id, user.FullName, user.Username, user.Role, user.IsActive);

    public static SupplierDto ToDto(this Supplier supplier) =>
        new(
            supplier.Id,
            supplier.Name,
            supplier.DocumentNumber,
            supplier.ContactName,
            supplier.Phone,
            supplier.Email,
            supplier.Address,
            supplier.Notes,
            supplier.IsActive);

    public static SaleDto ToDto(this Sale sale) =>
        new(
            sale.Id,
            sale.SaleDate,
            sale.UserId,
            sale.User?.FullName ?? string.Empty,
            sale.CashSessionId,
            sale.PrintJobs
                .OrderByDescending(job => job.RequestedAt)
                .Select(job => job.Status)
                .FirstOrDefault(),
            sale.PrintJobs
                .OrderByDescending(job => job.RequestedAt)
                .Select(job => (int?)job.Id)
                .FirstOrDefault(),
            sale.PaymentMethod,
            sale.SubTotal,
            sale.Igv,
            sale.Total,
            sale.Notes,
            sale.Details
                .Select(detail => new SaleDetailDto(
                    detail.Id,
                    detail.ProductId,
                    detail.Product?.Name ?? string.Empty,
                    string.IsNullOrWhiteSpace(detail.Product?.ShortName) ? (detail.Product?.Name ?? string.Empty) : detail.Product!.ShortName,
                    detail.Quantity,
                    detail.UnitPrice,
                    detail.Subtotal))
                .ToList());

    public static CashSessionDto ToDto(this CashSession session)
    {
        var currentAmount = session.OpeningAmount;

        foreach (var movement in session.Movements)
        {
            currentAmount += movement.Type.ToLowerInvariant() switch
            {
                "ingreso" => movement.Amount,
                "venta_efectivo" => movement.Amount,
                "retiro" => -movement.Amount,
                "gasto" => -movement.Amount,
                _ => 0m
            };
        }

        return new CashSessionDto(
            session.Id,
            session.UserId,
            session.User?.FullName ?? string.Empty,
            session.OpenedAt,
            session.ClosedAt,
            session.OpeningAmount,
            session.ClosingExpectedAmount,
            session.ClosingCountedAmount,
            session.Difference,
            session.Status,
            session.Notes,
            currentAmount,
            session.Movements
                .OrderByDescending(movement => movement.MovementDate)
                .Select(movement => new CashMovementDto(
                    movement.Id,
                    movement.MovementDate,
                    movement.Type,
                    movement.Amount,
                    movement.Description,
                    movement.ReferenceType,
                    movement.ReferenceId))
                .ToList());
    }

    public static PrintJobDto ToDto(this PrintJob job) =>
        new(
            job.Id,
            job.SaleId,
            job.SourceType,
            job.DocumentType,
            job.Status,
            job.Attempts,
            job.PrinterName,
            job.RequestedAt,
            job.StartedAt,
            job.ProcessedAt,
            job.LastError);

    public static PurchaseDto ToDto(this Purchase purchase) =>
        new(
            purchase.Id,
            purchase.PurchaseDate,
            purchase.SupplierId,
            purchase.Supplier?.Name ?? string.Empty,
            purchase.UserId,
            purchase.User?.FullName ?? string.Empty,
            purchase.InvoiceNumber,
            purchase.Notes,
            purchase.SubTotal,
            purchase.Igv,
            purchase.Total,
            purchase.Details
                .Select(detail => new PurchaseDetailDto(
                    detail.Id,
                    detail.ProductId,
                    detail.Product?.Name ?? string.Empty,
                    detail.PackageQuantity,
                    detail.UnitsPerPackage,
                    detail.TotalUnits,
                    detail.PackageCost,
                    detail.UnitCost,
                    detail.Subtotal,
                    detail.PurchaseUnitName,
                    detail.BarcodeSnapshot))
                .ToList());
}
