package com.resourcemagnate.app.economy;

import java.time.Duration;
import java.time.Instant;

public final class MachineProductionCalculator {

    private static final long NANOS_PER_HOUR = Duration.ofHours(1).toNanos();
    public static final Duration MAX_OFFLINE_WINDOW = Duration.ofHours(24);

    private MachineProductionCalculator() {
    }

    public static Accrual calculate(
            Instant lastCollectedAt,
            Instant now,
            long productionPerHour
    ) {
        if (lastCollectedAt == null || now == null) {
            throw new IllegalArgumentException("Collection timestamps are required");
        }
        if (productionPerHour <= 0 || productionPerHour > NANOS_PER_HOUR) {
            throw new IllegalArgumentException("Invalid machine production rate");
        }
        if (!now.isAfter(lastCollectedAt)) {
            return new Accrual(0, lastCollectedAt);
        }

        Instant windowStart = now.minus(MAX_OFFLINE_WINDOW);
        Instant effectiveStart = lastCollectedAt.isBefore(windowStart)
                ? windowStart
                : lastCollectedAt;
        long nanosPerUnit = NANOS_PER_HOUR / productionPerHour;
        long elapsedNanos;
        try {
            elapsedNanos = Duration.between(effectiveStart, now).toNanos();
        } catch (ArithmeticException ex) {
            throw new IllegalStateException("Machine collection interval is too large", ex);
        }

        long amount = elapsedNanos / nanosPerUnit;
        Instant collectedThrough = effectiveStart.plusNanos(
                Math.multiplyExact(amount, nanosPerUnit)
        );
        return new Accrual(amount, collectedThrough);
    }

    public record Accrual(long amount, Instant collectedThrough) {
    }
}
