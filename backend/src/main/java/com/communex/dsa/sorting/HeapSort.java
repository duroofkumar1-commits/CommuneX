package com.communex.dsa.sorting;

/**
 * Heap Sort implementation with O(n log n) time complexity.
 *
 * Sorts a defensive copy of the input array in ascending order,
 * leaving the original array unchanged.
 */
public final class HeapSort {

    // Prevent object creation.
    private HeapSort() {
    }

    /**
     * Sorts the input array using Heap Sort.
     *
     * @param input the array to sort
     * @return a sorted copy of the input array
     */
    public static int[] sort(int[] input) {
        int[] values = BubbleSort.copy(input);
        int length = values.length;

        // Build a max heap.
        for (int i = length / 2 - 1; i >= 0; i--) {
            siftDown(values, length, i);
        }

        // Move the largest element to the end of the array.
        for (int end = length - 1; end > 0; end--) {
            int temporary = values[0];
            values[0] = values[end];
            values[end] = temporary;

            // Restore the max-heap property for the remaining elements.
            siftDown(values, end, 0);
        }

        return values;
    }

    /**
     * Restores the max-heap property for the subtree rooted at root.
     *
     * @param values the array representing the heap
     * @param size the active heap size
     * @param root the root index of the subtree
     */
    private static void siftDown(int[] values, int size, int root) {
        while (true) {
            int largest = root;
            int leftChild = 2 * root + 1;
            int rightChild = leftChild + 1;

            if (leftChild < size && values[leftChild] > values[largest]) {
                largest = leftChild;
            }

            if (rightChild < size && values[rightChild] > values[largest]) {
                largest = rightChild;
            }

            // The heap property is satisfied.
            if (largest == root) {
                return;
            }

            // Swap the root with its largest child.
            int temporary = values[root];
            values[root] = values[largest];
            values[largest] = temporary;

            // Continue sifting down from the child's position.
            root = largest;
        }
    }
}