
package com.communex.dsa.tree;

/**
 * Self-balancing AVL tree set for unique comparable keys.
 *
 * An AVL tree maintains its height balance after insertions
 * using rotations, keeping search and insertion efficient.
 *
 * @param <T> the type of keys stored in the tree
 */
public final class AVLTree<T extends Comparable<T>> {

    private Node<T> root;

    private int size;

    /**
     * Represents a node in the AVL tree.
     *
     * @param <T> the type of value stored in the node
     */
    private static final class Node<T> {

        private T value;

        private Node<T> left;

        private Node<T> right;

        private int height = 1;

        private Node(T value) {
            this.value = value;
        }
    }

    /**
     * Returns the number of unique keys in the tree.
     *
     * @return the current number of keys
     */
    public int size() {
        return size;
    }

    /**
     * Checks whether the specified value exists in the tree.
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
     * Adds a value to the tree if it does not already exist.
     *
     * The tree is rebalanced after insertion when necessary.
     *
     * @param value the value to insert
     * @throws IllegalArgumentException if the value is null
     */
    public void add(T value) {
        if (value == null) {
            throw new IllegalArgumentException("Null key");
        }

        root = insert(root, value);
    }

    /**
     * Inserts a value into the appropriate subtree and
     * restores the AVL balance property.
     *
     * @param node the current subtree root
     * @param value the value to insert
     * @return the updated subtree root
     */
    private Node<T> insert(Node<T> node, T value) {
        if (node == null) {
            size++;
            return new Node<>(value);
        }

        int comparison = value.compareTo(node.value);

        if (comparison < 0) {
            node.left = insert(node.left, value);
        } else if (comparison > 0) {
            node.right = insert(node.right, value);
        } else {
            // Duplicate values are not inserted.
            return node;
        }

        // Update the height after modifying a subtree.
        update(node);

        int balanceFactor = balance(node);

        // Left-Left case.
        if (balanceFactor > 1
                && value.compareTo(node.left.value) < 0) {
            return rotateRight(node);
        }

        // Right-Right case.
        if (balanceFactor < -1
                && value.compareTo(node.right.value) > 0) {
            return rotateLeft(node);
        }

        // Left-Right case.
        if (balanceFactor > 1
                && value.compareTo(node.left.value) > 0) {
            node.left = rotateLeft(node.left);
            return rotateRight(node);
        }

        // Right-Left case.
        if (balanceFactor < -1
                && value.compareTo(node.right.value) < 0) {
            node.right = rotateRight(node.right);
            return rotateLeft(node);
        }

        return node;
    }

    /**
     * Returns the height of a node.
     *
     * An empty subtree has height zero.
     *
     * @param node the node to inspect
     * @return the node height, or zero if null
     */
    private int height(Node<T> node) {
        if (node == null) {
            return 0;
        }

        return node.height;
    }

    /**
     * Recalculates the height of a node using its children.
     *
     * @param node the node to update
     */
    private void update(Node<T> node) {
        node.height = 1 + Math.max(
                height(node.left),
                height(node.right)
        );
    }

    /**
     * Calculates the AVL balance factor of a node.
     *
     * @param node the node to inspect
     * @return left subtree height minus right subtree height
     */
    private int balance(Node<T> node) {
        return height(node.left) - height(node.right);
    }

    /**
     * Performs a right rotation.
     *
     * @param node the root of the unbalanced subtree
     * @return the new subtree root
     */
    private Node<T> rotateRight(Node<T> node) {
        Node<T> newRoot = node.left;
        Node<T> transferredSubtree = newRoot.right;

        newRoot.right = node;
        node.left = transferredSubtree;

        // Update heights from the lower node upward.
        update(node);
        update(newRoot);

        return newRoot;
    }

    /**
     * Performs a left rotation.
     *
     * @param node the root of the unbalanced subtree
     * @return the new subtree root
     */
    private Node<T> rotateLeft(Node<T> node) {
        Node<T> newRoot = node.right;
        Node<T> transferredSubtree = newRoot.left;

        newRoot.left = node;
        node.right = transferredSubtree;

        // Update heights from the lower node upward.
        update(node);
        update(newRoot);

        return newRoot;
    }
}
