
package com.communex.dsa.cache;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Least Frequently Used (LFU) cache.
 *
 * Entries with the lowest access frequency are evicted first.
 * When multiple entries have the same frequency, the
 * least recently used entry is evicted.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class LFUCache<K, V> {

    private final int capacity;

    private int minFrequency;

    private final Map<K, V> values = new HashMap<>();

    private final Map<K, Integer> frequencies = new HashMap<>();

    private final Map<Integer, LinkedHashSet<K>> buckets =
            new HashMap<>();

    /**
     * Creates an LFU cache with the specified capacity.
     *
     * @param capacity maximum number of entries
     * @throws IllegalArgumentException if capacity is less than 1
     */
    public LFUCache(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException(
                    "Capacity must be positive"
            );
        }

        this.capacity = capacity;
    }

    /**
     * Retrieves a value from the cache.
     *
     * A successful lookup increases the entry's frequency.
     *
     * @param key the key to look up
     * @return the associated value, or null if the key is absent
     */
    public V get(K key) {
        if (!values.containsKey(key)) {
            return null;
        }

        touch(key);

        return values.get(key);
    }

    /**
     * Inserts a new entry or updates an existing entry.
     *
     * When the cache is full, the least frequently used entry
     * is removed. Ties are resolved using least-recently-used order.
     *
     * @param key the entry key
     * @param value the entry value
     * @throws IllegalArgumentException if the key or value is null
     */
    public void put(K key, V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException(
                    "Null keys/values are not supported"
            );
        }

        // Update an existing entry and increase its frequency.
        if (values.containsKey(key)) {
            values.put(key, value);
            touch(key);
            return;
        }

        // Evict the least frequently used entry if the cache is full.
        if (values.size() == capacity) {
            LinkedHashSet<K> bucket = buckets.get(minFrequency);

            K victim = bucket.iterator().next();

            bucket.remove(victim);

            if (bucket.isEmpty()) {
                buckets.remove(minFrequency);
            }

            values.remove(victim);
            frequencies.remove(victim);
        }

        // New entries start with a frequency of 1.
        values.put(key, value);
        frequencies.put(key, 1);

        buckets.computeIfAbsent(
                1,
                frequency -> new LinkedHashSet<>()
        ).add(key);

        minFrequency = 1;
    }

    /**
     * Increases the access frequency of an existing entry
     * and moves it to the corresponding frequency bucket.
     *
     * LinkedHashSet preserves insertion order, allowing the
     * least recently used entry in a bucket to be identified.
     *
     * @param key the key whose frequency should be increased
     */
    private void touch(K key) {
        int oldFrequency = frequencies.get(key);

        LinkedHashSet<K> currentBucket =
                buckets.get(oldFrequency);

        currentBucket.remove(key);

        // Remove empty buckets and update the minimum frequency.
        if (currentBucket.isEmpty()) {
            buckets.remove(oldFrequency);

            if (minFrequency == oldFrequency) {
                minFrequency = oldFrequency + 1;
            }
        }

        int newFrequency = oldFrequency + 1;

        frequencies.put(key, newFrequency);

        buckets.computeIfAbsent(
                newFrequency,
                frequency -> new LinkedHashSet<>()
        ).add(key);
    }

    /**
     * Returns the current number of entries in the cache.
     *
     * @return the cache size
     */
    public int size() {
        return values.size();
    }
}
