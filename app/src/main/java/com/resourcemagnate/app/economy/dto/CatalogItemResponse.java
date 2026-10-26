package com.resourcemagnate.app.economy.dto;

import com.resourcemagnate.app.economy.model.ProductType;

public record CatalogItemResponse(
        ProductType productType,
        String productId,
        long price,
        String effect
) {
}
