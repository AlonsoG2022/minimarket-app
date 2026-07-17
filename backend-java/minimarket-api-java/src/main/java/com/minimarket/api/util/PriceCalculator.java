package com.minimarket.api.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PriceCalculator {

    private static final BigDecimal FIVE = new BigDecimal("5");
    private static final BigDecimal TEN = new BigDecimal("10");
    private static final BigDecimal TWO = new BigDecimal("2");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private PriceCalculator() {
    }

    public static BigDecimal applyCategoryAdjustment(BigDecimal basePrice, BigDecimal adjustmentPercentage) {
        var safeAdjustment = adjustmentPercentage != null ? adjustmentPercentage : BigDecimal.ZERO;
        var adjustedPrice = basePrice.multiply(
            BigDecimal.ONE.add(safeAdjustment.divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP))
        );
        return roundSellingPrice(adjustedPrice);
    }

    public static BigDecimal roundSellingPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (price.compareTo(FIVE) < 0) {
            return price
                .multiply(TEN)
                .setScale(0, RoundingMode.CEILING)
                .divide(TEN, 1, RoundingMode.UNNECESSARY);
        }

        return price
            .multiply(TWO)
            .setScale(0, RoundingMode.CEILING)
            .divide(TWO, 1, RoundingMode.UNNECESSARY);
    }
}
