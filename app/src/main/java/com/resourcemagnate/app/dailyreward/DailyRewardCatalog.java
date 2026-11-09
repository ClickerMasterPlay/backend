package com.resourcemagnate.app.dailyreward;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DailyRewardCatalog {

    public static final String STONE = "stone";
    public static final String DIAMOND = "diamond";

    private static final Map<Integer, Map<String, Long>> REWARDS = Map.of(
            1, Map.of(STONE, 10L),
            2, Map.of(DIAMOND, 1L),
            3, Map.of(STONE, 15L, DIAMOND, 1L),
            4, Map.of(STONE, 20L),
            5, Map.of(DIAMOND, 2L),
            6, Map.of(STONE, 25L, DIAMOND, 2L),
            7, Map.of(STONE, 50L, DIAMOND, 5L)
    );

    public Map<String, Long> rewardFor(int cycleDay) {
        Map<String, Long> reward = REWARDS.get(cycleDay);
        if (reward == null) {
            throw new IllegalStateException("Invalid daily reward cycle day: " + cycleDay);
        }
        return reward;
    }

    public int nextDay(int cycleDay) {
        return cycleDay == 7 ? 1 : cycleDay + 1;
    }
}
