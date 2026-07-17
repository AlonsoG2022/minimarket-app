namespace Minimarket.Api.Helpers;

public static class PriceCalculator
{
    public static decimal ApplyCategoryAdjustment(decimal basePrice, decimal adjustmentPercentage)
    {
        var adjustedPrice = basePrice * (1m + (adjustmentPercentage / 100m));
        return RoundSellingPrice(adjustedPrice);
    }

    public static decimal RoundSellingPrice(decimal price)
    {
        if (price <= 0m)
        {
            return 0m;
        }

        return price < 5m
            ? decimal.Ceiling(price * 10m) / 10m
            : decimal.Ceiling(price * 2m) / 2m;
    }
}
