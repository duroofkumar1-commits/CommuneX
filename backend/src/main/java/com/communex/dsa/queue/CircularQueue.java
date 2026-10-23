package com.communex.dsa.queue;

import java.util.NoSuchElementException;

/**
 * Fixed-capacity circular queue implemented using a ring buffer.
 *
 * Supports enqueue and dequeue operations in O(1) time.
 *
 * @param <T> type of elements stored in the queue
 */
public final class CircularQueue<T> {

    private final Object[] elements;

    private int head;
    private int tail;
    private int size;

    // Initialize the queue with a fixed capacity
    public CircularQueue(int capacity) {

        if (capacity < 1) {
            throw new IllegalArgumentException(
                    "Capacity must be positive"
            );
        }

        elements = new Object[capacity];
    }

    // Return the number of elements currently in the queue
    public int size() {
        return size;
    }

    // Return the maximum capacity of the queue
    public int capacity() {
        return elements.length;
    }

    // Check whether the queue is empty
    public boolean isEmpty() {
        return size == 0;
    }

    // Check whether the queue is full
    public boolean isFull() {
        return size == elements.length;
    }

    /**
     * Adds an element to the rear of the queue.
     *
     * @param value element to add
     * @throws IllegalStateException if the queue is full
     */
    public void enqueue(T value) {

        if (isFull()) {
            throw new IllegalStateException("Queue is full");
        }

        // Insert the element at the tail position
        elements[tail] = value;

        // Move the tail forward and wrap around when necessary
        tail = (tail + 1) % elements.length;

        size++;
    }

    /**
     * Removes and returns the front element.
     *
     * @return the removed element
     * @throws NoSuchElementException if the queue is empty
     */
    @SuppressWarnings("unchecked")
    public T dequeue() {

        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }

        // Retrieve the front element
        T value = (T) elements[head];

        // Clear the slot to release the stored reference
        elements[head] = null;

        // Move the head forward and wrap around when necessary
        head = (head + 1) % elements.length;

        size--;

        return value;
    }
}