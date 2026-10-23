package com.communex.dsa.advanced;

/**
 * Fenwick Tree, also known as a Binary Indexed Tree (BIT).
 *
 * Supports point updates, prefix sums, and range sums efficiently.
 */
public final class FenwickTree {

    private final long[] bit;

    // Initialize an empty Fenwick Tree
    public FenwickTree(int size) {

        if (size < 0) {
            throw new IllegalArgumentException(
                    "Size cannot be negative"
            );
        }

        bit = new long[size + 1];
    }

    // Initialize the tree using an existing integer array
    public FenwickTree(int[] values) {

        this(values.length);

        for (int i = 0; i < values.length; i++) {
            add(i, values[i]);
        }
    }

    // Return the number of elements represented by the tree
    public int size() {
        return bit.length - 1;
    }

    /**
     * Adds a value to the element at the specified index.
     *
     * @param index zero-based element index
     * @param delta value to add
     */
    public void add(int index, long delta) {

        check(index);

        // Update all relevant Fenwick Tree nodes
        for (int i = index + 1; i < bit.length; i += i & -i) {
            bit[i] += delta;
        }
    }

    /**
     * Calculates the sum from index 0 to the specified index.
     *
     * @param index zero-based inclusive endpoint
     * @return prefix sum
     */
    public long prefixSum(int index) {

        // An index below zero represents an empty prefix
        if (index < 0) {
            return 0;
        }

        if (index >= size()) {
            throw new IndexOutOfBoundsException(
                    "Invalid index"
            );
        }

        long sum = 0;

        // Traverse Fenwick Tree nodes to calculate the prefix sum
        for (int i = index + 1; i > 0; i -= i & -i) {
            sum += bit[i];
        }

        return sum;
    }

    /**
     * Calculates the sum within an inclusive range.
     *
     * @param left starting index
     * @param right ending index
     * @return sum from left to right, inclusive
     */
    public long rangeSum(int left, int right) {

        if (left < 0 || right < left || right >= size()) {
            throw new IndexOutOfBoundsException(
                    "Invalid range"
            );
        }

        return prefixSum(right) - prefixSum(left - 1);
    }

    // Validate a zero-based element index
    private void check(int index) {

        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException(
                    "Invalid index"
            );
        }
    }
}