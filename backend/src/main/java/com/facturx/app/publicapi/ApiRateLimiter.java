package com.facturx.app.publicapi;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Per-key request throttle: a fixed one-minute window, counted in memory.
 * Same deliberate choice as LoginAttemptService - no Redis/DB at this scale. The
 * consequence to know for the defence: counters reset when the backend restarts.
 */
@Component
public class ApiRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final int requestsPerMinute;
    private final Map<Long, Window> windowsByKeyId = new ConcurrentHashMap<>();

    public ApiRateLimiter(@Value("${app.public-api.rate-limit.requests-per-minute:60}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    /** Counts one request for this key and says whether it may go through. */
    public Decision consume(Long keyId) {
        Instant now = Instant.now();

        // compute() is atomic per key: two concurrent requests cannot both read the
        // same count and both slip under the limit.
        Window window = windowsByKeyId.compute(keyId, (id, existing) -> {
            if (existing == null || isExpired(existing, now)) {
                return new Window(1, now);
            }
            return new Window(existing.count() + 1, existing.start());
        });

        long secondsUntilReset = Math.max(1, Duration.between(now, window.start().plus(WINDOW)).toSeconds());

        return new Decision(
                window.count() <= requestsPerMinute,
                requestsPerMinute,
                Math.max(0, requestsPerMinute - window.count()),
                secondsUntilReset);
    }

    private boolean isExpired(Window window, Instant now) {
        return now.isAfter(window.start().plus(WINDOW));
    }

    @Scheduled(fixedRate = 5, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    void cleanup() {
        Instant now = Instant.now();
        windowsByKeyId.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
    }

    private record Window(int count, Instant start) {
    }

    public record Decision(boolean allowed, int limit, int remaining, long secondsUntilReset) {
    }
}
