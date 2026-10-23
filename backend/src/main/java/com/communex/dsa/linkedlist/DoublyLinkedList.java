package com.communex.dsa.linkedlist;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Doubly linked list with constant-time insertion and removal
 * at both ends.
 *
 * Useful for playlist navigation in both forward and backward
 * directions.
 *
 * @param <T> type of elements stored in the list
 */
public final class DoublyLinkedList<T> {

    /**
     * Represents a node in the doubly linked list.
     */
    public static final class Node<T> {

        private final T value;
        private Node<T> previous;
        private Node<T> next;

        private Node(T value) {
            this.value = value;
        }

        // Return the value stored in this node
        public T value() {
            return value;
        }

        // Return the previous node
        public Node<T> previous() {
            return previous;
        }

        // Return the next node
        public Node<T> next() {
            return next;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    // Return the number of elements in the list
    public int size() {
        return size;
    }

    // Check whether the list is empty
    public boolean isEmpty() {
        return size == 0;
    }

    // Return the first node
    public Node<T> first() {
        return head;
    }

    // Return the last node
    public Node<T> last() {
        return tail;
    }

    /**
     * Adds an element to the end of the list.
     *
     * @param value element to add
     * @return the newly created node
     */
    public Node<T> addLast(T value) {

        Node<T> newNode = new Node<>(value);

        newNode.previous = tail;

        if (tail == null) {
            head = newNode;
        } else {
            tail.next = newNode;
        }

        tail = newNode;
        size++;

        return newNode;
    }

    /**
     * Adds an element to the beginning of the list.
     *
     * @param value element to add
     * @return the newly created node
     */
    public Node<T> addFirst(T value) {

        Node<T> newNode = new Node<>(value);

        newNode.next = head;

        if (head == null) {
            tail = newNode;
        } else {
            head.previous = newNode;
        }

        head = newNode;
        size++;

        return newNode;
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

        if (head == null) {
            tail = null;
        } else {
            head.previous = null;
        }

        size--;

        return value;
    }

    /**
     * Removes and returns the last element.
     *
     * @return removed element
     * @throws NoSuchElementException if the list is empty
     */
    public T removeLast() {

        if (tail == null) {
            throw new NoSuchElementException("List is empty");
        }

        T value = tail.value;

        tail = tail.previous;

        if (tail == null) {
            head = null;
        } else {
            tail.next = null;
        }

        size--;

        return value;
    }

    /**
     * Returns all elements in their current order.
     *
     * @return immutable list containing the elements
     */
    public List<T> toList() {

        List<T> result = new ArrayList<>(size);

        Node<T> node = head;

        while (node != null) {
            result.add(node.value);
            node = node.next;
        }

        return List.copyOf(result);
    }
}