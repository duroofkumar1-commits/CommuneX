package com.communex.dsa.advanced;

/**
 * Segment Tree supporting point updates and inclusive range-sum queries.
 *
 * Supports:
 * 1. Building the tree from an integer array
 * 2. Updating an element at a specific index
 * 3. Calculating the sum of an inclusive range
 */
public final class SegmentTree {

    private final int n;
    private final long[] tree;

    // Initialize the Segment Tree using an input array
    public SegmentTree(int[] values) {

        if (values == null) {
            throw new IllegalArgumentException("Values required");
        }

        n = values.length;
        tree = new long[Math.max(1, n * 4)];

        if (n > 0) {
            build(1, 0, n - 1, values);
        }
    }

    // Build the tree recursively
    private void build(
            int node,
            int left,
            int right,
            int[] values
    ) {
        // Base case: leaf node
        if (left == right) {
            tree[node] = values[left];
            return;
        }

        int middle = (left + right) / 2;

        // Build the left and right subtrees
        build(node * 2, left, middle, values);
        build(node * 2 + 1, middle + 1, right, values);

        // Store the sum of both child nodes
        tree[node] = tree[node * 2] + tree[node * 2 + 1];
    }

    // Return the number of elements in the original array
    public int size() {
        return n;
    }

    // Update the value at a specific index
    public void update(int index, int value) {

        check(index);

        update(1, 0, n - 1, index, value);
    }

    // Recursively update the corresponding tree node
    private void update(
            int node,
            int left,
            int right,
            int index,
            int value
    ) {
        // Base case: update the leaf node
        if (left == right) {
            tree[node] = value;
            return;
        }

        int middle = (left + right) / 2;

        // Find the subtree containing the target index
        if (index <= middle) {
            update(
                    node * 2,
                    left,
                    middle,
                    index,
                    value
            );
        } else {
            update(
                    node * 2 + 1,
                    middle + 1,
                    right,
                    index,
                    value
            );
        }

        // Recalculate the sum of the current node
        tree[node] = tree[node * 2] + tree[node * 2 + 1];
    }

    // Calculate the sum for an inclusive range [left, right]
    public long sum(int left, int right) {

        if (left > right || left < 0 || right >= n) {
            throw new IndexOutOfBoundsException("Invalid range");
        }

        return sum(1, 0, n - 1, left, right);
    }

    // Recursively calculate the sum of a requested range
    private long sum(
            int node,
            int left,
            int right,
            int queryLeft,
            int queryRight
    ) {
        // Return the stored sum when the node is fully covered
        if (queryLeft <= left && right <= queryRight) {
            return tree[node];
        }

        int middle = (left + right) / 2;
        long result = 0;

        // Query the left subtree when the range overlaps it
        if (queryLeft <= middle) {
            result += sum(
                    node * 2,
                    left,
                    middle,
                    queryLeft,
                    queryRight
            );
        }

        // Query the right subtree when the range overlaps it
        if (queryRight > middle) {
            result += sum(
                    node * 2 + 1,
                    middle + 1,
                    right,
                    queryLeft,
                    queryRight
            );
        }

        return result;
    }

    // Validate an array index
    private void check(int index) {

        if (index < 0 || index >= n) {
            throw new IndexOutOfBoundsException("Invalid index");
        }
    }
}