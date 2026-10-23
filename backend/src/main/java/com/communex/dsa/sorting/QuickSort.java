package com.communex.dsa.sorting;

/** In-place three-way quicksort on a defensive copy; average O(n log n). */
public final class QuickSort {
    private QuickSort() { }
    public static int[] sort(int[] input) {
        int[] values = BubbleSort.copy(input); sort(values, 0, values.length - 1); return values;
    }
    private static void sort(int[] a, int low, int high) {
        if (low >= high) return;
        int pivot = a[low + (high - low) / 2]; int less = low, scan = low, greater = high;
        while (scan <= greater) {
            if (a[scan] < pivot) swap(a, less++, scan++);
            else if (a[scan] > pivot) swap(a, scan, greater--);
            else scan++;
        }
        sort(a, low, less - 1); sort(a, greater + 1, high);
    }
    private static void swap(int[] a, int i, int j) {
         int t = a[i]; a[i] = a[j]; a[j] = t;
        }
}
