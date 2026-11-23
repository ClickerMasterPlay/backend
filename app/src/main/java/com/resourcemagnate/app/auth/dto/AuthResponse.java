package com.resourcemagnate.app.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UUID userId;
    @JsonProperty("isGuest")
    private boolean isGuest;
    private String displayName;
    private Long credits;
    private Double clickPower;
    private Long offlineCredits;

    @JsonProperty("isGuest")
    public boolean isGuest() {
        return isGuest;
    }
}
