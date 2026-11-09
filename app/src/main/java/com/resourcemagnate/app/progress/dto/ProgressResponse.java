package com.resourcemagnate.app.progress.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {
    private UUID userId;
    @JsonProperty("isGuest")
    private boolean isGuest;
    private String displayName;
    private Long credits;
    private Double clickPower;
    private Map<String, Object> resources;
    private String equippedToolCode;
    private Instant lastSeenAt;

    @JsonProperty("isGuest")
    public boolean isGuest() {
        return isGuest;
    }
}
