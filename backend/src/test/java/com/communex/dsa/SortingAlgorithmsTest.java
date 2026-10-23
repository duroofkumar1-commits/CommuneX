
package com.communex.dsa;

import com.communex.dsa.sorting.BubbleSort;
import com.communex.dsa.sorting.BucketSort;
import com.communex.dsa.sorting.CountingSort;
import com.communex.dsa.sorting.HeapSort;
import com.communex.dsa.sorting.InsertionSort;
import com.communex.dsa.sorting.MergeSort;
import com.communex.dsa.sorting.QuickSort;
import com.communex.dsa.sorting.RadixSort;
import com.communex.dsa.sorting.SelectionSort;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 * Tests sorting algorithms for correctness, input preservation,
 * boundary conditions, and invalid input handling.
 */
class SortingAlgorithmsTest {

    private final int[] input = {
        9,
        -1,
        5,
        5,
        0,
        Integer.MIN_VALUE,
        Integer.MAX_VALUE,
        -20
    };

    private final int[] expected =
        Arrays.stream(input).sorted().toArray();

    @Test
    void comparisonSortsSortWithoutMutatingInput() {
        int[] original = input.clone();

        List<int[]> results = List.of(
            BubbleSort.sort(input),
            SelectionSort.sort(input),
            InsertionSort.sort(input),
            MergeSort.sort(input),
            QuickSort.sort(input),
            HeapSort.sort(input)
        );

        results.forEach(
            result -> assertArrayEquals(expected, result)
        );

        assertArrayEquals(
            original,
            input,
            "Sort implementations should leave their input unchanged"
        );
    }

    @Test
    void countingSortHandlesNegativesAndDuplicates() {
        int[] values = {2, -5, 3, 2, 0, -5};
        int[] sorted = {-5, -5, 0, 2, 2, 3};

        assertArrayEquals(
            sorted,
            CountingSort.sort(values)
        );
    }

    @Test
    void countingSortRejectsUnboundedRanges() {
        assertThrows(
            IllegalArgumentException.class,
            () -> CountingSort.sort(
                new int[] {
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE
                }
            )
        );
    }

    @Test
    void radixSortHandlesSignedIntegerExtremes() {
        assertArrayEquals(
            expected,
            RadixSort.sort(input)
        );
    }

    @Test
    void bucketSortHandlesBoundariesAndDuplicates() {
        double[] values = {1.0, 0.5, 0.0, 0.1, 0.5};
        double[] sorted = {0.0, 0.1, 0.5, 0.5, 1.0};

        assertArrayEquals(
            sorted,
            BucketSort.sort(values)
        );
    }

    @Test
    void bucketSortRejectsInvalidDomain() {
        assertThrows(
            IllegalArgumentException.class,
            () -> BucketSort.sort(
                new double[] {-0.1, 0.3}
            )
        );
    }

    @Test
    void allSortsHandleEmptyAndSingleItem() {
        assertArrayEquals(
            new int[0],
            MergeSort.sort(new int[0])
        );

        assertArrayEquals(
            new int[] {4},
            QuickSort.sort(new int[] {4})
        );

        assertArrayEquals(
            new int[] {4},
            CountingSort.sort(new int[] {4})
        );

        assertArrayEquals(
            new int[] {4},
            RadixSort.sort(new int[] {4})
        );

        assertArrayEquals(
            new double[0],
            BucketSort.sort(new double[0])
        );
    }

    @Test
    void nullArrayRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> BubbleSort.sort(null)
        );
    }
}
