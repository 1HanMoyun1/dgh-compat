package com.hanmoyun.dghcompat.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Bounded, latest-value queue for state packets arriving before their entities. Client thread only.
 */
public final class PendingEntityStates<K, V> {
    private record Pending<V>(V value, long expiresAt) {}

    private final Map<K, Pending<V>> pending = new LinkedHashMap<>();
    private final int capacity;
    private final int lifetimeTicks;
    private long tick;

    public PendingEntityStates(int capacity, int lifetimeTicks) {
        if (capacity < 1 || lifetimeTicks < 1)
            throw new IllegalArgumentException("Invalid queue limits");
        this.capacity = capacity;
        this.lifetimeTicks = lifetimeTicks;
    }

    public void put(K key, V value) {
        pending.remove(key);
        pending.put(key, new Pending<>(value, tick + lifetimeTicks));
        if (pending.size() > capacity) pending.remove(pending.keySet().iterator().next());
    }

    public void tick(Predicate<V> applyIfPresent) {
        tick++;
        pending.values()
                .removeIf(entry -> entry.expiresAt() <= tick || applyIfPresent.test(entry.value()));
    }

    public void remove(K key) {
        pending.remove(key);
    }

    public void clear() {
        pending.clear();
        tick = 0;
    }

    public int size() {
        return pending.size();
    }
}
