package com.resourcemagnate.app.common.exception;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ApiError(
        @JsonProperty("error_code")
        String errorCode,
        String message,
        Instant timestamp
) {
    public static ApiError of(String errorCode, String message) {
        return new ApiError(errorCode, message, Instant.now());
    }
}
