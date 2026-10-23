package com.communex.dsa.sorting;

/** Stable O(n²) insertion sort; effective for small or nearly sorted arrays. */
public final class InsertionSort {
    private InsertionSort() { }
    public static int[] sort(int[] input) {
        int[] values = BubbleSort.copy(input);
        for (int i = 1; i < values.length; i++) {
            int value = values[i], j = i - 1;
            while (j >= 0 && values[j] > value) {
                values[j + 1] = values[j]; j--;
            }
            values[j + 1] = value;
        }
        return values;
    }
}
