package com.resourcemagnate.app.leaderboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {
    private Integer rank;
    private UUID userId;
    private String displayName;
    private Long score;
    private Long credits;
    private Long totalResourcesValue;
    private Double clickPower;
}