namespace Minimarket.Api.DTOs;

public record CreatePurchaseDetailDto(
    int ProductId,
    int PackageQuantity,
    int UnitsPerPackage,
    decimal PackageCost,
    string? PurchaseUnitName,
    string? BarcodeSnapshot);

public record CreatePurchaseDto(
    int SupplierId,
    int UserId,
    string? InvoiceNumber,
    string? Notes,
    List<CreatePurchaseDetailDto> Details);

public record PurchaseDetailDto(
    int Id,
    int ProductId,
    string ProductName,
    int PackageQuantity,
    int UnitsPerPackage,
    int TotalUnits,
    decimal PackageCost,
    decimal UnitCost,
    decimal Subtotal,
    string PurchaseUnitName,
    string? BarcodeSnapshot);

public record PurchaseDto(
    int Id,
    DateTime PurchaseDate,
    int SupplierId,
    string SupplierName,
    int UserId,
    string UserName,
    string? InvoiceNumber,
    string? Notes,
    decimal SubTotal,
    decimal Igv,
    decimal Total,
    IReadOnlyCollection<PurchaseDetailDto> Details);
