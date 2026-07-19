package com.talan.creditplatform.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
    private static final int MAX_ATTEMPTS = 3;
    private static final int WINDOW_MINUTES = 15;

    // Stores timestamps of recovery requests for each key (IP address or email)
    private final Map<String, List<LocalDateTime>> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        cleanUp(key);
        List<LocalDateTime> times = attempts.get(key);
        return times != null && times.size() >= MAX_ATTEMPTS;
    }

    public void recordAttempt(String key) {
        cleanUp(key);
        attempts.computeIfAbsent(key, k -> new ArrayList<>()).add(LocalDateTime.now());
    }

    private void cleanUp(String key) {
        List<LocalDateTime> times = attempts.get(key);
        if (times == null) return;
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(WINDOW_MINUTES);
        times.removeIf(time -> time.isBefore(cutoff));
        if (times.isEmpty()) {
            attempts.remove(key);
        }
    }
}
