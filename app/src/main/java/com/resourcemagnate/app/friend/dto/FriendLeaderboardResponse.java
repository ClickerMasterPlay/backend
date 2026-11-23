package com.resourcemagnate.app.friend.dto;

import com.resourcemagnate.app.leaderboard.dto.LeaderboardEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendLeaderboardResponse {
    private List<LeaderboardEntry> top;
    private LeaderboardEntry me;
    private Long totalPlayers;
    private Long friendCount;
}