package com.resourcemagnate.app.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UUID userId;
    private boolean isGuest;
    private String displayName;
    private Long credits;
    private Double clickPower;
    private Map<String, Long> offlineRewards;
}
