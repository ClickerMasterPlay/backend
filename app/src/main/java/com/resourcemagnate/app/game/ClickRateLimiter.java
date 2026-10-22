package com.resourcemagnate.app.game;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClickRateLimiter {

    private static final Duration WINDOW = Duration.ofSeconds(2);

    private final StringRedisTemplate redisTemplate;

    public boolean allow(UUID userId) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                "click_rate:" + userId,
                "1",
                WINDOW
        );

        return Boolean.TRUE.equals(acquired);
    }
}
