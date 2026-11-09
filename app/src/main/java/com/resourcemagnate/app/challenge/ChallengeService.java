package com.resourcemagnate.app.challenge;

import com.resourcemagnate.app.challenge.dto.ChallengeClaimResponse;
import com.resourcemagnate.app.challenge.dto.ChallengeItemResponse;
import com.resourcemagnate.app.challenge.dto.ChallengeListResponse;
import com.resourcemagnate.app.challenge.dto.ChallengeStatsResponse;
import com.resourcemagnate.app.challenge.model.ChallengeProgress;
import com.resourcemagnate.app.challenge.model.ClaimedChallenge;
import com.resourcemagnate.app.challenge.repository.ChallengeProgressRepository;
import com.resourcemagnate.app.challenge.repository.ClaimedChallengeRepository;
import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.EconomyService;
import com.resourcemagnate.app.economy.model.LedgerOperationType;
import com.resourcemagnate.app.game.model.Tool;
import com.resourcemagnate.app.game.repository.ToolRepository;
import com.resourcemagnate.app.progress.PlayerProgress;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import com.resourcemagnate.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private static final long BLOCKS_PER_LEVEL = 2;
    private static final double EPSILON = 1e-9;

    private final ChallengeCatalog catalog;
    private final ChallengeProgressRepository challengeProgressRepository;
    private final ClaimedChallengeRepository claimedChallengeRepository;
    private final UserRepository userRepository;
    private final PlayerProgressRepository playerProgressRepository;
    private final ToolRepository toolRepository;
    private final EconomyService economyService;
    private final Clock clock;

    @Transactional
    public ChallengeListResponse getChallenges(UUID userId) {
        requireUser(userId);
        PlayerProgress player = playerProgressRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        "PROGRESS_NOT_FOUND", "Player progress not found"
                ));
        ChallengeProgress progress = challengeProgressRepository.findById(userId)
                .orElseGet(() -> emptyProgress(userId));
        return response(progress, player, claimedIndexes(userId));
    }

    /**
     * Records a successfully accepted, non-replayed click batch.
     */
    @Transactional
    public long recordClickBatch(
            UUID userId,
            int clicks,
            boolean blockDestroyed,
            long creditsEarned
    ) {
        if (clicks <= 0 || creditsEarned < 0) {
            throw new IllegalArgumentException("Invalid challenge click event");
        }
        ChallengeProgress progress = getOrCreateForUpdate(userId);
        progress.setTotalClicks(add(progress.getTotalClicks(), clicks, "clicks"));
        if (blockDestroyed) {
            progress.setBlocksBroken(add(progress.getBlocksBroken(), 1, "blocks broken"));
        }
        progress.setCreditsEarned(add(
                progress.getCreditsEarned(), creditsEarned, "credits earned"
        ));
        challengeProgressRepository.save(progress);
        return progress.getBlocksBroken();
    }

    @Transactional(readOnly = true)
    public long blocksBroken(UUID userId) {
        return challengeProgressRepository.findById(userId)
                .map(ChallengeProgress::getBlocksBroken)
                .orElse(0L);
    }

    /**
     * Records a completed, non-replayed purchase.
     */
    @Transactional
    public void recordPurchase(UUID userId, long price) {
        if (price < 0) {
            throw new IllegalArgumentException("Purchase price must not be negative");
        }
        ChallengeProgress progress = getOrCreateForUpdate(userId);
        progress.setCreditsSpent(add(progress.getCreditsSpent(), price, "credits spent"));
        progress.setPurchasesCount(add(progress.getPurchasesCount(), 1, "purchases"));
        challengeProgressRepository.save(progress);
    }

    @Transactional
    public ChallengeClaimResponse claim(UUID userId, int challengeIndex) {
        requireUser(userId);
        ChallengeDefinition definition = catalog.get(challengeIndex);
        if (definition == null) {
            throw new BusinessException("CHALLENGE_NOT_FOUND", "Challenge not found");
        }

        // Player progress is the first lock in game, shop and challenge flows.
        PlayerProgress player = playerProgressRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(
                        "PROGRESS_NOT_FOUND", "Player progress not found"
                ));
        ChallengeProgress progress = getOrCreateForUpdate(userId);
        Set<Integer> claimed = claimedIndexes(userId);

        if (claimed.contains(challengeIndex)) {
            throw new BusinessException(
                    "CHALLENGE_ALREADY_CLAIMED", "Challenge reward was already claimed"
            );
        }
        // Allow claiming ANY completed challenge, not just visible ones.
        // Visibility window is for UI only.
        double power = currentPower(player);
        if (!ready(progress, definition, power)) {
            throw new BusinessException(
                    "CHALLENGE_NOT_COMPLETED", "Challenge goal is not completed"
            );
        }

        economyService.creditCredits(player, definition.reward());
        playerProgressRepository.saveAndFlush(player);

        ClaimedChallenge claim = new ClaimedChallenge();
        claim.setUserId(userId);
        claim.setChallengeIndex(challengeIndex);
        claim.setReward(definition.reward());
        claim.setClaimedAt(clock.instant());
        claimedChallengeRepository.saveAndFlush(claim);

        economyService.recordCurrencyOperation(
                userId,
                LedgerOperationType.CHALLENGE_REWARD,
                definition.reward(),
                null,
                "challenge-" + challengeIndex,
                null,
                player.getCredits()
        );

        claimed.add(challengeIndex);
        return new ChallengeClaimResponse(
                challengeIndex,
                definition.reward(),
                economyService.toBalance(player),
                response(progress, player, claimed)
        );
    }

    private ChallengeListResponse response(
            ChallengeProgress progress,
            PlayerProgress player,
            Set<Integer> claimed
    ) {
        double power = currentPower(player);
        List<ChallengeItemResponse> items = visibleDefinitions(claimed).stream()
                .map(definition -> toItem(progress, definition, power))
                .toList();
        List<Integer> sortedClaimed = claimed.stream().sorted().toList();
        return new ChallengeListResponse(
                items,
                stats(progress, power),
                sortedClaimed,
                catalog.all().size()
        );
    }

    private List<ChallengeDefinition> visibleDefinitions(Set<Integer> claimed) {
        return catalog.all().stream()
                .filter(definition -> !claimed.contains(definition.index()))
                .limit(ChallengeCatalog.VISIBLE_CHALLENGES)
                .toList();
    }

    private ChallengeItemResponse toItem(
            ChallengeProgress progress,
            ChallengeDefinition definition,
            double power
    ) {
        double value = value(progress, definition.kind(), power);
        return new ChallengeItemResponse(
                definition.index(),
                definition.kind().apiValue(),
                definition.goal(),
                definition.reward(),
                Math.min(value, definition.goal()),
                value + EPSILON >= definition.goal()
        );
    }

    private ChallengeStatsResponse stats(ChallengeProgress progress, double power) {
        return new ChallengeStatsResponse(
                progress.getTotalClicks(),
                progress.getBlocksBroken(),
                progress.getCreditsEarned(),
                progress.getCreditsSpent(),
                progress.getPurchasesCount(),
                level(progress.getBlocksBroken()),
                power
        );
    }

    private boolean ready(
            ChallengeProgress progress,
            ChallengeDefinition definition,
            double power
    ) {
        return value(progress, definition.kind(), power) + EPSILON >= definition.goal();
    }

    private double value(
            ChallengeProgress progress,
            ChallengeKind kind,
            double power
    ) {
        return switch (kind) {
            case CLICKS -> progress.getTotalClicks();
            case BREAK -> progress.getBlocksBroken();
            case EARN -> progress.getCreditsEarned();
            case SPEND -> progress.getCreditsSpent();
            case LEVEL -> level(progress.getBlocksBroken());
            case BUY -> progress.getPurchasesCount();
            case POWER -> power;
        };
    }

    private long level(long blocksBroken) {
        return blocksBroken / BLOCKS_PER_LEVEL + 1;
    }

    private double currentPower(PlayerProgress progress) {
        double base = progress.getClickPower() == null ? 1.0 : progress.getClickPower();
        if (!Double.isFinite(base) || base <= 0) {
            throw new IllegalStateException("Invalid click power");
        }
        String toolCode = progress.getEquippedToolCode();
        if (toolCode == null || toolCode.isBlank()) {
            return base;
        }
        Tool tool = toolRepository.findById(toolCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Equipped tool does not exist: " + toolCode
                ));
        Double multiplier = tool.getPowerMultiplier();
        if (multiplier == null || !Double.isFinite(multiplier) || multiplier <= 0) {
            throw new IllegalStateException("Invalid tool power multiplier: " + toolCode);
        }
        return base * multiplier;
    }

    private Set<Integer> claimedIndexes(UUID userId) {
        Set<Integer> result = new HashSet<>();
        claimedChallengeRepository.findAllByUserIdOrderByChallengeIndex(userId)
                .forEach(claim -> result.add(claim.getChallengeIndex()));
        return result;
    }

    private ChallengeProgress getOrCreateForUpdate(UUID userId) {
        return challengeProgressRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> challengeProgressRepository.saveAndFlush(
                        emptyProgress(userId)
                ));
    }

    private ChallengeProgress emptyProgress(UUID userId) {
        ChallengeProgress progress = new ChallengeProgress();
        progress.setUserId(userId);
        return progress;
    }

    private long add(long current, long delta, String counter) {
        try {
            return Math.addExact(current, delta);
        } catch (ArithmeticException ex) {
            throw new IllegalStateException("Challenge counter overflow: " + counter, ex);
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
