package com.resourcemagnate.app.leaderboard;

import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.catalog.ProductCatalogService;
import com.resourcemagnate.app.economy.model.ProductType;
import com.resourcemagnate.app.economy.catalog.ProductDefinition;
import com.resourcemagnate.app.leaderboard.dto.LeaderboardEntry;
import com.resourcemagnate.app.leaderboard.dto.LeaderboardResponse;
import com.resourcemagnate.app.progress.PlayerProgress;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import com.resourcemagnate.app.user.User;
import com.resourcemagnate.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final PlayerProgressRepository progressRepository;
    private final CurrentLeaderboardRepository currentLeaderboardRepository;
    private final LeaderboardSnapshotRepository snapshotRepository;
    private final ScoreCalculator scoreCalculator;
    private final ProductCatalogService catalogService;

    private static final String REDIS_KEY = "leaderboard:scores";
    private static final int DEFAULT_TOP_LIMIT = 100;

    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(UUID userId, int topLimit) {
        if (topLimit <= 0 || topLimit > 1000) {
            topLimit = DEFAULT_TOP_LIMIT;
        }

        List<LeaderboardEntry> top = getTopFromRedis(topLimit);
        LeaderboardEntry me = getMyEntry(userId);
        long totalPlayers = getTotalPlayersFromRedis();

        return LeaderboardResponse.builder()
                .top(top)
                .me(me)
                .totalPlayers(totalPlayers)
                .build();
    }

    private List<LeaderboardEntry> getTopFromRedis(int limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate
                .opsForZSet()
                .reverseRangeWithScores(REDIS_KEY, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            return refreshAndGetTop(limit);
        }

        List<String> userIds = tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .toList();

        Map<UUID, User> users = userRepository.findAllById(
                userIds.stream().map(UUID::fromString).toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        Map<UUID, PlayerProgress> progressMap = progressRepository
                .findAllById(userIds.stream().map(UUID::fromString).toList())
                .stream().collect(Collectors.toMap(PlayerProgress::getUserId, p -> p));

        List<LeaderboardEntry> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            UUID uid = UUID.fromString(tuple.getValue());
            User user = users.get(uid);
            PlayerProgress progress = progressMap.get(uid);

            if (user != null && progress != null) {
                ScoreCalculator.ScoreBreakdown breakdown = scoreCalculator.calculate(
                        progress, progress.getEquippedToolCode()
                );
                
                result.add(LeaderboardEntry.builder()
                        .rank(rank++)
                        .userId(uid)
                        .displayName(user.getDisplayName())
                        .score(breakdown.score())
                        .credits(breakdown.credits())
                        .totalResourcesValue(breakdown.totalResourcesValue())
                        .clickPower(breakdown.clickPower())
                        .build());
            }
        }
        return result;
    }

    private LeaderboardEntry getMyEntry(UUID userId) {
        Long rank = redisTemplate.opsForZSet().reverseRank(REDIS_KEY, userId.toString());
        Double score = redisTemplate.opsForZSet().score(REDIS_KEY, userId.toString());

        if (rank == null || score == null) {
            return refreshAndGetMyEntry(userId);
        }

        User user = userRepository.findById(userId).orElse(null);
        PlayerProgress progress = progressRepository.findByUserId(userId).orElse(null);

        if (user == null || progress == null) {
            return null;
        }

        ScoreCalculator.ScoreBreakdown breakdown = scoreCalculator.calculate(
                progress, progress.getEquippedToolCode()
        );

        return LeaderboardEntry.builder()
                .rank(rank.intValue() + 1)
                .userId(userId)
                .displayName(user.getDisplayName())
                .score(breakdown.score())
                .credits(breakdown.credits())
                .totalResourcesValue(breakdown.totalResourcesValue())
                .clickPower(breakdown.clickPower())
                .build();
    }

    private long getTotalPlayersFromRedis() {
        Long count = redisTemplate.opsForZSet().zCard(REDIS_KEY);
        return count != null ? count : 0;
    }

    @Transactional
    public void updatePlayerScore(UUID userId) {
        PlayerProgress progress = progressRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("PROGRESS_NOT_FOUND", "Player progress not found"));
        
        ScoreCalculator.ScoreBreakdown breakdown = scoreCalculator.calculate(
                progress, progress.getEquippedToolCode()
        );

        redisTemplate.opsForZSet().add(REDIS_KEY, userId.toString(), breakdown.score());

        CurrentLeaderboard current = currentLeaderboardRepository.findByUserId(userId)
                .orElse(new CurrentLeaderboard());
        
        current.setUserId(userId);
        current.setScore(breakdown.score());
        current.setCredits(breakdown.credits());
        current.setTotalResourcesValue(breakdown.totalResourcesValue());
        current.setClickPower(breakdown.clickPower());
        current.setUpdatedAt(Instant.now());
        currentLeaderboardRepository.save(current);
    }

    @Transactional
    @Scheduled(fixedRate = 300000)
    public void refreshFullLeaderboard() {
        log.info("Starting full leaderboard refresh");
        
        List<UUID> allUserIds = userRepository.findAll().stream()
                .map(User::getId)
                .toList();

        if (allUserIds.isEmpty()) {
            return;
        }

        Map<UUID, PlayerProgress> progressMap = progressRepository
                .findAllById(allUserIds)
                .stream().collect(Collectors.toMap(PlayerProgress::getUserId, p -> p));

        List<ScoredUser> scoredUsers = new ArrayList<>();
        for (Map.Entry<UUID, PlayerProgress> entry : progressMap.entrySet()) {
            ScoreCalculator.ScoreBreakdown breakdown = scoreCalculator.calculate(
                    entry.getValue(), entry.getValue().getEquippedToolCode()
            );
            scoredUsers.add(new ScoredUser(entry.getKey(), breakdown.score(), breakdown));
        }

        scoredUsers.sort((a, b) -> Long.compare(b.score(), a.score()));

        redisTemplate.delete(REDIS_KEY);
        
        List<CurrentLeaderboard> currentLeaderboards = new ArrayList<>();
        for (int i = 0; i < scoredUsers.size(); i++) {
            ScoredUser su = scoredUsers.get(i);
            redisTemplate.opsForZSet().add(REDIS_KEY, su.userId().toString(), su.score());

            CurrentLeaderboard cl = new CurrentLeaderboard();
            cl.setUserId(su.userId());
            cl.setScore(su.score());
            cl.setRank(i + 1);
            cl.setCredits(su.breakdown().credits());
            cl.setTotalResourcesValue(su.breakdown().totalResourcesValue());
            cl.setClickPower(su.breakdown().clickPower());
            cl.setUpdatedAt(Instant.now());
            currentLeaderboards.add(cl);
        }
        
        currentLeaderboardRepository.saveAll(currentLeaderboards);
        
        log.info("Leaderboard refreshed for {} players", scoredUsers.size());
    }

    private List<LeaderboardEntry> refreshAndGetTop(int limit) {
        refreshFullLeaderboard();
        return getTopFromRedis(limit);
    }

    private LeaderboardEntry refreshAndGetMyEntry(UUID userId) {
        refreshFullLeaderboard();
        return getMyEntry(userId);
    }

    private record ScoredUser(UUID userId, long score, ScoreCalculator.ScoreBreakdown breakdown) {}
}