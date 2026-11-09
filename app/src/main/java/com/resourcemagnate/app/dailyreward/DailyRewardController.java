package com.resourcemagnate.app.dailyreward;

import com.resourcemagnate.app.dailyreward.dto.DailyRewardClaimResponse;
import com.resourcemagnate.app.dailyreward.dto.DailyRewardStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/daily-rewards")
@RequiredArgsConstructor
public class DailyRewardController {

    private final DailyRewardService dailyRewardService;

    @GetMapping("/status")
    public ResponseEntity<DailyRewardStatusResponse> status(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        return ResponseEntity.ok(dailyRewardService.status(userId));
    }

    @PostMapping("/claim")
    public ResponseEntity<DailyRewardClaimResponse> claim(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey
    ) {
        return ResponseEntity.ok(dailyRewardService.claim(userId, idempotencyKey));
    }
}
