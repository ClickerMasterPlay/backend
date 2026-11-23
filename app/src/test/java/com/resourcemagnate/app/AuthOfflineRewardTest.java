package com.resourcemagnate.app;

import com.resourcemagnate.app.auth.AuthService;
import com.resourcemagnate.app.auth.dto.AuthResponse;
import com.resourcemagnate.app.economy.MachineCollectionService;
import com.resourcemagnate.app.economy.dto.BalanceResponse;
import com.resourcemagnate.app.economy.dto.MachineCollectionResponse;
import com.resourcemagnate.app.progress.PlayerProgress;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import com.resourcemagnate.app.user.User;
import com.resourcemagnate.app.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthOfflineRewardTest {

    @Mock private UserRepository userRepository;
    @Mock private PlayerProgressRepository progressRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MachineCollectionService machineCollectionService;

    @Test
    void login_shouldCollectOfflineProductionAndReturnIt() {
        Instant now = Instant.parse("2026-11-08T10:00:00Z");
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setGuest(false);
        user.setEmail("player@example.com");
        user.setPasswordHash("hash");

        PlayerProgress progress = new PlayerProgress();
        progress.setUserId(userId);
        progress.setCredits(500L);
        progress.setClickPower(1.5);
        progress.setResources(Map.of());

        when(userRepository.findByEmail("player@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(progressRepository.findByUserId(userId))
                .thenReturn(Optional.of(progress));
        when(machineCollectionService.collect(userId)).thenReturn(
                new MachineCollectionResponse(
                        240L,
                        new BalanceResponse(740L, Map.of()),
                        now
                )
        );

        AuthService authService = new AuthService(
                userRepository,
                progressRepository,
                passwordEncoder,
                machineCollectionService,
                Clock.fixed(now, ZoneOffset.UTC)
        );

        AuthResponse response = authService.login(
                "player@example.com", "password"
        );

        assertEquals(240L, response.getOfflineCredits());
        assertEquals(740L, response.getCredits());
        assertEquals(now, user.getLastLoginAt());
        assertEquals(now, progress.getLastSeenAt());
        verify(machineCollectionService).collect(userId);
    }

    @Test
    void login_shouldCollectOnceAndNotWriteRewardWhenNothingAccrued() {
        Instant now = Instant.parse("2026-11-08T10:00:00Z");
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setGuest(false);
        user.setEmail("idle@example.com");
        user.setPasswordHash("hash");

        PlayerProgress progress = new PlayerProgress();
        progress.setUserId(userId);
        progress.setCredits(500L);
        progress.setClickPower(1.5);
        progress.setResources(Map.of());

        when(userRepository.findByEmail("idle@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(progressRepository.findByUserId(userId))
                .thenReturn(Optional.of(progress));
        when(machineCollectionService.collect(userId)).thenReturn(
                new MachineCollectionResponse(
                        0L,
                        new BalanceResponse(500L, Map.of()),
                        now
                )
        );

        AuthService authService = new AuthService(
                userRepository,
                progressRepository,
                passwordEncoder,
                machineCollectionService,
                Clock.fixed(now, ZoneOffset.UTC)
        );

        AuthResponse response = authService.login(
                "idle@example.com", "password"
        );

        assertEquals(0L, response.getOfflineCredits());
        assertEquals(500L, response.getCredits());
        verify(machineCollectionService).collect(userId);
    }
}
