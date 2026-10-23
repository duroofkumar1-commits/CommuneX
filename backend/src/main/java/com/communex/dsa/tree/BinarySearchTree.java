
package com.communex.dsa.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Unbalanced Binary Search Tree (BST) for unique comparable keys.
 *
 * Values smaller than a node are stored in its left subtree.
 * Values greater than a node are stored in its right subtree.
 * Duplicate values are ignored.
 *
 * @param <T> the type of comparable values stored in the tree
 */
public final class BinarySearchTree<T extends Comparable<T>> {

    private Node<T> root;

    private int size;

    /**
     * Represents a node in the binary search tree.
     *
     * @param <T> the type of value stored in the node
     */
    private static final class Node<T> {

        private T value;

        private Node<T> left;

        private Node<T> right;

        private Node(T value) {
            this.value = value;
        }
    }

    /**
     * Returns the number of unique values in the tree.
     *
     * @return the current tree size
     */
    public int size() {
        return size;
    }

    /**
     * Inserts a value into the tree if it does not already exist.
     *
     * @param value the value to insert
     * @throws IllegalArgumentException if the value is null
     */
    public void add(T value) {
        if (value == null) {
            throw new IllegalArgumentException("Null key");
        }

        // Handle insertion into an empty tree.
        if (root == null) {
            root = new Node<>(value);
            size++;
            return;
        }

        Node<T> current = root;

        while (true) {
            int comparison = value.compareTo(current.value);

            // Ignore duplicate values.
            if (comparison == 0) {
                return;
            }

            if (comparison < 0) {
                // Insert into the left subtree.
                if (current.left == null) {
                    current.left = new Node<>(value);
                    size++;
                    return;
                }

                current = current.left;
            } else {
                // Insert into the right subtree.
                if (current.right == null) {
                    current.right = new Node<>(value);
                    size++;
                    return;
                }

                current = current.right;
            }
        }
    }

    /**
     * Checks whether a value exists in the tree.
     *
     * @param value the value to search for
     * @return true if the value exists, otherwise false
     */
    public boolean contains(T value) {
        Node<T> current = root;

        while (current != null) {
            int comparison = value.compareTo(current.value);

            if (comparison == 0) {
                return true;
            }

            if (comparison < 0) {
                current = current.left;
            } else {
                current = current.right;
            }
        }

        return false;
    }

    /**
     * Returns all stored values in ascending order.
     *
     * In-order traversal visits the left subtree, current node,
     * and then the right subtree.
     *
     * @return an immutable list of sorted values
     */
    public List<T> inOrder() {
        List<T> values = new ArrayList<>();

        walk(root, values);

        return List.copyOf(values);
    }

    /**
     * Performs a recursive in-order traversal.
     *
     * @param node the current subtree root
     * @param values the list collecting traversal results
     */
    private void walk(Node<T> node, List<T> values) {
        if (node == null) {
            return;
        }

        // Visit the left subtree.
        walk(node.left, values);

        // Visit the current node.
        values.add(node.value);

        // Visit the right subtree.
        walk(node.right, values);
    }
}
