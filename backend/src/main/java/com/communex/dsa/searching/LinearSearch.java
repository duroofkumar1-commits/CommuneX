
package com.communex.dsa.searching;

/**
 * Linear Search implementation.
 *
 * Searches an array sequentially from the first element
 * to the last element.
 *
 * Returns the first matching index, or -1 if the target
 * value is not present.
 */
public final class LinearSearch {

    // Prevent object creation.
    private LinearSearch() {
    }

    /**
     * Searches for the target value in the given array.
     *
     * @param values the array to search
     * @param target the value to find
     * @return the first matching index, or -1 if not found
     * @throws IllegalArgumentException if the input array is null
     */
    public static int indexOf(int[] values, int target) {
        if (values == null) {
            throw new IllegalArgumentException(
                    "Input cannot be null"
            );
        }

        // Check each element sequentially.
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target) {
                return i;
            }
        }

        // Target was not found.
        return -1;
    }
}
