using Minimarket.Api.DTOs;
using Minimarket.Api.Mapping;
using Minimarket.Api.Models;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

public class SupplierService(ISupplierRepository supplierRepository) : ISupplierService
{
    public async Task<IReadOnlyCollection<SupplierDto>> GetAllAsync() =>
        (await supplierRepository.GetAllAsync()).Select(x => x.ToDto()).ToList();

    public async Task<SupplierDto?> GetByIdAsync(int id) =>
        (await supplierRepository.GetByIdAsync(id))?.ToDto();

    public async Task<(bool Success, string? Error, SupplierDto? Supplier)> CreateAsync(SaveSupplierDto dto)
    {
        if (string.IsNullOrWhiteSpace(dto.Name))
        {
            return (false, "El nombre del proveedor es obligatorio.", null);
        }

        var supplier = new Supplier
        {
            Name = dto.Name.Trim(),
            DocumentNumber = NormalizeOptional(dto.DocumentNumber),
            ContactName = NormalizeOptional(dto.ContactName),
            Phone = NormalizeOptional(dto.Phone),
            Email = NormalizeOptional(dto.Email),
            Address = NormalizeOptional(dto.Address),
            Notes = NormalizeOptional(dto.Notes),
            IsActive = dto.IsActive
        };

        await supplierRepository.AddAsync(supplier);
        await supplierRepository.SaveChangesAsync();

        var created = await supplierRepository.GetByIdAsync(supplier.Id);
        return (true, null, created?.ToDto());
    }

    public async Task<(bool Success, string? Error, SupplierDto? Supplier)> UpdateAsync(int id, SaveSupplierDto dto)
    {
        var supplier = await supplierRepository.GetByIdAsync(id);
        if (supplier is null)
        {
            return (false, "Proveedor no encontrado.", null);
        }

        if (string.IsNullOrWhiteSpace(dto.Name))
        {
            return (false, "El nombre del proveedor es obligatorio.", null);
        }

        supplier.Name = dto.Name.Trim();
        supplier.DocumentNumber = NormalizeOptional(dto.DocumentNumber);
        supplier.ContactName = NormalizeOptional(dto.ContactName);
        supplier.Phone = NormalizeOptional(dto.Phone);
        supplier.Email = NormalizeOptional(dto.Email);
        supplier.Address = NormalizeOptional(dto.Address);
        supplier.Notes = NormalizeOptional(dto.Notes);
        supplier.IsActive = dto.IsActive;

        supplierRepository.Update(supplier);
        await supplierRepository.SaveChangesAsync();

        var updated = await supplierRepository.GetByIdAsync(id);
        return (true, null, updated?.ToDto());
    }

    public async Task<(bool Success, string? Error)> DeleteAsync(int id)
    {
        var supplier = await supplierRepository.GetByIdAsync(id);
        if (supplier is null)
        {
            return (false, "Proveedor no encontrado.");
        }

        supplierRepository.Remove(supplier);
        await supplierRepository.SaveChangesAsync();
        return (true, null);
    }

    private static string? NormalizeOptional(string? value)
    {
        var trimmed = value?.Trim();
        return string.IsNullOrWhiteSpace(trimmed) ? null : trimmed;
    }
}
