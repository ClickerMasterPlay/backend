package com.resourcemagnate.app.dailyreward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.dailyreward.dto.DailyRewardClaimResponse;
import com.resourcemagnate.app.dailyreward.dto.DailyRewardStatusResponse;
import com.resourcemagnate.app.dailyreward.model.DailyRewardClaim;
import com.resourcemagnate.app.dailyreward.model.DailyRewardProgress;
import com.resourcemagnate.app.dailyreward.repository.DailyRewardClaimRepository;
import com.resourcemagnate.app.dailyreward.repository.DailyRewardProgressRepository;
import com.resourcemagnate.app.economy.EconomyService;
import com.resourcemagnate.app.economy.model.LedgerOperationType;
import com.resourcemagnate.app.progress.PlayerProgress;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import com.resourcemagnate.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DailyRewardService {

    private final DailyRewardCatalog catalog;
    private final DailyRewardProgressRepository dailyProgressRepository;
    private final DailyRewardClaimRepository claimRepository;
    private final PlayerProgressRepository playerProgressRepository;
    private final UserRepository userRepository;
    private final EconomyService economyService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DailyRewardStatusResponse status(UUID userId) {
        requireUser(userId);
        LocalDate today = today();
        DailyRewardProgress progress = dailyProgressRepository.findById(userId)
                .orElseGet(() -> emptyProgress(userId));
        return new DailyRewardStatusResponse(
                !today.equals(progress.getLastClaimedDate()),
                progress.getCycleDay(),
                catalog.rewardFor(progress.getCycleDay()),
                today,
                nextResetAt(today)
        );
    }

    @Transactional
    public DailyRewardClaimResponse claim(UUID userId, UUID idempotencyKey) {
        validateClaim(userId, idempotencyKey);
        var existing = claimRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), userId);
        }

        requireUser(userId);
        // Keep the global lock order consistent with game, shop and challenges.
        PlayerProgress player = playerProgressRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(
                        "PROGRESS_NOT_FOUND", "Player progress not found"
                ));

        // The first lookup may have raced with another request waiting on the lock.
        existing = claimRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), userId);
        }

        DailyRewardProgress dailyProgress = dailyProgressRepository
                .findByUserIdForUpdate(userId)
                .orElseGet(() -> dailyProgressRepository.saveAndFlush(
                        emptyProgress(userId)
                ));
        LocalDate today = today();
        if (today.equals(dailyProgress.getLastClaimedDate())) {
            throw new BusinessException(
                    "DAILY_REWARD_ALREADY_CLAIMED",
                    "Daily reward has already been claimed"
            );
        }

        int claimedDay = dailyProgress.getCycleDay();
        Map<String, Long> reward = catalog.rewardFor(claimedDay);
        reward.forEach((resourceCode, amount) ->
                economyService.addResource(player, resourceCode, amount)
        );
        playerProgressRepository.saveAndFlush(player);

        dailyProgress.setLastClaimedDate(today);
        dailyProgress.setCycleDay(catalog.nextDay(claimedDay));
        try {
            dailyProgress.setTotalClaims(Math.addExact(
                    dailyProgress.getTotalClaims(), 1
            ));
        } catch (ArithmeticException ex) {
            throw new IllegalStateException("Daily reward claim counter overflow", ex);
        }
        dailyProgressRepository.saveAndFlush(dailyProgress);

        DailyRewardClaimResponse response = new DailyRewardClaimResponse(
                claimedDay,
                reward,
                economyService.toBalance(player).resources(),
                dailyProgress.getCycleDay(),
                nextResetAt(today)
        );

        DailyRewardClaim claim = new DailyRewardClaim();
        claim.setId(UUID.randomUUID());
        claim.setUserId(userId);
        claim.setRewardDate(today);
        claim.setCycleDay(claimedDay);
        claim.setRewardJson(objectMapper.valueToTree(reward));
        claim.setResponseJson(objectMapper.valueToTree(response));
        claim.setIdempotencyKey(idempotencyKey);
        claim.setClaimedAt(clock.instant());
        claimRepository.saveAndFlush(claim);

        reward.forEach((resourceCode, amount) ->
                economyService.recordResourceOperation(
                        userId,
                        LedgerOperationType.DAILY_REWARD,
                        resourceCode,
                        amount,
                        player.getCredits()
                )
        );
        return response;
    }

    private DailyRewardClaimResponse replay(DailyRewardClaim claim, UUID userId) {
        if (!claim.getUserId().equals(userId)) {
            throw new BusinessException(
                    "IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key was already used by another player"
            );
        }
        try {
            return objectMapper.treeToValue(
                    claim.getResponseJson(), DailyRewardClaimResponse.class
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot read stored daily reward response", ex);
        }
    }

    private DailyRewardProgress emptyProgress(UUID userId) {
        DailyRewardProgress progress = new DailyRewardProgress();
        progress.setUserId(userId);
        progress.setCycleDay(1);
        return progress;
    }

    private LocalDate today() {
        return clock.instant().atZone(ZoneOffset.UTC).toLocalDate();
    }

    private Instant nextResetAt(LocalDate today) {
        return today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private void validateClaim(UUID userId, UUID idempotencyKey) {
        if (userId == null) {
            throw new BusinessException("VALIDATION_ERROR", "X-User-Id is required");
        }
        if (idempotencyKey == null) {
            throw new BusinessException("VALIDATION_ERROR", "Idempotency-Key is required");
        }
    }

    private void requireUser(UUID userId) {
        if (userId == null) {
            throw new BusinessException("VALIDATION_ERROR", "X-User-Id is required");
        }
        if (!userRepository.existsById(userId)) {
            throw new BusinessException("USER_NOT_FOUND", "User not found");
        }
    }
}
