package com.albion.market.util;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sliding-window limiter for multiple windows.
 * Enforces request quotas such as 180/min and 300/5min.
 */
public class SimpleRateLimiter {
    private final int maxPerMinute;
    private final int maxPerFiveMinutes;
    private final Deque<Instant> oneMinuteWindow = new ArrayDeque<>();
    private final Deque<Instant> fiveMinuteWindow = new ArrayDeque<>();
    private Instant blockedUntil = Instant.EPOCH;

    public SimpleRateLimiter(int maxPerMinute, int maxPerFiveMinutes) {
        this.maxPerMinute = Math.max(1, maxPerMinute);
        this.maxPerFiveMinutes = Math.max(1, maxPerFiveMinutes);
    }

    public void acquire() {
        while (true) {
            Duration wait;
            synchronized (this) {
                Instant now = Instant.now();
                trim(now);

                Duration blockedWait = Duration.between(now, blockedUntil);
                if (!blockedWait.isNegative() && !blockedWait.isZero()) {
                    wait = blockedWait;
                } else {
                    boolean minuteExceeded = oneMinuteWindow.size() >= maxPerMinute;
                    boolean fiveMinutesExceeded = fiveMinuteWindow.size() >= maxPerFiveMinutes;

                    if (!minuteExceeded && !fiveMinutesExceeded) {
                        oneMinuteWindow.addLast(now);
                        fiveMinuteWindow.addLast(now);
                        return;
                    }

                    wait = computeWindowWait(now, minuteExceeded, fiveMinutesExceeded);
                }
            }

            try {
                Thread.sleep(Math.max(1, wait.toMillis()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public synchronized void penalize(Duration duration) {
        Instant candidate = Instant.now().plus(duration);
        if (candidate.isAfter(blockedUntil)) {
            blockedUntil = candidate;
        }
    }

    private void trim(Instant now) {
        Instant oneMinuteAgo = now.minusSeconds(60);
        while (!oneMinuteWindow.isEmpty() && oneMinuteWindow.peekFirst().isBefore(oneMinuteAgo)) {
            oneMinuteWindow.removeFirst();
        }

        Instant fiveMinutesAgo = now.minusSeconds(300);
        while (!fiveMinuteWindow.isEmpty() && fiveMinuteWindow.peekFirst().isBefore(fiveMinutesAgo)) {
            fiveMinuteWindow.removeFirst();
        }
    }

    private Duration computeWindowWait(Instant now, boolean minuteExceeded, boolean fiveMinutesExceeded) {
        Duration maxWait = Duration.ofMillis(1);

        if (minuteExceeded && !oneMinuteWindow.isEmpty()) {
            Instant oldest = oneMinuteWindow.peekFirst();
            Duration candidate = Duration.between(now, oldest.plusSeconds(60));
            if (candidate.compareTo(maxWait) > 0) {
                maxWait = candidate;
            }
        }

        if (fiveMinutesExceeded && !fiveMinuteWindow.isEmpty()) {
            Instant oldest = fiveMinuteWindow.peekFirst();
            Duration candidate = Duration.between(now, oldest.plusSeconds(300));
            if (candidate.compareTo(maxWait) > 0) {
                maxWait = candidate;
            }
        }

        return maxWait;
    }
}
