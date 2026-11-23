package com.resourcemagnate.app.friend;

import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.friend.dto.FriendLeaderboardResponse;
import com.resourcemagnate.app.friend.dto.FriendResponse;
import com.resourcemagnate.app.leaderboard.LeaderboardService;
import com.resourcemagnate.app.leaderboard.dto.LeaderboardEntry;
import com.resourcemagnate.app.leaderboard.dto.LeaderboardResponse;
import com.resourcemagnate.app.progress.PlayerProgress;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import com.resourcemagnate.app.user.User;
import com.resourcemagnate.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final PlayerProgressRepository progressRepository;
    private final LeaderboardService leaderboardService;

    @Transactional
    public FriendResponse inviteFriend(UUID inviterId, String email) {
        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User invitee = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (invitee.isGuest()) {
            throw new RuntimeException("Cannot invite guest user");
        }

        if (inviterId.equals(invitee.getId())) {
            throw new RuntimeException("Cannot invite yourself");
        }

        boolean exists = friendRepository.existsByUserIdAndFriendId(inviterId, invitee.getId())
                || friendRepository.existsByFriendIdAndUserId(inviterId, invitee.getId());

        if (exists) {
            throw new RuntimeException("Friend relationship already exists");
        }

        Friend friend = Friend.builder()
                .id(UUID.randomUUID())
                .userId(inviterId)
                .friendId(invitee.getId())
                .status(FriendStatus.PENDING)
                .invitedBy(inviterId)
                .build();

        Friend saved = friendRepository.save(friend);

        return toFriendResponse(saved, invitee);
    }

    @Transactional
    public FriendResponse acceptInvitation(UUID inviteeId, UUID friendId) {
        Friend friend = friendRepository.findByFriendIdAndUserId(inviteeId, friendId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (friend.getStatus() != FriendStatus.PENDING) {
            throw new RuntimeException("Invitation is not pending");
        }

        friend.setStatus(FriendStatus.ACCEPTED);
        Friend saved = friendRepository.save(friend);

        User otherUser = userRepository.findById(friendId).orElse(null);
        return toFriendResponse(saved, otherUser);
    }

    @Transactional
    public void blockFriend(UUID userId, UUID friendId) {
        Friend friend = friendRepository.findByUserIdAndFriendId(userId, friendId)
                .orElseGet(() -> {
                    Friend f = Friend.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .friendId(friendId)
                            .status(FriendStatus.BLOCKED)
                            .build();
                    return friendRepository.save(f);
                });

        friend.setStatus(FriendStatus.BLOCKED);
        friendRepository.save(friend);
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getAcceptedFriends(UUID userId) {
        List<Friend> friends = friendRepository.findByUserIdAndStatus(userId, FriendStatus.ACCEPTED);
        List<Friend> friends2 = friendRepository.findByFriendIdAndStatus(userId, FriendStatus.ACCEPTED);

        Set<UUID> friendUserIds = new HashSet<>();
        for (Friend f : friends) friendUserIds.add(f.getFriendId());
        for (Friend f : friends2) friendUserIds.add(f.getFriendId());

        Map<UUID, User> userMap = userRepository.findAllById(friendUserIds.stream().toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        List<FriendResponse> result = new ArrayList<>();
        for (Friend f : friends) {
            User u = userMap.get(f.getFriendId());
            if (u != null) {
                result.add(toFriendResponse(f, u));
            }
        }
        for (Friend f : friends2) {
            User u = userMap.get(f.getFriendId());
            if (u != null) {
                result.add(toFriendResponse(f, u));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getPendingInvitations(UUID userId) {
        List<Friend> pending = friendRepository.findByFriendIdAndStatus(userId, FriendStatus.PENDING);
        Map<UUID, User> userMap = userRepository.findAllById(
                pending.stream().map(Friend::getUserId).toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        List<FriendResponse> result = new ArrayList<>();
        for (Friend f : pending) {
            User u = userMap.get(f.getFriendId());
            if (u != null) {
                result.add(toFriendResponse(f, u));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public FriendLeaderboardResponse getFriendLeaderboard(UUID userId, UUID friendId) {
        List<FriendResponse> friends = getAcceptedFriends(userId);
        Set<UUID> friendUserIds = new HashSet<>();
        friendUserIds.add(userId);
        for (FriendResponse fr : friends) {
            friendUserIds.add(fr.getFriendId());
        }

        LeaderboardResponse global = leaderboardService.getLeaderboard(userId, 1000);

        List<LeaderboardEntry> filtered = global.getTop().stream()
                .filter(e -> friendUserIds.contains(e.getUserId()))
                .toList();

        int rank = 1;
        for (LeaderboardEntry e : filtered) {
            e.setRank(rank++);
        }

        return FriendLeaderboardResponse.builder()
                .top(filtered)
                .me(global.getMe())
                .totalPlayers((long) filtered.size())
                .friendCount((long) friends.size())
                .build();
    }

    private FriendResponse toFriendResponse(Friend friend, User otherUser) {
        Long score = null;
        Integer rank = null;
        if (otherUser != null) {
            PlayerProgress progress = progressRepository.findByUserId(otherUser.getId()).orElse(null);
            if (progress != null) {
                score = progress.getCredits();
            }
        }

        return FriendResponse.builder()
                .id(friend.getId())
                .userId(friend.getFriendId())
                .friendId(friend.getFriendId())
                .status(friend.getStatus().name())
                .displayName(otherUser != null ? otherUser.getDisplayName() : null)
                .email(otherUser != null ? otherUser.getEmail() : null)
                .score(score)
                .rank(rank)
                .build();
    }
}