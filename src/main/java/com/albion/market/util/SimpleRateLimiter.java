package com.albion.market.util;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleRateLimiter {
    private final long nanosBetweenTokens;
    private final AtomicLong nextAllowedNanos = new AtomicLong(System.nanoTime());

    public SimpleRateLimiter(int permitsPerSecond) {
        this.nanosBetweenTokens = Duration.ofSeconds(1).toNanos() / Math.max(1, permitsPerSecond);
    }

    public void acquire() {
        while (true) {
            long now = System.nanoTime();
            long scheduled = nextAllowedNanos.get();
            long next = Math.max(now, scheduled) + nanosBetweenTokens;
            if (nextAllowedNanos.compareAndSet(scheduled, next)) {
                long waitNanos = Math.max(0, scheduled - now);
                if (waitNanos > 0) {
                    try {
                        long millis = waitNanos / 1_000_000;
                        int nanos = (int) (waitNanos % 1_000_000);
                        Thread.sleep(millis, nanos);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return;
            }
        }
    }

    public void penalize(Duration duration) {
        nextAllowedNanos.addAndGet(duration.toNanos());
    }
}
