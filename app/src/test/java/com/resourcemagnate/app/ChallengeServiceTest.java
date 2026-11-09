package com.resourcemagnate.app;

import com.resourcemagnate.app.challenge.ChallengeCatalog;
import com.resourcemagnate.app.challenge.ChallengeService;
import com.resourcemagnate.app.challenge.dto.ChallengeClaimResponse;
import com.resourcemagnate.app.challenge.model.ChallengeProgress;
import com.resourcemagnate.app.challenge.model.ClaimedChallenge;
import com.resourcemagnate.app.challenge.repository.ChallengeProgressRepository;
import com.resourcemagnate.app.challenge.repository.ClaimedChallengeRepository;
import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.EconomyService;
import com.resourcemagnate.app.economy.dto.BalanceResponse;
import com.resourcemagnate.app.economy.model.LedgerOperationType;
import com.resourcemagnate.app.game.repository.ToolRepository;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock private ChallengeProgressRepository challengeProgressRepository;
    @Mock private ClaimedChallengeRepository claimedChallengeRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlayerProgressRepository playerProgressRepository;
    @Mock private ToolRepository toolRepository;
    @Mock private EconomyService economyService;

    private ChallengeService challengeService;
    private UUID userId;
    private ChallengeProgress challengeProgress;
    private PlayerProgress playerProgress;

    @BeforeEach
    void setUp() {
        challengeService = new ChallengeService(
                new ChallengeCatalog(),
                challengeProgressRepository,
                claimedChallengeRepository,
                userRepository,
                playerProgressRepository,
                toolRepository,
                economyService,
                Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC)
        );
        userId = UUID.randomUUID();
        challengeProgress = new ChallengeProgress();
        challengeProgress.setUserId(userId);
        playerProgress = new PlayerProgress();
        playerProgress.setUserId(userId);
        playerProgress.setCredits(10L);
        playerProgress.setClickPower(1.0);
        playerProgress.setResources(Map.of());
    }

    @Test
    void recordsOnlyAcceptedClickBatchCounters() {
        when(challengeProgressRepository.findByUserIdForUpdate(userId))
                .thenReturn(Optional.of(challengeProgress));

        challengeService.recordClickBatch(userId, 15, true, 2L);

        assertEquals(15L, challengeProgress.getTotalClicks());
        assertEquals(1L, challengeProgress.getBlocksBroken());
        assertEquals(2L, challengeProgress.getCreditsEarned());
        verify(challengeProgressRepository).save(challengeProgress);
    }

    @Test
    void returnsFirstThreeUnclaimedChallengesWithGlobalProgress() {
        challengeProgress.setTotalClicks(15);
        challengeProgress.setBlocksBroken(1);
        challengeProgress.setCreditsEarned(2);
        when(userRepository.existsById(userId)).thenReturn(true);
        when(playerProgressRepository.findByUserId(userId))
                .thenReturn(Optional.of(playerProgress));
        when(challengeProgressRepository.findById(userId))
                .thenReturn(Optional.of(challengeProgress));
        when(claimedChallengeRepository.findAllByUserIdOrderByChallengeIndex(userId))
                .thenReturn(List.of());

        var response = challengeService.getChallenges(userId);

        assertEquals(60, response.total());
        assertEquals(List.of(0, 1, 2), response.items().stream()
                .map(item -> item.index()).toList());
        assertTrue(response.items().stream().allMatch(item -> item.ready()));
        assertEquals(1L, response.stats().level());
    }

    @Test
    void claimsCompletedVisibleChallengeAtomically() {
        challengeProgress.setTotalClicks(15);
        when(userRepository.existsById(userId)).thenReturn(true);
        when(playerProgressRepository.findByUserIdForUpdate(userId))
                .thenReturn(Optional.of(playerProgress));
        when(challengeProgressRepository.findByUserIdForUpdate(userId))
                .thenReturn(Optional.of(challengeProgress));
        when(claimedChallengeRepository.findAllByUserIdOrderByChallengeIndex(userId))
                .thenReturn(List.of());
        doAnswer(invocation -> {
            PlayerProgress progress = invocation.getArgument(0);
            progress.setCredits(progress.getCredits() + invocation.<Long>getArgument(1));
            return null;
        }).when(economyService).creditCredits(any(PlayerProgress.class), any(Long.class));
        when(economyService.toBalance(playerProgress))
                .thenReturn(new BalanceResponse(12L, Map.of()));

        ChallengeClaimResponse response = challengeService.claim(userId, 0);

        assertEquals(2L, response.reward());
        assertEquals(12L, response.balance().credits());
        assertEquals(List.of(1, 2, 3), response.challenges().items().stream()
                .map(item -> item.index()).toList());
        verify(claimedChallengeRepository).saveAndFlush(any(ClaimedChallenge.class));
        verify(economyService).recordCurrencyOperation(
                userId,
                LedgerOperationType.CHALLENGE_REWARD,
                2L,
                null,
                "challenge-0",
                null,
                12L
        );
    }

    @Test
    void rejectsIncompleteChallengeWithoutReward() {
        when(userRepository.existsById(userId)).thenReturn(true);
        when(playerProgressRepository.findByUserIdForUpdate(userId))
                .thenReturn(Optional.of(playerProgress));
        when(challengeProgressRepository.findByUserIdForUpdate(userId))
                .thenReturn(Optional.of(challengeProgress));
        when(claimedChallengeRepository.findAllByUserIdOrderByChallengeIndex(userId))
                .thenReturn(List.of());

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> challengeService.claim(userId, 0)
        );

        assertEquals("CHALLENGE_NOT_COMPLETED", error.getErrorCode());
        verify(economyService, never()).creditCredits(any(), any(Long.class));
        verify(claimedChallengeRepository, never()).saveAndFlush(any());
    }
}
