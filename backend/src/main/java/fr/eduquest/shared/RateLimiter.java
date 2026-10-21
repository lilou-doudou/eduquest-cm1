package fr.eduquest.shared;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Limitation de débit persistée dans PostgreSQL (fenêtre fixe), partagée entre instances.
 * Aucun blocage définitif : la fenêtre expire d'elle-même.
 */
@Service
public class RateLimiter {
    private final JdbcClient jdbc;
    private final Clock clock;

    public RateLimiter(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void consume(String key, int limit, Duration window) {
        Instant now = clock.instant();
        long windowSeconds = window.toSeconds();
        long startEpoch = (now.getEpochSecond() / windowSeconds) * windowSeconds;
        OffsetDateTime start = OffsetDateTime.ofInstant(Instant.ofEpochSecond(startEpoch), ZoneOffset.UTC);
        Integer hits = jdbc.sql("""
                INSERT INTO rate_limit_bucket (bucket_key, window_start, hits) VALUES (:k, :s, 1)
                ON CONFLICT (bucket_key, window_start) DO UPDATE SET hits = rate_limit_bucket.hits + 1
                RETURNING hits""")
                .param("k", key.length() > 200 ? key.substring(0, 200) : key)
                .param("s", start)
                .query(Integer.class).single();
        if (hits > limit) {
            long retryAfter = Math.max(1, startEpoch + windowSeconds - now.getEpochSecond());
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "Trop de tentatives. Réessaie un peu plus tard.", retryAfter);
        }
    }
}
