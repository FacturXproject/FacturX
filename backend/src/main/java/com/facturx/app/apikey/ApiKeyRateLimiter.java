package com.facturx.app.apikey;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * F17 : limite de debit par cle API, fenetre fixe en memoire (par defaut 60 requetes
 * par minute). Meme choix que LoginAttemptService : une seule instance du backend, pas
 * besoin de Redis a cette echelle. Le compteur est atomique (ConcurrentHashMap.compute),
 * des requetes simultanees ne peuvent pas depasser la limite.
 */
@Component
public class ApiKeyRateLimiter {

    private final int limit;
    private final Duration window;
    private final Map<Long, Window> windowsByKey = new ConcurrentHashMap<>();

    public ApiKeyRateLimiter(
            @Value("${app.public-api.rate-limit.requests:60}") int limit,
            @Value("${app.public-api.rate-limit.window-seconds:60}") long windowSeconds) {

        this.limit = limit;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    /** Resultat d'une tentative : de quoi remplir les en-tetes X-RateLimit-* et Retry-After. */
    public record Decision(boolean allowed, int limit, int remaining, long resetInSeconds) {
    }

    public Decision tryConsume(Long apiKeyId) {
        Instant now = Instant.now();

        Window current = windowsByKey.compute(apiKeyId, (id, existing) -> {
            if (existing == null || isExpired(existing, now)) {
                return new Window(now, 1);
            }
            return new Window(existing.start(), existing.count() + 1);
        });

        long resetIn = Math.max(1, Duration.between(now, current.start().plus(window)).toSeconds());
        boolean allowed = current.count() <= limit;
        int remaining = Math.max(0, limit - current.count());
        return new Decision(allowed, limit, remaining, resetIn);
    }

    private boolean isExpired(Window w, Instant now) {
        return !now.isBefore(w.start().plus(window));
    }

    @Scheduled(fixedRate = 5, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    void cleanup() {
        Instant now = Instant.now();
        windowsByKey.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
    }

    private record Window(Instant start, int count) {
    }
}
