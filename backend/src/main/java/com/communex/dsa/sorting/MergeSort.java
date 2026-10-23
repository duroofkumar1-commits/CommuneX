package com.communex.dsa.sorting;

/** Stable O(n log n) merge sort with O(n) auxiliary storage. */
public final class MergeSort {
    private MergeSort() { }
    public static int[] sort(int[] input) {
        int[] values = BubbleSort.copy(input);
        if (values.length > 1) mergeSort(values, new int[values.length], 0, values.length - 1);
        return values;
    }
    private static void mergeSort(int[] a, int[] temp, int left, int right) {
        if (left >= right) return;
        int mid = left + (right - left) / 2;
        mergeSort(a, temp, left, mid); mergeSort(a, temp, mid + 1, right);
        if (a[mid] <= a[mid + 1]) return;
        int i = left, j = mid + 1, k = left;
        while (i <= mid && j <= right) temp[k++] = a[i] <= a[j] ? a[i++] : a[j++];
        while (i <= mid) temp[k++] = a[i++];
        while (j <= right) temp[k++] = a[j++];
        System.arraycopy(temp, left, a, left, right - left + 1);
    }
}
