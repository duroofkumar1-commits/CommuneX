package com.communex.dsa.sorting;

/** Educational O(n²) adjacent-swap sort; useful for teaching, not large production lists. */
public final class BubbleSort {
    private BubbleSort() { }
    public static int[] sort(int[] input) {
        int[] values = copy(input);
        for (int end = values.length - 1; end > 0; end--) {
            boolean swapped = false;
            for (int i = 0; i < end; i++) {
                if (values[i] > values[i + 1]) {
                    int temp = values[i]; values[i] = values[i + 1]; values[i + 1] = temp; swapped = true;
                }
            }
            if (!swapped) break;
        }
        return values;
    }
    static int[] copy(int[] input) { if (input == null) throw new IllegalArgumentException("Input cannot be null"); return input.clone(); }
}
