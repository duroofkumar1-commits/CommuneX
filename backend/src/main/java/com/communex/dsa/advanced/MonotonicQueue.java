package com.communex.dsa.advanced;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

/**
 * Monotonic decreasing deque for sliding-window maximum queries.
 *
 * Stores array indices in decreasing order of their corresponding values.
 * The index at the front represents the maximum value in the current window.
 */
public final class MonotonicQueue {

    private final ArrayDeque<Integer> indices = new ArrayDeque<>();

    /**
     * Adds an index while maintaining decreasing value order.
     *
     * @param index index of the new value
     * @param value value at the specified index
     * @param values source array
     */
    public void add(int index, int value, int[] values) {

        // Remove indices whose values cannot be the maximum anymore
        while (!indices.isEmpty()
                && values[indices.peekLast()] <= value) {

            indices.removeLast();
        }

        // Add the new index at the back
        indices.addLast(index);
    }

    /**
     * Removes indices that fall before the current window.
     *
     * @param firstValidIndex first valid index in the window
     */
    public void removeBefore(int firstValidIndex) {

        while (!indices.isEmpty()
                && indices.peekFirst() < firstValidIndex) {

            indices.removeFirst();
        }
    }

    /**
     * Returns the index of the current maximum value.
     *
     * @return index at the front of the deque
     * @throws NoSuchElementException if the queue is empty
     */
    public int maxIndex() {

        if (indices.isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }

        return indices.peekFirst();
    }

    /**
     * Checks whether the queue contains any indices.
     *
     * @return true if empty; otherwise false
     */
    public boolean isEmpty() {
        return indices.isEmpty();
    }
}