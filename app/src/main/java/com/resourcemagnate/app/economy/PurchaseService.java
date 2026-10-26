package com.resourcemagnate.app.economy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.catalog.ProductCatalogService;
import com.resourcemagnate.app.economy.catalog.ProductDefinition;
import com.resourcemagnate.app.economy.dto.PurchaseRequest;
import com.resourcemagnate.app.economy.dto.PurchaseResponse;
import com.resourcemagnate.app.economy.model.*;
import com.resourcemagnate.app.economy.repository.InventoryMachineRepository;
import com.resourcemagnate.app.economy.repository.InventoryToolRepository;
import com.resourcemagnate.app.economy.repository.PurchaseRepository;
import com.resourcemagnate.app.progress.PlayerProgress;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import com.resourcemagnate.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final InventoryToolRepository inventoryToolRepository;
    private final InventoryMachineRepository inventoryMachineRepository;
    private final UserRepository userRepository;
    private final PlayerProgressRepository progressRepository;
    private final ProductCatalogService catalogService;
    private final EconomyService economyService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public PurchaseResponse purchase(
            UUID userId,
            UUID idempotencyKey,
            PurchaseRequest request
    ) {
        validateInput(userId, idempotencyKey, request);

        var existing = purchaseRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), userId, request);
        }

        ProductDefinition product = catalogService.get(
                request.productType(), request.productId()
        );

        if (!userRepository.existsById(userId)) {
            throw new BusinessException("USER_NOT_FOUND", "User not found");
        }

        // Deliberately no pessimistic lock: @Version detects concurrent writes.
        PlayerProgress progress = progressRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        "PROGRESS_NOT_FOUND", "Player progress not found"
                ));

        economyService.debitCredits(progress, product.price());
        applyProduct(userId, progress, product);

        // Force the version check inside this method rather than at transaction commit.
        progressRepository.saveAndFlush(progress);

        UUID purchaseId = UUID.randomUUID();
        PurchaseResponse response = new PurchaseResponse(
                purchaseId,
                product.type(),
                product.id(),
                product.price(),
                PurchaseStatus.COMPLETED,
                economyService.toBalance(progress),
                progress.getClickPower()
        );

        Purchase purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setUserId(userId);
        purchase.setProductType(product.type());
        purchase.setProductId(product.id());
        purchase.setPrice(product.price());
        purchase.setStatus(PurchaseStatus.COMPLETED);
        purchase.setIdempotencyKey(idempotencyKey);
        purchase.setResponseJson(objectMapper.valueToTree(response));
        purchaseRepository.saveAndFlush(purchase);

        economyService.recordCurrencyOperation(
                userId,
                LedgerOperationType.PURCHASE,
                -product.price(),
                product.type(),
                product.id(),
                purchaseId,
                progress.getCredits()
        );

        return response;
    }

    private void applyProduct(
            UUID userId,
            PlayerProgress progress,
            ProductDefinition product
    ) {
        switch (product.type()) {
            case CLICK_UPGRADE -> economyService.increaseClickPower(
                    progress, product.clickPowerIncrease()
            );
            case TOOL -> addTool(userId, product.id());
            case MACHINE -> addMachine(userId, product.id());
        }
    }

    private void addTool(UUID userId, String toolCode) {
        if (inventoryToolRepository.existsByUserIdAndToolCode(userId, toolCode)) {
            throw new BusinessException("PRODUCT_ALREADY_OWNED", "Tool already owned");
        }
        InventoryTool item = new InventoryTool();
        item.setUserId(userId);
        item.setToolCode(toolCode);
        inventoryToolRepository.save(item);
    }

    private void addMachine(UUID userId, String machineCode) {
        if (inventoryMachineRepository.existsByUserIdAndMachineCode(userId, machineCode)) {
            throw new BusinessException("PRODUCT_ALREADY_OWNED", "Machine already owned");
        }
        InventoryMachine item = new InventoryMachine();
        item.setUserId(userId);
        item.setMachineCode(machineCode);
        item.setLastCollectedAt(clock.instant());
        inventoryMachineRepository.save(item);
    }

    private PurchaseResponse replay(
            Purchase saved,
            UUID userId,
            PurchaseRequest request
    ) {
        if (!saved.getUserId().equals(userId)
                || saved.getProductType() != request.productType()
                || !saved.getProductId().equals(request.productId())) {
            throw new BusinessException(
                    "IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key was already used for another purchase"
            );
        }
        try {
            return objectMapper.treeToValue(
                    saved.getResponseJson(), PurchaseResponse.class
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot read stored purchase response", ex);
        }
    }

    private void validateInput(
            UUID userId,
            UUID idempotencyKey,
            PurchaseRequest request
    ) {
        if (userId == null) {
            throw new BusinessException("VALIDATION_ERROR", "X-User-Id is required");
        }
        if (idempotencyKey == null) {
            throw new BusinessException("VALIDATION_ERROR", "Idempotency-Key is required");
        }
        if (request == null || request.productType() == null
                || request.productId() == null || request.productId().isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "Valid product is required");
        }
    }
}
