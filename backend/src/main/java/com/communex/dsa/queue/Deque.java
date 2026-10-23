package com.communex.dsa.queue;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

/**
 * Double-ended queue (Deque) wrapper.
 *
 * Supports insertion and removal at both ends of the queue.
 *
 * @param <T> type of elements stored in the deque
 */
public final class Deque<T> {

    private final ArrayDeque<T> data = new ArrayDeque<>();

    // Add an element to the front
    public void addFirst(T value) {
        data.addFirst(value);
    }

    // Add an element to the back
    public void addLast(T value) {
        data.addLast(value);
    }

    /**
     * Remove and return the first element.
     *
     * @return the removed element
     * @throws NoSuchElementException if the deque is empty
     */
    public T removeFirst() {

        if (data.isEmpty()) {
            throw new NoSuchElementException("Deque is empty");
        }

        return data.removeFirst();
    }

    /**
     * Remove and return the last element.
     *
     * @return the removed element
     * @throws NoSuchElementException if the deque is empty
     */
    public T removeLast() {

        if (data.isEmpty()) {
            throw new NoSuchElementException("Deque is empty");
        }

        return data.removeLast();
    }

    // Return the first element without removing it
    public T peekFirst() {
        return data.peekFirst();
    }

    // Return the last element without removing it
    public T peekLast() {
        return data.peekLast();
    }

    // Return the number of elements
    public int size() {
        return data.size();
    }

    // Check whether the deque is empty
    public boolean isEmpty() {
        return data.isEmpty();
    }
}