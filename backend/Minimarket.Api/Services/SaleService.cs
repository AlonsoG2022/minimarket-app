using Minimarket.Api.DTOs;
using Minimarket.Api.Mapping;
using Minimarket.Api.Models;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

public class SaleService(
    ISaleRepository saleRepository,
    IUserRepository userRepository,
    IProductRepository productRepository) : ISaleService
{
    public async Task<IReadOnlyCollection<SaleDto>> GetAllAsync() =>
        (await saleRepository.GetAllAsync()).Select(x => x.ToDto()).ToList();

    public async Task<SaleDto?> GetByIdAsync(int id) =>
        (await saleRepository.GetByIdAsync(id))?.ToDto();

    public async Task<(bool Success, string? Error, SaleDto? Sale)> CreateAsync(CreateSaleDto dto)
    {
        if (dto.Details.Count == 0)
        {
            return (false, "La venta debe tener al menos un item.", null);
        }

        var user = await userRepository.GetByIdAsync(dto.UserId);
        if (user is null || !user.IsActive)
        {
            return (false, "El usuario seleccionado no existe o esta inactivo.", null);
        }

        var sale = new Sale
        {
            SaleDate = DateTime.Now,
            UserId = dto.UserId,
            PaymentMethod = dto.PaymentMethod.Trim(),
            Notes = dto.Notes?.Trim()
        };

        foreach (var item in dto.Details)
        {
            var product = await productRepository.GetByIdAsync(item.ProductId);
            if (product is null || !product.IsActive)
            {
                return (false, $"El producto con id {item.ProductId} no existe.", null);
            }

            if (item.Quantity <= 0)
            {
                return (false, $"La cantidad del producto {product.Name} debe ser mayor que cero.", null);
            }

            if (product.Stock < item.Quantity)
            {
                return (false, $"Stock insuficiente para {product.Name}. Disponible: {product.Stock}.", null);
            }

            product.Stock -= item.Quantity;

            sale.Details.Add(new SaleDetail
            {
                ProductId = product.Id,
                Quantity = item.Quantity,
                UnitPrice = product.Price,
                Subtotal = product.Price * item.Quantity
            });
        }

        sale.Total = sale.Details.Sum(x => x.Subtotal);

        await saleRepository.AddAsync(sale);
        await saleRepository.SaveChangesAsync();

        var created = await saleRepository.GetByIdAsync(sale.Id);
        return (true, null, created?.ToDto());
    }
}
