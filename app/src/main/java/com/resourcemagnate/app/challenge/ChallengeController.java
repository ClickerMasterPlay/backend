package com.resourcemagnate.app.challenge;

import com.resourcemagnate.app.challenge.dto.ChallengeClaimResponse;
import com.resourcemagnate.app.challenge.dto.ChallengeListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @GetMapping
    public ResponseEntity<ChallengeListResponse> getChallenges(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        return ResponseEntity.ok(challengeService.getChallenges(userId));
    }

    @PostMapping("/{challengeIndex}/claim")
    public ResponseEntity<ChallengeClaimResponse> claim(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable int challengeIndex
    ) {
        return ResponseEntity.ok(challengeService.claim(userId, challengeIndex));
    }
}
