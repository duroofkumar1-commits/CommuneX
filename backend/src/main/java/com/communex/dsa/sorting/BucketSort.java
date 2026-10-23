package com.communex.dsa.sorting;

import java.util.ArrayList;
import java.util.List;

/**
 * Educational Bucket Sort implementation.
 *
 * Supports finite values in the inclusive range [0, 1].
 * Each bucket is sorted using insertion sort before the results
 * are combined into the final sorted array.
 */
public final class BucketSort {

    // Prevent object creation
    private BucketSort() {
    }

    /**
     * Sorts values in ascending order using Bucket Sort.
     *
     * @param input array containing finite values between 0 and 1
     * @return a new sorted array
     * @throws IllegalArgumentException if the input is null or
     *         contains values outside the supported range
     */
    public static double[] sort(double[] input) {

        // Validate the input reference
        if (input == null) {
            throw new IllegalArgumentException(
                    "Input cannot be null"
            );
        }

        // Preserve the original input array
        double[] result = input.clone();

        // Validate values and handle small arrays
        if (result.length < 2) {
            validate(result);
            return result;
        }

        validate(result);

        // Step 1: Create buckets
        List<List<Double>> buckets = new ArrayList<>(result.length);

        for (int i = 0; i < result.length; i++) {
            buckets.add(new ArrayList<>());
        }

        // Step 2: Distribute values across buckets
        for (double value : result) {

            int bucketIndex = (int) (value * result.length);

            // Value 1.0 maps to the final bucket
            if (bucketIndex == result.length) {
                bucketIndex--;
            }

            buckets.get(bucketIndex).add(value);
        }

        // Step 3: Sort each bucket using insertion sort
        int outputIndex = 0;

        for (List<Double> bucket : buckets) {

            for (int i = 1; i < bucket.size(); i++) {

                double value = bucket.get(i);
                int j = i - 1;

                // Shift larger elements one position to the right
                while (j >= 0 && bucket.get(j) > value) {
                    bucket.set(j + 1, bucket.get(j));
                    j--;
                }

                // Insert the value at its correct position
                bucket.set(j + 1, value);
            }

            // Step 4: Merge sorted buckets into the result array
            for (double value : bucket) {
                result[outputIndex] = value;
                outputIndex++;
            }
        }

        return result;
    }

    /**
     * Validates that all values are finite and within [0, 1].
     *
     * @param input array to validate
     * @throws IllegalArgumentException if a value is invalid
     */
    private static void validate(double[] input) {

        for (double value : input) {

            if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException(
                        "Bucket sort values must be finite and in [0, 1]"
                );
            }
        }
    }
}