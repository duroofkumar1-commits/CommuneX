package com.communex.dsa.queue;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

/**
 * Generic FIFO queue for ordered processing.
 *
 * FIFO means First In, First Out:
 * the element inserted first is removed first.
 *
 * @param <T> type of elements stored in the queue
 */
public final class Queue<T> {

    private final ArrayDeque<T> data = new ArrayDeque<>();

    /**
     * Adds an element to the rear of the queue.
     *
     * @param value element to insert
     * @throws IllegalArgumentException if the value is null
     */
    public void enqueue(T value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Null values are not supported"
            );
        }

        data.addLast(value);
    }

    /**
     * Removes and returns the front element.
     *
     * @return the removed element
     * @throws NoSuchElementException if the queue is empty
     */
    public T dequeue() {

        if (data.isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }

        return data.removeFirst();
    }

    /**
     * Returns the front element without removing it.
     *
     * @return the front element
     * @throws NoSuchElementException if the queue is empty
     */
    public T peek() {

        if (data.isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }

        return data.getFirst();
    }

    // Returns the number of elements in the queue
    public int size() {
        return data.size();
    }

    // Checks whether the queue is empty
    public boolean isEmpty() {
        return data.isEmpty();
    }
}