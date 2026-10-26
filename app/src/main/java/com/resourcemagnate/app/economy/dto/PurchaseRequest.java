package com.resourcemagnate.app.economy.dto;

import com.resourcemagnate.app.economy.model.ProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PurchaseRequest(
        @NotNull ProductType productType,
        @NotBlank String productId
) {
}
