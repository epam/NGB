package com.epam.catgenome.util.feature.reader.index;

/*
 * Copyright (c) 2007-2010 by The Broad Institute, Inc. and the Massachusetts Institute of Technology.
 * All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL), Version 2.1 which
 * is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR WARRANTIES OF
 * ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING, WITHOUT LIMITATION, WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT
 * OR OTHER DEFECTS, WHETHER OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR
 * RESPECTIVE TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES OF
 * ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES, ECONOMIC
 * DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER THE BROAD OR MIT SHALL
 * BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT SHALL KNOW OF THE POSSIBILITY OF THE
 * FOREGOING.
 */

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of an interval tree, following the explanation.
 * from CLR. For efficiently finding all intervals which overlap a given
 * interval or point.
 * <p/>
 * References:
 * http://en.wikipedia.org/wiki/Interval_tree
 * <p/>
 * Cormen, Thomas H.; Leiserson, Charles E., Rivest, Ronald L. (1990). Introduction to Algorithms (1st ed.). MIT Press and McGraw-Hill. ISBN 0-262-03141-8
 */
public class IntervalTree {

    Node root;
    Node nil = Node.nil;

    /**
     * See {@link #getSize()}
     */
    int treeSize;

    public IntervalTree() {
        this.root = nil;
        this.treeSize = 0;
    }


    public void insert(Interval interval) {
        Node node = new Node(interval);
        insert(node);
        treeSize++;
    }

    /**
     * The estimated size of the tree. We keep a running count
     * on each insert, this getter returns that count.
     *
     * @return
     * @see #size()
     */
    public int getSize() {
        return treeSize;
    }

    /**
     * @param interval
     * @return all matches as a list of Intervals
     */
    public List<Interval> findOverlapping(Interval interval) {

        if (getRoot().isNull()) {
            return Collections.emptyList();
        }

        List<Interval> results = new ArrayList<Interval>();
        searchAll(interval, getRoot(), results);
        return results;
    }

    public String toString() {
        return getRoot().toString();
    }

    private List<Interval> searchAll(Interval interval, Node node, List<Interval> results) {
        if (node.interval.overlaps(interval)) {
            results.add(node.interval);
        }
        if (!node.left.isNull() && node.left.max >= interval.start) {
            searchAll(interval, node.left, results);
        }
        if (!node.right.isNull() && node.right.min <= interval.end) {
            searchAll(interval, node.right, results);
        }
        return results;
    }

    /**
     * Return all intervals in tree.
     * TODO: an iterator would be more effecient.
     *
     * @return
     */
    public List<Interval> getIntervals() {
        if (getRoot().isNull()) {
            return Collections.emptyList();
        }
        List<Interval> results = new ArrayList<Interval>(treeSize);
        getAll(getRoot(), results);
        return results;
    }

    /**
     * Get all nodes which are descendants of {@code node}, inclusive.
     * {@code results} is modified in place
     *
     * @param node
     * @param results
     * @return the total list of descendants, including original {@code results}
     */
    private List<Interval> getAll(Node node, List<Interval> results) {

        results.add(node.interval);
        if (!node.left.isNull()) {
            getAll(node.left, results);
        }
        if (!node.right.isNull()) {
            getAll(node.right, results);
        }
        return results;
    }


    /**
     * Used for testing only.
     *
     * @param node
     * @return
     */
    private int getRealMax(Node node) {
        if (node.isNull()) {
            return Integer.MIN_VALUE;
        }
        int leftMax = getRealMax(node.left);
        int rightMax = getRealMax(node.right);
        int nodeHigh = (node.interval).end;

        int max1 = (leftMax > rightMax ? leftMax : rightMax);
        return (max1 > nodeHigh ? max1 : nodeHigh);
    }

    /**
     * Used for testing only
     *
     * @param node
     * @return
     */
    private int getRealMin(Node node) {
        if (node.isNull()) {
            return Integer.MAX_VALUE;
        }
        int leftMin = getRealMin(node.left);
        int rightMin = getRealMin(node.right);
        int nodeLow = (node.interval).start;

        int min1 = (leftMin < rightMin ? leftMin : rightMin);
        return (min1 < nodeLow ? min1 : nodeLow);
    }


    private void insert(Node x) {
        assert (x != null);
        assert (!x.isNull());

        treeInsert(x);
        x.color = Node.red;
        while (x != this.root && x.parent.color == Node.red) {
            if (x.parent == x.parent.parent.left) {
                Node y = x.parent.parent.right;
                if (y.color == Node.red) {
                    x.parent.color = Node.black;
                    y.color = Node.black;
                    x.parent.parent.color = Node.red;
                    x = x.parent.parent;
                } else {
                    if (x == x.parent.right) {
                        x = x.parent;
                        this.leftRotate(x);
                    }
                    x.parent.color = Node.black;
                    x.parent.parent.color = Node.red;
                    this.rightRotate(x.parent.parent);
                }
            } else {
                Node y = x.parent.parent.left;
                if (y.color == Node.red) {
                    x.parent.color = Node.black;
                    y.color = Node.black;
                    x.parent.parent.color = Node.red;
                    x = x.parent.parent;
                } else {
                    if (x == x.parent.left) {
                        x = x.parent;
                        this.rightRotate(x);
                    }
                    x.parent.color = Node.black;
                    x.parent.parent.color = Node.red;
                    this.leftRotate(x.parent.parent);
                }
            }
        }
        this.root.color = Node.black;
    }


    private Node getRoot() {
        return this.root;
    }


    private void leftRotate(Node x) {
        Node y = x.right;
        x.right = y.left;
        if (y.left != nil) {
            y.left.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == nil) {
            this.root = y;
        } else {
            if (x.parent.left == x) {
                x.parent.left = y;
            } else {
                x.parent.right = y;
            }
        }
        y.left = x;
        x.parent = y;

        applyUpdate(x);
        // no need to apply update on y, since it'll y is an ancestor
        // of x, and will be touched by applyUpdate().
    }


    private void rightRotate(Node x) {
        Node y = x.left;
        x.left = y.right;
        if (y.right != nil) {
            y.right.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == nil) {
            this.root = y;
        } else {
            if (x.parent.right == x) {
                x.parent.right = y;
            } else {
                x.parent.left = y;
            }
        }
        y.right = x;
        x.parent = y;


        applyUpdate(x);
        // no need to apply update on y, since it'll y is an ancestor
        // of x, and will be touched by applyUpdate().
    }


    /**
     * Note:  Does not maintain RB constraints,  this is done post insert
     *
     * @param x
     */
    private void treeInsert(Node x) {
        Node node = this.root;
        Node y = nil;
        while (node != nil) {
            y = node;
            if (x.interval.start <= node.interval.start) {
                node = node.left;
            } else {
                node = node.right;
            }
        }
        x.parent = y;

        if (y == nil) {
            this.root = x;
            x.left = x.right = nil;
        } else {
            if (x.interval.start <= y.interval.start) {
                y.left = x;
            } else {
                y.right = x;
            }
        }

        this.applyUpdate(x);
    }


    // Applies the statistic update on the node and its ancestors.

    private void applyUpdate(Node node) {
        while (!node.isNull()) {
            this.update(node);
            node = node.parent;
        }
    }

    private void update(Node node) {
        node.max = Math.max(Math.max(node.left.max, node.right.max), node.interval.end);
        node.min = Math.min(Math.min(node.left.min, node.right.min), node.interval.start);
    }

    /**
     * @return Returns the number of nodes in the tree.
     * Recalculated each call
     * @see #getSize()
     */
    public int size() {
        return intervalTreeSize(this.root);
    }


    private int intervalTreeSize(Node node) {
        if (node.isNull()) {
            return 0;
        }
        return 1 + intervalTreeSize(node.left) + intervalTreeSize(node.right);
    }


    private boolean allRedNodesFollowConstraints(Node node) {
        if (node.isNull()) {
            return true;
        }

        if (node.color == Node.black) {
            return (allRedNodesFollowConstraints(node.left) &&
                    allRedNodesFollowConstraints(node.right));
        }

        // At this point, we know we're on a red node.
        return (node.left.color == Node.black &&
                node.right.color == Node.black &&
                allRedNodesFollowConstraints(node.left) &&
                allRedNodesFollowConstraints(node.right));
    }


    // Check that both ends are equally balanced in terms of black height.

    private boolean isBalancedBlackHeight(Node node) {
        if (node.isNull()) {
            return true;
        }
        return (blackHeight(node.left) == blackHeight(node.right) &&
                isBalancedBlackHeight(node.left) &&
                isBalancedBlackHeight(node.right));
    }


    // The black height of a node should be left/right equal.

    private int blackHeight(Node node) {
        if (node.isNull()) {
            return 0;
        }
        int leftBlackHeight = blackHeight(node.left);
        if (node.color == Node.black) {
            return leftBlackHeight + 1;
        } else {
            return leftBlackHeight;
        }
    }


    /**
     * Test code: make sure that the tree has all the properties
     * defined by Red Black trees and interval trees
     * <p/>
     * o.  Root is black.
     * <p/>
     * o.  nil is black.
     * <p/>
     * o.  Red nodes have black children.
     * <p/>
     * o.  Every path from root to leaves contains the same number of
     * black nodes.
     * <p/>
     * o.  getMax(node) is the maximum of any interval rooted at that node..
     * <p/>
     * This code is expensive, and only meant to be used for
     * assertions and testing.
     */
    public boolean isValid() {
        if (this.root.color != Node.black) {
            //logger.warn("root color is wrong");
            return false;
        }
        if (nil.color != Node.black) {
            //logger.warn("nil color is wrong");
            return false;
        }
        if (allRedNodesFollowConstraints(this.root) == false) {
            //logger.warn("red node doesn't follow constraints");
            return false;
        }
        if (isBalancedBlackHeight(this.root) == false) {
            //logger.warn("black height unbalanced");
            return false;
        }

        return hasCorrectMaxFields(this.root) &&
                hasCorrectMinFields(this.root);
    }


    private boolean hasCorrectMaxFields(Node node) {
        if (node.isNull()) {
            return true;
        }
        return (getRealMax(node) == (node.max) &&
                hasCorrectMaxFields(node.left) &&
                hasCorrectMaxFields(node.right));
    }


    private boolean hasCorrectMinFields(Node node) {
        if (node.isNull()) {
            return true;
        }
        return (getRealMin(node) == (node.min) &&
                hasCorrectMinFields(node.left) &&
                hasCorrectMinFields(node.right));
    }


    static class Node {

        public static boolean black = false;
        public static boolean red = true;

        Interval interval;
        int min;
        int max;
        Node left;
        Node right;

        // Color and parent are used for inserts.  If tree is immutable these are not required (no requirement
        // to store these persistently).
        boolean color;
        Node parent;


        private Node() {
            this.max = Integer.MIN_VALUE;
            this.min = Integer.MAX_VALUE;
        }

        public void store(DataOutputStream dos) throws IOException {
            dos.writeInt(interval.start);
            dos.writeInt(interval.end);
            dos.writeInt(min);
            dos.writeInt(max);

        }

        public Node(Interval interval) {
            this();
            this.parent = nil;
            this.left = nil;
            this.right = nil;
            this.interval = interval;
            this.color = red;
        }


        static Node nil;

        static {
            nil = new Node();
            nil.color = black;
            nil.parent = nil;
            nil.left = nil;
            nil.right = nil;
        }


        public boolean isNull() {
            return this == nil;
        }


        public String toString() {

            // Make some shorthand for the nodes
            Map<Interval, Integer> keys = new LinkedHashMap<Interval, Integer>();

            if (this == nil) {
                return "nil";
            }

            StringBuffer buf = new StringBuffer();
            toStringIntervalTree(buf, keys);

            buf.append('\n');
            for (Map.Entry<Interval, Integer> entry : keys.entrySet()) {
                buf.append(entry.getValue()).append(" = ").append(entry.getKey());
                buf.append('\n');
            }

            return buf.toString();
        }

        public void toStringIntervalTree(StringBuffer buf, Map<Interval, Integer> keys) {
            if (this == nil) {
                buf.append("nil\n");
                return;
            }

            Integer selfKey = keys.get(this.interval);
            if (selfKey == null) {
                selfKey = keys.size();
                keys.put(this.interval, selfKey);
            }
            Integer leftKey = keys.get(this.left.interval);
            if (leftKey == null) {
                leftKey = keys.size();
                keys.put(this.left.interval, leftKey);
            }
            Integer rightKey = keys.get(this.right.interval);
            if (rightKey == null) {
                rightKey = keys.size();
                keys.put(this.right.interval, rightKey);
            }


            buf.append(selfKey).append(" -> ").append(leftKey).append(" , ").append(rightKey);
            buf.append('\n');
            this.left.toStringIntervalTree(buf, keys);
            this.right.toStringIntervalTree(buf, keys);
        }
    }
}

