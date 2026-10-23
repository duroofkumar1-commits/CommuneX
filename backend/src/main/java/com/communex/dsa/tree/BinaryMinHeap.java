
package com.communex.dsa.tree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Binary Min-Heap implementation.
 *
 * The element with the highest priority, as determined by
 * the comparator, is always available at the root.
 *
 * Insertion and removal take O(log n) time.
 *
 * @param <T> the type of elements stored in the heap
 */
public final class BinaryMinHeap<T> {

    private final List<T> items = new ArrayList<>();

    private final Comparator<? super T> comparator;

    /**
     * Creates an empty min-heap using the specified comparator.
     *
     * @param comparator defines the priority order of elements
     * @throws NullPointerException if the comparator is null
     */
    public BinaryMinHeap(Comparator<? super T> comparator) {
        this.comparator = Objects.requireNonNull(comparator);
    }

    /**
     * Returns the number of elements in the heap.
     *
     * @return the current heap size
     */
    public int size() {
        return items.size();
    }

    /**
     * Checks whether the heap contains no elements.
     *
     * @return true if the heap is empty, otherwise false
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Inserts an element into the heap and restores
     * the min-heap property.
     *
     * @param value the element to insert
     */
    public void add(T value) {
        items.add(value);

        // Restore heap order by moving the new element upward.
        siftUp(items.size() - 1);
    }

    /**
     * Returns the highest-priority element without removing it.
     *
     * @return the root element
     * @throws NoSuchElementException if the heap is empty
     */
    public T peek() {
        if (items.isEmpty()) {
            throw new NoSuchElementException("Heap is empty");
        }

        return items.get(0);
    }

    /**
     * Removes and returns the highest-priority element.
     *
     * The last element replaces the root, and the heap
     * property is restored by moving it downward.
     *
     * @return the removed root element
     * @throws NoSuchElementException if the heap is empty
     */
    public T remove() {
        if (items.isEmpty()) {
            throw new NoSuchElementException("Heap is empty");
        }

        T first = items.get(0);
        T last = items.remove(items.size() - 1);

        if (!items.isEmpty()) {
            items.set(0, last);
            siftDown(0);
        }

        return first;
    }

    /**
     * Moves an element upward until the min-heap property
     * is satisfied.
     *
     * @param index the index of the element to move
     */
    private void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;

            // Stop when the parent has equal or higher priority.
            if (comparator.compare(
                    items.get(index),
                    items.get(parent)
            ) >= 0) {
                break;
            }

            swap(index, parent);

            index = parent;
        }
    }

    /**
     * Moves an element downward until the min-heap property
     * is satisfied.
     *
     * @param index the index of the element to move
     */
    private void siftDown(int index) {
        while (true) {
            int leftChild = 2 * index + 1;
            int rightChild = leftChild + 1;
            int smallest = index;

            // Find the highest-priority element among the node
            // and its existing children.
            if (leftChild < items.size()
                    && comparator.compare(
                            items.get(leftChild),
                            items.get(smallest)
                    ) < 0) {
                smallest = leftChild;
            }

            if (rightChild < items.size()
                    && comparator.compare(
                            items.get(rightChild),
                            items.get(smallest)
                    ) < 0) {
                smallest = rightChild;
            }

            // Stop when the current node is already in the
            // correct position.
            if (smallest == index) {
                return;
            }

            swap(index, smallest);

            index = smallest;
        }
    }

    /**
     * Swaps two elements in the heap.
     *
     * @param firstIndex the index of the first element
     * @param secondIndex the index of the second element
     */
    private void swap(int firstIndex, int secondIndex) {
        T temporary = items.get(firstIndex);

        items.set(firstIndex, items.get(secondIndex));
        items.set(secondIndex, temporary);
    }
}
