package com.resourcemagnate.app.friend;

import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.friend.dto.FriendLeaderboardResponse;
import com.resourcemagnate.app.friend.dto.FriendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/invite")
    public ResponseEntity<FriendResponse> inviteFriend(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam("email") String email
    ) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "email is required");
        }
        FriendResponse response = friendService.inviteFriend(userId, email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{friendId}/accept")
    public ResponseEntity<FriendResponse> acceptInvitation(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("friendId") UUID friendId
    ) {
        FriendResponse response = friendService.acceptInvitation(userId, friendId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{friendId}/block")
    public ResponseEntity<Void> blockFriend(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("friendId") UUID friendId
    ) {
        friendService.blockFriend(userId, friendId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<FriendResponse>> getAcceptedFriends(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        List<FriendResponse> response = friendService.getAcceptedFriends(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<FriendResponse>> getPendingInvitations(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        List<FriendResponse> response = friendService.getPendingInvitations(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<FriendLeaderboardResponse> getFriendLeaderboard(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        FriendLeaderboardResponse response = friendService.getFriendLeaderboard(userId, userId);
        return ResponseEntity.ok(response);
    }
}
