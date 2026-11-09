package com.resourcemagnate.app.game;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class ClickRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ClickRateLimiter.class);
    private static final Duration WINDOW = Duration.ofSeconds(2);
    private static final long WARNING_INTERVAL_NANOS = Duration.ofMinutes(1).toNanos();

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<UUID, Long> localWindows = new ConcurrentHashMap<>();
    private final AtomicLong lastWarningAt = new AtomicLong(Long.MIN_VALUE);

    public boolean allow(UUID userId) {
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    "click_rate:" + userId,
                    "1",
                    WINDOW
            );
            if (acquired != null) {
                return acquired;
            }
        } catch (DataAccessException ex) {
            warnRedisUnavailable(ex);
        }
        return allowLocally(userId);
    }

    private boolean allowLocally(UUID userId) {
        long now = System.nanoTime();
        AtomicBoolean allowed = new AtomicBoolean(false);
        localWindows.compute(userId, (id, blockedUntil) -> {
            if (blockedUntil == null || blockedUntil <= now) {
                allowed.set(true);
                return now + WINDOW.toNanos();
            }
            return blockedUntil;
        });
        if (localWindows.size() > 10_000) {
            localWindows.entrySet().removeIf(entry -> entry.getValue() <= now);
        }
        return allowed.get();
    }

    private void warnRedisUnavailable(DataAccessException ex) {
        long now = System.nanoTime();
        long previous = lastWarningAt.get();
        if ((previous == Long.MIN_VALUE || now - previous >= WARNING_INTERVAL_NANOS)
                && lastWarningAt.compareAndSet(previous, now)) {
            log.warn("Redis rate limiter is unavailable; using local fallback", ex);
        }
    }
}
