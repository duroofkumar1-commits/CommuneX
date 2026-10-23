
package com.communex.dsa.strings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Knuth-Morris-Pratt (KMP) substring search algorithm.
 *
 * Searches for a pattern in a text using a prefix table
 * to avoid unnecessary character comparisons.
 *
 * Time complexity: O(n + m), where n is the text length
 * and m is the pattern length.
 */
public final class KmpSearch {

    // Prevent object creation.
    private KmpSearch() {
    }

    /**
     * Finds the first occurrence of a pattern in the text.
     *
     * @param text the text to search
     * @param pattern the substring to find
     * @return the first matching index, or -1 if not found
     * @throws IllegalArgumentException if text or pattern is null
     */
    public static int indexOf(String text, String pattern) {
        if (text == null || pattern == null) {
            throw new IllegalArgumentException(
                    "Text and pattern are required"
            );
        }

        // An empty pattern matches at index zero.
        if (pattern.isEmpty()) {
            return 0;
        }

        int[] lps = prefixTable(pattern);

        int textIndex = 0;
        int patternIndex = 0;

        while (textIndex < text.length()) {
            if (text.charAt(textIndex)
                    == pattern.charAt(patternIndex)) {

                textIndex++;
                patternIndex++;

                // The complete pattern has been matched.
                if (patternIndex == pattern.length()) {
                    return textIndex - patternIndex;
                }
            } else if (patternIndex > 0) {
                // Use the prefix table to avoid restarting the search.
                patternIndex = lps[patternIndex - 1];
            } else {
                // No prefix can be reused; move to the next text character.
                textIndex++;
            }
        }

        return -1;
    }

    /**
     * Finds all occurrences of a pattern in the text.
     *
     * Overlapping matches are included.
     *
     * @param text the text to search
     * @param pattern the substring to find
     * @return an immutable list of matching start indexes
     * @throws IllegalArgumentException if text or pattern is null
     */
    public static List<Integer> allIndexes(
            String text,
            String pattern
    ) {
        if (text == null || pattern == null) {
            throw new IllegalArgumentException(
                    "Text and pattern are required"
            );
        }

        // An empty pattern matches at every position.
        if (pattern.isEmpty()) {
            return IntStream.rangeClosed(0, text.length())
                    .boxed()
                    .toList();
        }

        int[] lps = prefixTable(pattern);
        List<Integer> indexes = new ArrayList<>();

        int textIndex = 0;
        int patternIndex = 0;

        while (textIndex < text.length()) {
            if (text.charAt(textIndex)
                    == pattern.charAt(patternIndex)) {

                textIndex++;
                patternIndex++;

                // Record a match and continue to find overlapping matches.
                if (patternIndex == pattern.length()) {
                    indexes.add(textIndex - patternIndex);

                    patternIndex = lps[patternIndex - 1];
                }
            } else if (patternIndex > 0) {
                // Reuse the longest matching prefix.
                patternIndex = lps[patternIndex - 1];
            } else {
                textIndex++;
            }
        }

        return List.copyOf(indexes);
    }

    /**
     * Builds the Longest Proper Prefix which is also a Suffix (LPS) table.
     *
     * Each entry stores the length of the longest proper prefix
     * that is also a suffix for the pattern ending at that index.
     *
     * @param pattern the pattern used to build the table
     * @return the computed prefix table
     */
    private static int[] prefixTable(String pattern) {
        int[] lps = new int[pattern.length()];

        int index = 1;
        int prefixLength = 0;

        while (index < pattern.length()) {
            if (pattern.charAt(index)
                    == pattern.charAt(prefixLength)) {

                prefixLength++;
                lps[index] = prefixLength;
                index++;
            } else if (prefixLength > 0) {
                prefixLength = lps[prefixLength - 1];
            } else {
                lps[index] = 0;
                index++;
            }
        }

        return lps;
    }
}
