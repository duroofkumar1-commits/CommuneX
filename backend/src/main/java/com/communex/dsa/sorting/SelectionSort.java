package com.communex.dsa.sorting;

/** O(n²) selection sort with constant auxiliary space beyond the returned copy. */
public final class SelectionSort {
    private SelectionSort() { }
    public static int[] sort(int[] input) {
        int[] values = BubbleSort.copy(input);
        for (int i = 0; i < values.length; i++) {
            int min = i;
            for (int j = i + 1; j < values.length; j++) if (values[j] < values[min]) min = j;
            int temp = values[i]; values[i] = values[min]; values[min] = temp;
        }
        return values;
    }
}
