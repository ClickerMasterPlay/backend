package com.resourcemagnate.app.economy;

import com.resourcemagnate.app.economy.catalog.ProductCatalogService;
import com.resourcemagnate.app.economy.dto.CatalogResponse;
import com.resourcemagnate.app.economy.dto.PurchaseRequest;
import com.resourcemagnate.app.economy.dto.PurchaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shop")
@RequiredArgsConstructor
public class ShopController {

    private final PurchaseService purchaseService;
    private final ProductCatalogService catalogService;

    @PostMapping("/purchase")
    public ResponseEntity<PurchaseResponse> purchase(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody PurchaseRequest request
    ) {
        return ResponseEntity.ok(
                purchaseService.purchase(userId, idempotencyKey, request)
        );
    }

    @GetMapping("/catalog")
    public ResponseEntity<CatalogResponse> catalog() {
        return ResponseEntity.ok(catalogService.getCatalog());
    }
}
