package com.pahappa.sacco.security;

import javax.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

 //Tracks failed login attempts per username and imposes a temporary
 //lockout after too many failures
@ApplicationScoped
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private static class AttemptRecord {
        int failureCount = 0;
        Instant lockedUntil = null;
    }

    private final Map<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String username) {
        AttemptRecord record = attempts.get(normalizedKey(username));
        if (record == null || record.lockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(record.lockedUntil)) {
            // Lockout window has expired — reset and allow another attempt.
            attempts.remove(normalizedKey(username));
            return false;
        }
        return true;
    }

    public void recordFailure(String username) {
        AttemptRecord record = attempts.computeIfAbsent(normalizedKey(username), k -> new AttemptRecord());
        record.failureCount++;
        if (record.failureCount >= MAX_ATTEMPTS) {
            record.lockedUntil = Instant.now().plus(LOCKOUT_DURATION);
        }
    }

    public void recordSuccess(String username) {
        attempts.remove(normalizedKey(username));
    }

    private String normalizedKey(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
