using Minimarket.Api.DTOs;
using Minimarket.Api.Mapping;
using Minimarket.Api.Models;
using Minimarket.Api.Repositories;

namespace Minimarket.Api.Services;

public class SaleService(
    ISaleRepository saleRepository,
    IUserRepository userRepository,
    IProductRepository productRepository,
    ICashSessionRepository cashSessionRepository,
    IPrintJobService printJobService,
    ILogger<SaleService> logger) : ISaleService
{
    private const decimal IgvDivisor = 1.18m;

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
            CashSessionId = dto.CashSessionId,
            PaymentMethod = dto.PaymentMethod.Trim(),
            Notes = dto.Notes?.Trim(),
            SubTotal = 0m,
            Igv = 0m,
            Total = 0m
        };

        var cashSession = await cashSessionRepository.GetCurrentOpenAsync(dto.UserId);
        if (cashSession is null)
        {
            return (false, "Debes abrir caja antes de registrar ventas.", null);
        }

        if (dto.CashSessionId.HasValue && cashSession.Id != dto.CashSessionId.Value)
        {
            return (false, "La caja activa no coincide con la sesion enviada.", null);
        }

        sale.CashSessionId = cashSession.Id;

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
        sale.SubTotal = CalculateSubTotalFromGross(sale.Total);
        sale.Igv = CalculateIgvFromGross(sale.Total, sale.SubTotal);

        if (string.Equals(sale.PaymentMethod, "Efectivo", StringComparison.OrdinalIgnoreCase))
        {
            cashSession.Movements.Add(new CashMovement
            {
                MovementDate = DateTime.Now,
                Type = "venta_efectivo",
                Amount = sale.Total,
                Description = $"Venta #{sale.Id}",
                ReferenceType = "venta"
            });
        }

        await saleRepository.AddAsync(sale);
        await saleRepository.SaveChangesAsync();

        if (string.Equals(sale.PaymentMethod, "Efectivo", StringComparison.OrdinalIgnoreCase))
        {
            var saleMovement = cashSession.Movements
                .LastOrDefault(movement => movement.ReferenceType == "venta" && movement.ReferenceId is null);

            if (saleMovement is not null)
            {
                saleMovement.ReferenceId = sale.Id;
                saleMovement.Description = $"Venta #{sale.Id}";
                await saleRepository.SaveChangesAsync();
            }
        }

        var created = await saleRepository.GetByIdAsync(sale.Id);
        if (created is not null)
        {
            try
            {
                await printJobService.EnqueueSaleTicketAsync(created.Id);
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "La venta {SaleId} se guardo, pero no se pudo encolar el ticket.", created.Id);
            }

            created = await saleRepository.GetByIdAsync(created.Id);
        }
        return (true, null, created?.ToDto());
    }

    private static decimal CalculateSubTotalFromGross(decimal total) =>
        decimal.Round(total / IgvDivisor, 2, MidpointRounding.AwayFromZero);

    private static decimal CalculateIgvFromGross(decimal total, decimal subTotal) =>
        decimal.Round(total - subTotal, 2, MidpointRounding.AwayFromZero);
}
