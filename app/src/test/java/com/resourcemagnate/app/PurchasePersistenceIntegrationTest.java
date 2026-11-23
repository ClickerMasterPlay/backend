package com.resourcemagnate.app;

import com.resourcemagnate.app.auth.AuthService;
import com.resourcemagnate.app.economy.PurchaseService;
import com.resourcemagnate.app.economy.dto.PurchaseRequest;
import com.resourcemagnate.app.economy.model.ProductType;
import com.resourcemagnate.app.economy.model.PurchaseStatus;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class PurchasePersistenceIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private PurchaseService purchaseService;
    @Autowired private PlayerProgressRepository progressRepository;

    @Test
    void nativeReservation_shouldCompleteAndReplayTheSamePurchase() {
        UUID userId = authService.createGuest().getUserId();
        var progress = progressRepository.findByUserId(userId).orElseThrow();
        progress.setCredits(500L);
        progressRepository.saveAndFlush(progress);

        UUID key = UUID.randomUUID();
        PurchaseRequest request = new PurchaseRequest(
                ProductType.CLICK_UPGRADE, "click_power_1"
        );

        var first = purchaseService.purchase(userId, key, request);
        var replay = purchaseService.purchase(userId, key, request);

        assertEquals(PurchaseStatus.COMPLETED, first.status());
        assertEquals(first, replay);
        assertEquals(400L, first.balance().credits());
    }
}
