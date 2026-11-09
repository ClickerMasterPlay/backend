package com.resourcemagnate.app.leaderboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {
    private List<LeaderboardEntry> top;
    private LeaderboardEntry me;
    private Long totalPlayers;
}