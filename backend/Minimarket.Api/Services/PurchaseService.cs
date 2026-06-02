using Minimarket.Api.DTOs;
using Minimarket.Api.Mapping;
using Minimarket.Api.Models;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

public class PurchaseService(
    IPurchaseRepository purchaseRepository,
    ISupplierRepository supplierRepository,
    IUserRepository userRepository,
    IProductRepository productRepository) : IPurchaseService
{
    public async Task<IReadOnlyCollection<PurchaseDto>> GetAllAsync() =>
        (await purchaseRepository.GetAllAsync()).Select(x => x.ToDto()).ToList();

    public async Task<PurchaseDto?> GetByIdAsync(int id) =>
        (await purchaseRepository.GetByIdAsync(id))?.ToDto();

    public async Task<(bool Success, string? Error, PurchaseDto? Purchase)> CreateAsync(CreatePurchaseDto dto)
    {
        if (dto.Details.Count == 0)
        {
            return (false, "La compra debe tener al menos un item.", null);
        }

        var supplier = await supplierRepository.GetByIdAsync(dto.SupplierId);
        if (supplier is null || !supplier.IsActive)
        {
            return (false, "El proveedor seleccionado no existe o esta inactivo.", null);
        }

        var user = await userRepository.GetByIdAsync(dto.UserId);
        if (user is null || !user.IsActive)
        {
            return (false, "El usuario seleccionado no existe o esta inactivo.", null);
        }

        var purchase = new Purchase
        {
            PurchaseDate = DateTime.Now,
            SupplierId = dto.SupplierId,
            UserId = dto.UserId,
            InvoiceNumber = NormalizeOptional(dto.InvoiceNumber),
            Notes = NormalizeOptional(dto.Notes)
        };

        foreach (var item in dto.Details)
        {
            var product = await productRepository.GetByIdAsync(item.ProductId);
            if (product is null || !product.IsActive)
            {
                return (false, $"El producto con id {item.ProductId} no existe.", null);
            }

            if (item.PackageQuantity <= 0 || item.UnitsPerPackage <= 0 || item.PackageCost <= 0)
            {
                return (false, $"Los valores de compra para {product.Name} no son validos.", null);
            }

            var totalUnits = item.PackageQuantity * item.UnitsPerPackage;
            var subtotal = item.PackageCost * item.PackageQuantity;
            var unitCost = subtotal / totalUnits;
            var currentStock = product.Stock;
            var currentCost = product.Cost;

            product.Stock += totalUnits;
            product.Cost = currentStock + totalUnits > 0
                ? decimal.Round(((currentCost * currentStock) + (unitCost * totalUnits)) / (currentStock + totalUnits), 2)
                : decimal.Round(unitCost, 2);
            product.PurchaseUnitName = NormalizeUnitName(item.PurchaseUnitName, product.PurchaseUnitName);
            product.UnitsPerPurchaseUnit = item.UnitsPerPackage;

            purchase.Details.Add(new PurchaseDetail
            {
                ProductId = product.Id,
                PackageQuantity = item.PackageQuantity,
                UnitsPerPackage = item.UnitsPerPackage,
                TotalUnits = totalUnits,
                PackageCost = decimal.Round(item.PackageCost, 2),
                UnitCost = decimal.Round(unitCost, 2),
                Subtotal = decimal.Round(subtotal, 2),
                PurchaseUnitName = NormalizeUnitName(item.PurchaseUnitName, product.PurchaseUnitName),
                BarcodeSnapshot = NormalizeOptional(item.BarcodeSnapshot)
            });
        }

        purchase.Total = purchase.Details.Sum(x => x.Subtotal);

        await purchaseRepository.AddAsync(purchase);
        await purchaseRepository.SaveChangesAsync();

        var created = await purchaseRepository.GetByIdAsync(purchase.Id);
        return (true, null, created?.ToDto());
    }

    private static string? NormalizeOptional(string? value)
    {
        var trimmed = value?.Trim();
        return string.IsNullOrWhiteSpace(trimmed) ? null : trimmed;
    }

    private static string NormalizeUnitName(string? providedValue, string fallbackValue)
    {
        var trimmed = providedValue?.Trim();
        return string.IsNullOrWhiteSpace(trimmed) ? fallbackValue : trimmed;
    }
}
