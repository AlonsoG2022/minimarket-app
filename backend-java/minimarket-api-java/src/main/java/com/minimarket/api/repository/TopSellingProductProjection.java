package com.minimarket.api.repository;

import java.math.BigDecimal;

public interface TopSellingProductProjection {
    Integer getProductId();
    String getProductName();
    String getSku();
    Integer getTotalQuantity();
    BigDecimal getTotalAmount();
}
