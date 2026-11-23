package com.resourcemagnate.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resourcemagnate.app.auth.dto.AuthResponse;
import com.resourcemagnate.app.economy.dto.BalanceResponse;
import com.resourcemagnate.app.economy.dto.MachineBankStateResponse;
import com.resourcemagnate.app.economy.dto.MachineCollectionResponse;
import com.resourcemagnate.app.progress.dto.ProgressResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void guestFlag_shouldBeSerializedAsIsGuestForFrontendContract() throws Exception {
        String authJson = objectMapper.writeValueAsString(AuthResponse.builder()
                .userId(UUID.randomUUID())
                .isGuest(true)
                .offlineCredits(0L)
                .build());
        String progressJson = objectMapper.writeValueAsString(ProgressResponse.builder()
                .userId(UUID.randomUUID())
                .isGuest(true)
                .resources(Map.of())
                .build());

        assertTrue(authJson.contains("\"isGuest\":true"));
        assertTrue(progressJson.contains("\"isGuest\":true"));
        assertFalse(authJson.contains("\"guest\":"));
        assertFalse(progressJson.contains("\"guest\":"));
    }

    @Test
    void machineCollect_shouldExposeCreditsBalanceTimeAndBankState()
            throws Exception {
        Instant serverTime = Instant.parse("2026-11-23T12:00:00Z");
        var response = new MachineCollectionResponse(
                10L,
                new BalanceResponse(1_250L, Map.of()),
                serverTime,
                List.of(new MachineBankStateResponse(
                        "basic_miner", 600L, 0L, 864_000L, false
                ))
        );

        var json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertEquals(10L, json.get("creditedCredits").longValue());
        assertEquals(1_250L, json.get("balance").get("credits").longValue());
        assertEquals(serverTime.toString(), json.get("serverTime").textValue());
        assertEquals(600L, json.get("banks").get(0)
                .get("releasedCredits").longValue());
        assertEquals(0L, json.get("banks").get(0)
                .get("filledCredits").longValue());
        assertFalse(json.has("collectedCredits"));
        assertFalse(json.has("collectedAt"));
    }
}
