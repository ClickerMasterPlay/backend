package com.resourcemagnate.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.dailyreward.DailyRewardCatalog;
import com.resourcemagnate.app.dailyreward.DailyRewardService;
import com.resourcemagnate.app.dailyreward.model.DailyRewardClaim;
import com.resourcemagnate.app.dailyreward.model.DailyRewardProgress;
import com.resourcemagnate.app.dailyreward.repository.DailyRewardClaimRepository;
import com.resourcemagnate.app.dailyreward.repository.DailyRewardProgressRepository;
import com.resourcemagnate.app.economy.EconomyService;
import com.resourcemagnate.app.economy.dto.BalanceResponse;
import com.resourcemagnate.app.economy.model.LedgerOperationType;
import com.resourcemagnate.app.progress.PlayerProgress;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import com.resourcemagnate.app.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyRewardServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-11-09");

    @Mock private DailyRewardProgressRepository dailyProgressRepository;
    @Mock private DailyRewardClaimRepository claimRepository;
    @Mock private PlayerProgressRepository playerProgressRepository;
    @Mock private UserRepository userRepository;
    @Mock private EconomyService economyService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private DailyRewardService service;
    private UUID userId;
    private UUID idempotencyKey;
    private PlayerProgress player;
    private DailyRewardProgress dailyProgress;

    @BeforeEach
    void setUp() {
        service = new DailyRewardService(
                new DailyRewardCatalog(),
                dailyProgressRepository,
                claimRepository,
                playerProgressRepository,
                userRepository,
                economyService,
                objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        userId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();
        player = new PlayerProgress();
        player.setUserId(userId);
        player.setCredits(0L);
        player.setResources(new HashMap<>(Map.of("stone", 5L, "diamond", 2L)));
        dailyProgress = new DailyRewardProgress();
        dailyProgress.setUserId(userId);
    }

    @Test
    void status_shouldExposeFirstRewardForNewPlayer() {
        when(userRepository.existsById(userId)).thenReturn(true);
        when(dailyProgressRepository.findById(userId)).thenReturn(Optional.empty());

        var response = service.status(userId);

        assertTrue(response.available());
        assertEquals(1, response.cycleDay());
        assertEquals(Map.of("stone", 10L), response.reward());
        assertEquals(TODAY, response.serverDate());
    }

    @Test
    void status_shouldBeUnavailableAfterTodaysClaim() {
        dailyProgress.setCycleDay(4);
        dailyProgress.setLastClaimedDate(TODAY);
        when(userRepository.existsById(userId)).thenReturn(true);
        when(dailyProgressRepository.findById(userId))
                .thenReturn(Optional.of(dailyProgress));

        var response = service.status(userId);

        assertFalse(response.available());
        assertEquals(4, response.cycleDay());
        assertEquals(Map.of("stone", 20L), response.reward());
    }

    @Test
    void claim_shouldGrantMixedRewardAndAdvanceCycle() {
        dailyProgress.setCycleDay(3);
        stubClaim();
        mutateResourcesWhenGranted();

        var response = service.claim(userId, idempotencyKey);

        assertEquals(3, response.cycleDay());
        assertEquals(Map.of("stone", 15L, "diamond", 1L), response.grantedResources());
        assertEquals(20L, response.resources().get("stone"));
        assertEquals(3L, response.resources().get("diamond"));
        assertEquals(4, response.nextCycleDay());
        assertEquals(TODAY, dailyProgress.getLastClaimedDate());
        assertEquals(1L, dailyProgress.getTotalClaims());
        verify(claimRepository).saveAndFlush(any(DailyRewardClaim.class));
        verify(economyService).recordResourceOperation(
                userId, LedgerOperationType.DAILY_REWARD, "stone", 15L, 0L
        );
        verify(economyService).recordResourceOperation(
                userId, LedgerOperationType.DAILY_REWARD, "diamond", 1L, 0L
        );
    }

    @Test
    void claim_shouldWrapDaySevenBackToDayOne() {
        dailyProgress.setCycleDay(7);
        stubClaim();
        mutateResourcesWhenGranted();

        var response = service.claim(userId, idempotencyKey);

        assertEquals(Map.of("stone", 50L, "diamond", 5L), response.grantedResources());
        assertEquals(1, response.nextCycleDay());
    }

    @Test
    void claim_shouldRejectSecondRewardOnSameServerDate() {
        dailyProgress.setLastClaimedDate(TODAY);
        stubClaim();

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.claim(userId, idempotencyKey)
        );

        assertEquals("DAILY_REWARD_ALREADY_CLAIMED", error.getErrorCode());
        verify(economyService, never()).addResource(any(), any(), any(Long.class));
        verify(claimRepository, never()).saveAndFlush(any());
    }

    @Test
    void claim_shouldReplayStoredResponseForSameKey() {
        var expected = new com.resourcemagnate.app.dailyreward.dto.DailyRewardClaimResponse(
                1,
                Map.of("stone", 10L),
                Map.of("stone", 10L),
                2,
                Instant.parse("2026-11-10T00:00:00Z")
        );
        DailyRewardClaim saved = new DailyRewardClaim();
        saved.setUserId(userId);
        saved.setResponseJson(objectMapper.valueToTree(expected));
        when(claimRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(saved));

        var replay = service.claim(userId, idempotencyKey);

        assertEquals(expected, replay);
        verify(playerProgressRepository, never()).findByUserIdForUpdate(any());
    }

    private void stubClaim() {
        when(claimRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(userRepository.existsById(userId)).thenReturn(true);
        when(playerProgressRepository.findByUserIdForUpdate(userId))
                .thenReturn(Optional.of(player));
        when(dailyProgressRepository.findByUserIdForUpdate(userId))
                .thenReturn(Optional.of(dailyProgress));
    }

    private void mutateResourcesWhenGranted() {
        doAnswer(invocation -> {
            PlayerProgress target = invocation.getArgument(0);
            String code = invocation.getArgument(1);
            long amount = invocation.getArgument(2);
            Map<String, Object> resources = new HashMap<>(target.getResources());
            long current = ((Number) resources.getOrDefault(code, 0L)).longValue();
            resources.put(code, current + amount);
            target.setResources(resources);
            return null;
        }).when(economyService).addResource(any(), any(), any(Long.class));
        when(economyService.toBalance(player)).thenAnswer(invocation -> {
            Map<String, Long> resources = new HashMap<>();
            player.getResources().forEach((code, amount) ->
                    resources.put(code, ((Number) amount).longValue())
            );
            return new BalanceResponse(0L, Map.copyOf(resources));
        });
    }
}
