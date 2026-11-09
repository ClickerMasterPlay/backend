package com.resourcemagnate.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resourcemagnate.app.auth.dto.AuthResponse;
import com.resourcemagnate.app.progress.dto.ProgressResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void guestFlag_shouldBeSerializedAsIsGuestForFrontendContract() throws Exception {
        String authJson = objectMapper.writeValueAsString(AuthResponse.builder()
                .userId(UUID.randomUUID())
                .isGuest(true)
                .offlineRewards(Map.of())
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
}
