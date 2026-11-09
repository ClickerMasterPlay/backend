package com.resourcemagnate.app;

import com.resourcemagnate.app.auth.AuthService;
import com.resourcemagnate.app.dailyreward.DailyRewardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Transactional
class DailyRewardPersistenceIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private DailyRewardService dailyRewardService;

    @Test
    void firstClaim_shouldPersistResourcesAndReplayWithoutDoubleReward() {
        UUID userId = authService.createGuest().getUserId();
        UUID key = UUID.randomUUID();

        var first = dailyRewardService.claim(userId, key);
        var replay = dailyRewardService.claim(userId, key);
        var status = dailyRewardService.status(userId);

        assertEquals(Map.of("stone", 10L), first.grantedResources());
        assertEquals(10L, first.resources().get("stone"));
        assertEquals(first, replay);
        assertEquals(2, first.nextCycleDay());
        assertFalse(status.available());
        assertEquals(2, status.cycleDay());
        assertEquals(Map.of("diamond", 1L), status.reward());
    }
}
