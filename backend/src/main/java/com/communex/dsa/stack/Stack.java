
package com.communex.dsa.stack;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

/**
 * Generic Stack implementation following the
 * Last In, First Out (LIFO) principle.
 *
 * Useful for undo/redo histories and other stack-based operations.
 *
 * @param <T> the type of elements stored in the stack
 */
public final class Stack<T> {

    private final ArrayDeque<T> data = new ArrayDeque<>();

    /**
     * Adds an element to the top of the stack.
     *
     * @param value the element to add
     * @throws IllegalArgumentException if the value is null
     */
    public void push(T value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Null values are not supported"
            );
        }

        data.push(value);
    }

    /**
     * Removes and returns the top element.
     *
     * @return the removed element
     * @throws NoSuchElementException if the stack is empty
     */
    public T pop() {
        if (data.isEmpty()) {
            throw new NoSuchElementException(
                    "Stack is empty"
            );
        }

        return data.pop();
    }

    /**
     * Returns the top element without removing it.
     *
     * @return the top element
     * @throws NoSuchElementException if the stack is empty
     */
    public T peek() {
        if (data.isEmpty()) {
            throw new NoSuchElementException(
                    "Stack is empty"
            );
        }

        return data.peek();
    }

    /**
     * Checks whether the stack contains no elements.
     *
     * @return true if the stack is empty, otherwise false
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Returns the number of elements in the stack.
     *
     * @return the current stack size
     */
    public int size() {
        return data.size();
    }

    /**
     * Removes all elements from the stack.
     */
    public void clear() {
        data.clear();
    }
}
