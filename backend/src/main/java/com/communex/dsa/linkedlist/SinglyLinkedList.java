package com.communex.dsa.linkedlist;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Generic singly linked list for learning and small in-memory workflows.
 *
 * Supports insertion, removal, searching, and iteration.
 *
 * @param <T> type of elements stored in the list
 */
public final class SinglyLinkedList<T> implements Iterable<T> {

    private Node<T> head;
    private Node<T> tail;
    private int size;

    /**
     * Represents a node in the singly linked list.
     */
    private static final class Node<T> {

        private final T value;
        private Node<T> next;

        private Node(T value) {
            this.value = value;
        }
    }

    // Return the number of elements in the list
    public int size() {
        return size;
    }

    // Check whether the list is empty
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Adds an element to the beginning of the list.
     *
     * @param value element to add
     */
    public void addFirst(T value) {

        Node<T> newNode = new Node<>(value);

        newNode.next = head;
        head = newNode;

        // Initialize the tail when inserting the first element
        if (tail == null) {
            tail = newNode;
        }

        size++;
    }

    /**
     * Adds an element to the end of the list.
     *
     * @param value element to add
     */
    public void addLast(T value) {

        Node<T> newNode = new Node<>(value);

        if (tail == null) {
            head = newNode;
        } else {
            tail.next = newNode;
        }

        tail = newNode;
        size++;
    }

    /**
     * Removes and returns the first element.
     *
     * @return removed element
     * @throws NoSuchElementException if the list is empty
     */
    public T removeFirst() {

        if (head == null) {
            throw new NoSuchElementException("List is empty");
        }

        T value = head.value;

        head = head.next;

        // Clear the tail when the list becomes empty
        if (head == null) {
            tail = null;
        }

        size--;

        return value;
    }

    /**
     * Removes the first occurrence of the specified value.
     *
     * Uses Objects.equals() to support null values.
     *
     * @param value value to remove
     * @return true if an element was removed; otherwise false
     */
    public boolean remove(T value) {

        Node<T> previous = null;
        Node<T> current = head;

        while (current != null) {

            if (Objects.equals(current.value, value)) {

                // Update the head when removing the first node
                if (previous == null) {
                    head = current.next;
                } else {
                    previous.next = current.next;
                }

                // Update the tail when removing the last node
                if (current == tail) {
                    tail = previous;
                }

                size--;

                return true;
            }

            previous = current;
            current = current.next;
        }

        return false;
    }

    /**
     * Checks whether the list contains the specified value.
     *
     * @param value value to search for
     * @return true if found; otherwise false
     */
    public boolean contains(T value) {

        for (T item : this) {

            if (Objects.equals(item, value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns an iterator over the elements in list order.
     *
     * @return iterator starting at the head
     */
    @Override
    public Iterator<T> iterator() {

        return new Iterator<T>() {

            private Node<T> current = head;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {

                if (current == null) {
                    throw new NoSuchElementException();
                }

                T value = current.value;
                current = current.next;

                return value;
            }
        };
    }
}