package com.facturx.app.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Simple in-memory throttle: 5 failed logins for the same (normalized) email
 * within 15 minutes blocks that email for the rest of the window.
 * Deliberately not backed by Redis/DB - not needed at this scale (see F01 brief §5).
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, AttemptRecord> attemptsByEmail = new ConcurrentHashMap<>();

    public boolean isBlocked(String normalizedEmail) {
        AttemptRecord record = attemptsByEmail.get(normalizedEmail);
        return record != null && record.count() >= MAX_ATTEMPTS && !isExpired(record);
    }

    public void recordFailure(String normalizedEmail) {
        attemptsByEmail.compute(normalizedEmail, (email, existing) -> {
            if (existing == null || isExpired(existing)) {
                return new AttemptRecord(1, Instant.now());
            }
            return new AttemptRecord(existing.count() + 1, existing.windowStart());
        });
    }

    public void recordSuccess(String normalizedEmail) {
        attemptsByEmail.remove(normalizedEmail);
    }

    private boolean isExpired(AttemptRecord record) {
        return Instant.now().isAfter(record.windowStart().plus(WINDOW));
    }

    @Scheduled(fixedRate = 15, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    void cleanup() {
        attemptsByEmail.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }

    private record AttemptRecord(int count, Instant windowStart) {
    }
}
