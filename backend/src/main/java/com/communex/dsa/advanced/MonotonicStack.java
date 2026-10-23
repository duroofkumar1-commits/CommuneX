package com.communex.dsa.advanced;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

/**
 * Monotonic stack for next-smaller and next-greater element problems.
 *
 * Stores array indices and removes indices whose corresponding
 * values are greater than the supplied value.
 */
public final class MonotonicStack {

    private final ArrayDeque<Integer> stack = new ArrayDeque<>();

    /**
     * Removes indices while their corresponding values are greater
     * than the specified value.
     *
     * @param value value used for comparison
     * @param values source array
     */
    public void pushWhileGreater(int value, int[] values) {

        while (!stack.isEmpty()
                && values[stack.peek()] > value) {

            stack.pop();
        }
    }

    /**
     * Pushes an index onto the stack.
     *
     * @param index index to push
     */
    public void push(int index) {
        stack.push(index);
    }

    /**
     * Removes and returns the top index.
     *
     * @return top index
     * @throws NoSuchElementException if the stack is empty
     */
    public int pop() {

        if (stack.isEmpty()) {
            throw new NoSuchElementException("Stack is empty");
        }

        return stack.pop();
    }

    /**
     * Returns the top index without removing it.
     *
     * @return top index
     * @throws NoSuchElementException if the stack is empty
     */
    public int peek() {

        if (stack.isEmpty()) {
            throw new NoSuchElementException("Stack is empty");
        }

        return stack.peek();
    }

    /**
     * Checks whether the stack is empty.
     *
     * @return true if empty; otherwise false
     */
    public boolean isEmpty() {
        return stack.isEmpty();
    }
}