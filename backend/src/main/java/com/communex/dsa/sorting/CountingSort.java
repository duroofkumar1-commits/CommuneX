package com.communex.dsa.sorting;

/**
 * Counting Sort implementation supporting negative integers.
 *
 * Limits the number of buckets to control memory usage.
 * For very large value ranges, Merge Sort or Radix Sort
 * may be a more appropriate choice.
 */
public final class CountingSort {

    private static final long MAX_BUCKETS = 1_000_000L;

    // Prevent object creation
    private CountingSort() {
    }

    /**
     * Sorts an integer array in ascending order.
     *
     * @param input array of integers to sort
     * @return a new sorted array
     * @throws IllegalArgumentException if the value range exceeds
     *         the configured bucket limit
     */
    public static int[] sort(int[] input) {

        // Copy the input to preserve the original array
        int[] values = BubbleSort.copy(input);

        // No sorting is required for arrays with fewer than two elements
        if (values.length < 2) {
            return values;
        }

        // Find the minimum and maximum values
        int min = values[0];
        int max = values[0];

        for (int value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        // Calculate the number of buckets using long arithmetic
        long range = (long) max - (long) min + 1;

        // Prevent excessive memory allocation
        if (range > MAX_BUCKETS) {
            throw new IllegalArgumentException(
                    "Counting sort range is too large; "
                            + "use merge sort or radix sort."
            );
        }

        // Count the occurrences of each value
        int[] counts = new int[(int) range];

        for (int value : values) {
            int index = (int) ((long) value - min);
            counts[index]++;
        }

        // Reconstruct the sorted array from the frequency counts
        int index = 0;

        for (int i = 0; i < counts.length; i++) {

            while (counts[i]-- > 0) {
                values[index] = (int) ((long) min + i);
                index++;
            }
        }

        return values;
    }
}