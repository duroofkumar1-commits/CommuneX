package com.communex.dsa.linkedlist;

import java.util.ArrayList;
import java.util.List;

/**
 * Circular singly linked list.
 *
 * The next() operation wraps around to the first element,
 * making this list suitable for repeat-all playlist navigation.
 *
 * @param <T> type of elements stored in the list
 */
public final class CircularLinkedList<T> {

    // Represents a node in the linked list
    private static final class Node<T> {

        private final T value;
        private Node<T> next;

        private Node(T value) {
            this.value = value;
        }
    }

    private Node<T> tail;
    private Node<T> current;
    private int size;

    // Return the number of elements
    public int size() {
        return size;
    }

    // Check whether the list is empty
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Adds an element to the end of the circular linked list.
     */
    public void add(T value) {

        Node<T> newNode = new Node<>(value);

        // Initialize the first node
        if (tail == null) {
            newNode.next = newNode;

            tail = newNode;
            current = newNode;
        } else {
            // Insert the new node after the current tail
            newNode.next = tail.next;
            tail.next = newNode;
            tail = newNode;
        }

        size++;
    }

    /**
     * Returns the current element.
     *
     * @return current element, or null if the list is empty
     */
    public T current() {

        if (current == null) {
            return null;
        }

        return current.value;
    }

    /**
     * Moves to the next element.
     *
     * The list is circular, so moving past the last element
     * returns to the first element.
     *
     * @return the new current element, or null if the list is empty
     */
    public T next() {

        if (current != null) {
            current = current.next;
        }

        return current();
    }

    /**
     * Returns all elements in insertion order.
     *
     * @return an immutable list of the stored elements
     */
    public List<T> values() {

        List<T> list = new ArrayList<>(size);

        if (tail == null) {
            return list;
        }

        // The first node is immediately after the tail
        Node<T> node = tail.next;

        // Traverse exactly size nodes to avoid an infinite loop
        for (int i = 0; i < size; i++) {
            list.add(node.value);
            node = node.next;
        }

        return List.copyOf(list);
    }
}