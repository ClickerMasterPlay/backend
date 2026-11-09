package com.resourcemagnate.app.leaderboard;

import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.leaderboard.dto.LeaderboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<LeaderboardResponse> getLeaderboard(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestParam(value = "top", defaultValue = "100") int topLimit
    ) {
        if (userId == null) {
            throw new BusinessException("VALIDATION_ERROR", "X-User-Id header is required");
        }
        
        LeaderboardResponse response = leaderboardService.getLeaderboard(userId, topLimit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshLeaderboard(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        if (userId == null) {
            throw new BusinessException("VALIDATION_ERROR", "X-User-Id header is required");
        }
        
        leaderboardService.updatePlayerScore(userId);
        return ResponseEntity.ok().build();
    }
}