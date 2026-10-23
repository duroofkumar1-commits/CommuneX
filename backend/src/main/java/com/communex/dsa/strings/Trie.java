
package com.communex.dsa.strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Trie data structure for storing terms and supporting autocomplete.
 *
 * Terms are normalized by trimming whitespace and converting
 * characters to lowercase.
 *
 * Suggestions are returned in alphabetical order.
 * This implementation stores data in memory; a database index
 * should be used when persistent search is required.
 */
public final class Trie {

    private final Node root = new Node();

    /**
     * Inserts a term into the trie.
     *
     * @param term the term to insert
     * @throws IllegalArgumentException if the term is null
     */
    public void insert(String term) {
        if (term == null) {
            throw new IllegalArgumentException(
                    "Term cannot be null"
            );
        }

        String normalized = term.trim().toLowerCase(Locale.ROOT);

        // Ignore empty terms.
        if (normalized.isEmpty()) {
            return;
        }

        Node node = root;

        // Create or reuse a node for each character.
        for (char character : normalized.toCharArray()) {
            node = node.children.computeIfAbsent(
                    character,
                    key -> new Node()
            );
        }

        // Mark this node as the end of a complete word.
        node.word = normalized;
    }

    /**
     * Checks whether the exact term exists in the trie.
     *
     * @param term the term to search for
     * @return true if the term exists, otherwise false
     */
    public boolean contains(String term) {
        Node node = find(term);

        if (node != null && node.word != null) {
            return true;
        }

        return false;
    }

    /**
     * Checks whether any stored term begins with the given prefix.
     *
     * @param prefix the prefix to check
     * @return true if the prefix exists, otherwise false
     */
    public boolean startsWith(String prefix) {
        return find(prefix) != null;
    }

    /**
     * Returns autocomplete suggestions for a given prefix.
     *
     * Suggestions are returned in alphabetical order.
     *
     * @param prefix the prefix to search for
     * @param limit the maximum number of suggestions
     * @return an immutable list of matching terms
     */
    public List<String> suggest(String prefix, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        Node node = find(prefix);

        if (node == null) {
            return List.of();
        }

        List<String> suggestions = new ArrayList<>();

        collect(node, suggestions, limit);

        return List.copyOf(suggestions);
    }

    /**
     * Finds the trie node corresponding to the specified text.
     *
     * @param text the text or prefix to locate
     * @return the matching node, or null if it does not exist
     */
    private Node find(String text) {
        if (text == null) {
            return null;
        }

        String normalized = text.trim().toLowerCase(Locale.ROOT);

        Node node = root;

        for (char character : normalized.toCharArray()) {
            node = node.children.get(character);

            if (node == null) {
                return null;
            }
        }

        return node;
    }

    /**
     * Collects stored words from a node and its descendants.
     *
     * TreeMap keeps child characters ordered, producing
     * alphabetical suggestions.
     *
     * @param node the node from which collection begins
     * @param suggestions the list receiving matching terms
     * @param limit the maximum number of terms to collect
     */
    private void collect(
            Node node,
            List<String> suggestions,
            int limit
    ) {
        if (suggestions.size() >= limit) {
            return;
        }

        // Add the current term if this node marks a complete word.
        if (node.word != null) {
            suggestions.add(node.word);
        }

        // Visit child nodes in alphabetical character order.
        for (Node child : node.children.values()) {
            if (suggestions.size() >= limit) {
                return;
            }

            collect(child, suggestions, limit);
        }
    }

    /**
     * Represents a node in the trie.
     */
    private static final class Node {

        private final Map<Character, Node> children =
                new TreeMap<>();

        private String word;
    }
}
