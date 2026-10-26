package com.resourcemagnate.app.progress;

import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.progress.dto.ProgressResponse;
import com.resourcemagnate.app.user.User;
import com.resourcemagnate.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UserRepository userRepository;
    private final PlayerProgressRepository progressRepository;

    @Transactional(readOnly = true)
    public ProgressResponse getProgress(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        PlayerProgress progress = progressRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("PROGRESS_NOT_FOUND", "Progress not found"));

        return ProgressResponse.builder()
                .userId(user.getId())
                .isGuest(user.isGuest())
                .displayName(user.getDisplayName())
                .credits(progress.getCredits())
                .clickPower(progress.getClickPower())
                .resources(progress.getResources())
                .equippedToolCode(progress.getEquippedToolCode())
                .lastSeenAt(progress.getLastSeenAt())
                .build();
    }
}