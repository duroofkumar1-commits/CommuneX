
package com.communex.dsa.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Least Recently Used (LRU) cache.
 *
 * Uses LinkedHashMap with access-order enabled to maintain
 * entries from least recently used to most recently used.
 *
 * Provides O(1) average time complexity for get and put.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class LRUCache<K, V> {

    private final int capacity;

    private final Map<K, V> map;

    /**
     * Creates an LRU cache with the specified capacity.
     *
     * @param capacity maximum number of entries
     * @throws IllegalArgumentException if capacity is less than 1
     */
    public LRUCache(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException(
                    "Capacity must be positive"
            );
        }

        this.capacity = capacity;

        this.map = new LinkedHashMap<K, V>(
                capacity,
                0.75f,
                true
        ) {
            @Override
            protected boolean removeEldestEntry(
                    Map.Entry<K, V> eldest
            ) {
                return size() > LRUCache.this.capacity;
            }
        };
    }

    /**
     * Retrieves a value from the cache.
     *
     * A successful lookup updates the entry's access order,
     * making it the most recently used entry.
     *
     * @param key the key to look up
     * @return the associated value, or null if absent
     */
    public V get(K key) {
        return map.get(key);
    }

    /**
     * Inserts a new entry or updates an existing entry.
     *
     * If the cache exceeds its capacity, the least recently
     * used entry is automatically removed.
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

        map.put(key, value);
    }

    /**
     * Returns the current number of entries in the cache.
     *
     * @return the cache size
     */
    public int size() {
        return map.size();
    }

    /**
     * Checks whether the cache contains the specified key.
     *
     * @param key the key to check
     * @return true if the key exists, otherwise false
     */
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }
}
