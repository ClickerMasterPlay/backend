package com.resourcemagnate.app.auth;

import com.resourcemagnate.app.auth.dto.AuthResponse;
import com.resourcemagnate.app.auth.dto.RegisterRequest;
import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.progress.PlayerProgress;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import com.resourcemagnate.app.user.User;
import com.resourcemagnate.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PlayerProgressRepository progressRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse createGuest() {
        UUID userId = UUID.randomUUID();
        
        User user = User.builder()
                .id(userId)
                .guest(true)
                .build();
        userRepository.save(user);

        PlayerProgress progress = PlayerProgress.builder()
                .userId(userId)
                .credits(0L)
                .clickPower(1.0)
                .resources(Map.of())
                .version(0L)
                .build();
        progressRepository.save(progress);

        return AuthResponse.builder()
                .userId(userId)
                .isGuest(true)
                .displayName(null)
                .credits(0L)
                .clickPower(1.0)
                .build();
    }

    @Transactional
    public AuthResponse register(UUID guestUserId, RegisterRequest request) {
        User user = userRepository.findById(guestUserId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        if (!user.isGuest()) {
            throw new BusinessException("USER_NOT_GUEST", "User is not a guest");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email already registered");
        }

        user.setGuest(false);
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        userRepository.save(user);

        PlayerProgress progress = progressRepository.findByUserId(guestUserId)
                .orElseThrow(() -> new BusinessException("PROGRESS_NOT_FOUND", "Progress not found"));

        return AuthResponse.builder()
                .userId(user.getId())
                .isGuest(false)
                .displayName(user.getDisplayName())
                .credits(progress.getCredits())
                .clickPower(progress.getClickPower())
                .build();
    }

    @Transactional
    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Invalid email or password"));

        if (user.isGuest()) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        user.setLastLoginAt(java.time.Instant.now());
        userRepository.save(user);

        PlayerProgress progress = progressRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("PROGRESS_NOT_FOUND", "Progress not found"));

        return AuthResponse.builder()
                .userId(user.getId())
                .isGuest(false)
                .displayName(user.getDisplayName())
                .credits(progress.getCredits())
                .clickPower(progress.getClickPower())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse getProgress(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        PlayerProgress progress = progressRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("PROGRESS_NOT_FOUND", "Progress not found"));

        return AuthResponse.builder()
                .userId(user.getId())
                .isGuest(user.isGuest())
                .displayName(user.getDisplayName())
                .credits(progress.getCredits())
                .clickPower(progress.getClickPower())
                .build();
    }
}