
package com.communex.dsa.searching;

/**
 * Binary Search implementation for an ascending sorted array.
 *
 * Returns the index of a matching target if found.
 * Returns -1 if the target does not exist in the array.
 */
public final class BinarySearch {

    // Prevent object creation.
    private BinarySearch() {
    }

    /**
     * Searches for a target value using binary search.
     *
     * @param values the array sorted in ascending order
     * @param target the value to search for
     * @return the index of a matching element, or -1 if not found
     * @throws IllegalArgumentException if the input array is null
     */
    public static int indexOf(int[] values, int target) {
        if (values == null) {
            throw new IllegalArgumentException(
                    "Input cannot be null"
            );
        }

        int low = 0;
        int high = values.length - 1;

        while (low <= high) {
            // Calculate the midpoint without risking low + high overflow.
            int mid = low + (high - low) / 2;

            if (values[mid] == target) {
                return mid;
            }

            if (values[mid] < target) {
                // Search the right half.
                low = mid + 1;
            } else {
                // Search the left half.
                high = mid - 1;
            }
        }

        return -1;
    }
}
