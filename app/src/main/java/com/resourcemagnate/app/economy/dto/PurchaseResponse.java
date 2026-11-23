package com.resourcemagnate.app.economy.dto;

import com.resourcemagnate.app.economy.model.ProductType;
import com.resourcemagnate.app.economy.model.PurchaseStatus;

import java.util.UUID;

public record PurchaseResponse(
        UUID purchaseId,
        ProductType productType,
        String productId,
        long price,
        PurchaseStatus status,
        BalanceResponse balance,
        double clickPower
) {
}
