package com.resourcemagnate.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resourcemagnate.app.challenge.ChallengeService;
import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.EconomyService;
import com.resourcemagnate.app.economy.PurchaseService;
import com.resourcemagnate.app.economy.catalog.ProductCatalogService;
import com.resourcemagnate.app.economy.dto.BalanceResponse;
import com.resourcemagnate.app.economy.dto.PurchaseRequest;
import com.resourcemagnate.app.economy.dto.PurchaseResponse;
import com.resourcemagnate.app.economy.model.*;
import com.resourcemagnate.app.economy.repository.InventoryMachineRepository;
import com.resourcemagnate.app.economy.repository.InventoryToolRepository;
import com.resourcemagnate.app.economy.repository.LedgerRepository;
import com.resourcemagnate.app.economy.repository.PurchaseRepository;
import com.resourcemagnate.app.progress.PlayerProgress;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import com.resourcemagnate.app.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock private PurchaseRepository purchaseRepository;
    @Mock private InventoryToolRepository inventoryToolRepository;
    @Mock private InventoryMachineRepository inventoryMachineRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlayerProgressRepository progressRepository;
    @Mock private LedgerRepository ledgerRepository;
    @Mock private ChallengeService challengeService;

    private PurchaseService purchaseService;
    private ObjectMapper objectMapper;
    private UUID userId;
    private UUID idempotencyKey;
    private PlayerProgress progress;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        EconomyService economyService = new EconomyService(
                userRepository, progressRepository, ledgerRepository
        );
        purchaseService = new PurchaseService(
                purchaseRepository,
                inventoryToolRepository,
                inventoryMachineRepository,
                userRepository,
                progressRepository,
                new ProductCatalogService(),
                economyService,
                objectMapper,
                Clock.systemUTC(),
                challengeService
        );

        userId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();
        progress = new PlayerProgress();
        progress.setUserId(userId);
        progress.setCredits(500L);
        progress.setClickPower(1.0);
        progress.setResources(Map.of("stone", 3L));
    }

    @Test
    void purchase_shouldDebitCreditsUpgradeClickPowerAndWriteLedger() {
        PurchaseRequest request = new PurchaseRequest(
                ProductType.CLICK_UPGRADE, "click_power_1"
        );
        stubNewPurchase(request, 100L);

        PurchaseResponse response = purchaseService.purchase(
                userId, idempotencyKey, request
        );

        assertEquals(100L, response.price());
        assertEquals(400L, response.balance().credits());
        assertEquals(1.5, response.clickPower());
        assertEquals(PurchaseStatus.COMPLETED, response.status());
        assertEquals(400L, progress.getCredits());
        assertEquals(1.5, progress.getClickPower());

        verify(progressRepository).saveAndFlush(progress);
        verify(purchaseRepository).saveAndFlush(any(Purchase.class));
        verify(ledgerRepository).save(argThat(entry ->
                entry.getOperationType() == LedgerOperationType.PURCHASE
                        && entry.getAmount() == -100L
                        && entry.getBalanceAfter() == 400L
        ));
        verify(challengeService).recordPurchase(userId, 100L);
    }

    @Test
    void purchase_shouldNotChangeBalanceWhenFundsAreInsufficient() {
        progress.setCredits(50L);
        PurchaseRequest request = new PurchaseRequest(
                ProductType.TOOL, "wooden_pickaxe"
        );
        when(purchaseRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(userRepository.existsById(userId)).thenReturn(true);
        when(purchaseRepository.reserve(
                any(UUID.class), eq(userId), eq("TOOL"), eq("wooden_pickaxe"),
                eq(250L), eq(idempotencyKey)
        )).thenReturn(1);
        when(purchaseRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> Optional.of(
                        reservedPurchase(
                                invocation.getArgument(0),
                                new PurchaseRequest(ProductType.TOOL, "wooden_pickaxe"),
                                250L
                        )
                ));
        when(progressRepository.findByUserId(userId)).thenReturn(Optional.of(progress));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> purchaseService.purchase(userId, idempotencyKey, request)
        );

        assertEquals("INSUFFICIENT_FUNDS", exception.getErrorCode());
        assertEquals(50L, progress.getCredits());
        verify(progressRepository, never()).saveAndFlush(any());
        verify(purchaseRepository, never()).saveAndFlush(any());
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void purchase_shouldReplayStoredResponseForSameIdempotencyKey() {
        PurchaseRequest request = new PurchaseRequest(
                ProductType.TOOL, "iron_pickaxe"
        );
        PurchaseResponse original = new PurchaseResponse(
                UUID.randomUUID(),
                ProductType.TOOL,
                "iron_pickaxe",
                800L,
                PurchaseStatus.COMPLETED,
                new BalanceResponse(450L, Map.of("stone", 120L)),
                1.0
        );
        Purchase saved = savedPurchase(request, original);
        when(purchaseRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(saved));

        PurchaseResponse replayed = purchaseService.purchase(
                userId, idempotencyKey, request
        );

        assertEquals(original, replayed);
        verifyNoInteractions(userRepository, progressRepository, ledgerRepository);
    }

    @Test
    void purchase_shouldRejectReusedKeyForDifferentProduct() {
        PurchaseRequest originalRequest = new PurchaseRequest(
                ProductType.TOOL, "iron_pickaxe"
        );
        PurchaseResponse original = new PurchaseResponse(
                UUID.randomUUID(), ProductType.TOOL, "iron_pickaxe", 800L,
                PurchaseStatus.COMPLETED,
                new BalanceResponse(0L, Map.of()), 1.0
        );
        when(purchaseRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(savedPurchase(originalRequest, original)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> purchaseService.purchase(
                        userId,
                        idempotencyKey,
                        new PurchaseRequest(ProductType.TOOL, "wooden_pickaxe")
                )
        );

        assertEquals("IDEMPOTENCY_KEY_REUSED", exception.getErrorCode());
        verifyNoInteractions(progressRepository, ledgerRepository);
    }

    @Test
    void purchase_shouldReplayWinnerOfConcurrentIdempotencyReservation() {
        PurchaseRequest request = new PurchaseRequest(
                ProductType.CLICK_UPGRADE, "click_power_1"
        );
        PurchaseResponse original = new PurchaseResponse(
                UUID.randomUUID(), ProductType.CLICK_UPGRADE, "click_power_1", 100L,
                PurchaseStatus.COMPLETED,
                new BalanceResponse(400L, Map.of()), 1.5
        );
        when(purchaseRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty(), Optional.of(savedPurchase(request, original)));
        when(userRepository.existsById(userId)).thenReturn(true);
        when(purchaseRepository.reserve(
                any(UUID.class), eq(userId), eq("CLICK_UPGRADE"),
                eq("click_power_1"), eq(100L), eq(idempotencyKey)
        )).thenReturn(0);

        PurchaseResponse replayed = purchaseService.purchase(
                userId, idempotencyKey, request
        );

        assertEquals(original, replayed);
        verifyNoInteractions(progressRepository, ledgerRepository, challengeService);
    }

    @Test
    void purchase_shouldDebitCreditsAndAddToolToInventory() {
        PurchaseRequest request = new PurchaseRequest(
                ProductType.TOOL, "wooden_pickaxe"
        );
        stubNewPurchase(request, 250L);

        PurchaseResponse response = purchaseService.purchase(
                userId, idempotencyKey, request
        );

        assertEquals(250L, response.price());
        assertEquals(250L, response.balance().credits());
        verify(inventoryToolRepository).save(argThat(item ->
                item.getUserId().equals(userId)
                        && item.getToolCode().equals("wooden_pickaxe")
        ));
        verify(ledgerRepository).save(argThat(entry ->
                entry.getAmount() == -250L
                        && entry.getProductType() == ProductType.TOOL
        ));
    }

    @Test
    void purchase_shouldDebitCreditsAndAddMachineToInventory() {
        progress.setCredits(2000L);
        PurchaseRequest request = new PurchaseRequest(
                ProductType.MACHINE, "basic_miner"
        );
        stubNewPurchase(request, 1500L);

        PurchaseResponse response = purchaseService.purchase(
                userId, idempotencyKey, request
        );

        assertEquals(500L, response.balance().credits());
        verify(inventoryMachineRepository).save(argThat(item ->
                item.getUserId().equals(userId)
                        && item.getMachineCode().equals("basic_miner")
                        && !item.isInstalled()
                        && item.getInstalledAt() == null
        ));
        verify(ledgerRepository).save(argThat(entry ->
                entry.getAmount() == -1500L
                        && entry.getProductType() == ProductType.MACHINE
        ));
    }

    private void stubNewPurchase(PurchaseRequest request, long price) {
        when(purchaseRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(userRepository.existsById(userId)).thenReturn(true);
        when(purchaseRepository.reserve(
                any(UUID.class), eq(userId), anyString(), anyString(),
                anyLong(), eq(idempotencyKey)
        )).thenReturn(1);
        when(purchaseRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> Optional.of(reservedPurchase(
                        invocation.getArgument(0),
                        request,
                        price
                )));
        when(progressRepository.findByUserId(userId)).thenReturn(Optional.of(progress));
        when(progressRepository.saveAndFlush(progress)).thenReturn(progress);
        when(purchaseRepository.saveAndFlush(any(Purchase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Purchase reservedPurchase(
            UUID id,
            PurchaseRequest request,
            long price
    ) {
        Purchase purchase = new Purchase();
        purchase.setId(id);
        purchase.setUserId(userId);
        purchase.setProductType(request.productType());
        purchase.setProductId(request.productId());
        purchase.setPrice(price);
        purchase.setStatus(PurchaseStatus.PROCESSING);
        purchase.setIdempotencyKey(idempotencyKey);
        purchase.setResponseJson(objectMapper.createObjectNode());
        return purchase;
    }

    private Purchase savedPurchase(
            PurchaseRequest request,
            PurchaseResponse response
    ) {
        Purchase saved = new Purchase();
        saved.setId(response.purchaseId());
        saved.setUserId(userId);
        saved.setProductType(request.productType());
        saved.setProductId(request.productId());
        saved.setPrice(response.price());
        saved.setStatus(PurchaseStatus.COMPLETED);
        saved.setIdempotencyKey(idempotencyKey);
        saved.setResponseJson(objectMapper.valueToTree(response));
        return saved;
    }
}
