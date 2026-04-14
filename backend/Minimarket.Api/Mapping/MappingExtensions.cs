using Minimarket.Api.DTOs;
using Minimarket.Api.Models;

namespace Minimarket.Api.Mapping;

public static class MappingExtensions
{
    public static ProductDto ToDto(this Product product) =>
        new(
            product.Id,
            product.Name,
            product.Sku,
            product.Description,
            product.Price,
            product.Stock,
            product.MinimumStock,
            product.IsActive,
            product.CategoryId,
            product.Category?.Name ?? string.Empty);

    public static CategoryDto ToDto(this Category category) =>
        new(category.Id, category.Name, category.Description, category.IsActive);

    public static UserDto ToDto(this User user) =>
        new(user.Id, user.FullName, user.Username, user.Role, user.IsActive);

    public static SaleDto ToDto(this Sale sale) =>
        new(
            sale.Id,
            sale.SaleDate,
            sale.UserId,
            sale.User?.FullName ?? string.Empty,
            sale.PaymentMethod,
            sale.Total,
            sale.Notes,
            sale.Details
                .Select(detail => new SaleDetailDto(
                    detail.Id,
                    detail.ProductId,
                    detail.Product?.Name ?? string.Empty,
                    detail.Quantity,
                    detail.UnitPrice,
                    detail.Subtotal))
                .ToList());
}
