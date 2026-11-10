package com.mobila.shop;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Limitare simplă în memorie (fereastră glisantă). Suficient pentru o singură instanță de backend. */
final class RateLimiter {
    private RateLimiter() {}
    private static final Map<String, ArrayDeque<Long>> HITS = new HashMap<>();

    private static ArrayDeque<Long> window(String key, long windowMs, long now) {
        if (HITS.size() > 20_000) HITS.values().removeIf(d -> d.isEmpty() || now - d.peekLast() > 3_600_000L);
        ArrayDeque<Long> d = HITS.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!d.isEmpty() && now - d.peekFirst() > windowMs) d.pollFirst();
        return d;
    }

    /** Câte evenimente sunt în fereastră. */
    static synchronized int count(String key, long windowMs) {
        return window(key, windowMs, System.currentTimeMillis()).size();
    }

    static synchronized void hit(String key, long windowMs) {
        long now = System.currentTimeMillis();
        window(key, windowMs, now).addLast(now);
    }

    static synchronized void clear(String key) { HITS.remove(key); }
}
