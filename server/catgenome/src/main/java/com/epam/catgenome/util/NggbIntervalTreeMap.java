/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import htsjdk.samtools.util.Interval;
import htsjdk.samtools.util.IntervalTree;
import htsjdk.samtools.util.IntervalTreeMap;

/**
 * Source:      MyIntervalTreeMap
 * Created:     15.09.16, 12:09
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * A custom version of Samtools {@code IntervalTreeMap}, containing minimum start index and maximum end index of all
 * it's features.
 * </p>
 */
public class NggbIntervalTreeMap<T> extends IntervalTreeMap<T> {
    private IntervalTreeMap<T> treeMap;
    private int minStartIndex = 0;
    private int maxEndIndex = 0;

    public NggbIntervalTreeMap() {
        super();
        this.treeMap = new IntervalTreeMap<>();
    }

    public NggbIntervalTreeMap(Map<? extends Interval, ? extends T> map) {
        treeMap = new IntervalTreeMap<>(map);
    }

    @Override
    public IntervalTree<T> debugGetTree(String sequence) {
        return treeMap.debugGetTree(sequence);
    }

    @Override
    public void clear() {
        treeMap.clear();
    }

    @Override
    public boolean containsKey(Object object) {
        return treeMap.containsKey(object);
    }

    @Override
    public boolean containsKey(Interval key) {
        return treeMap.containsKey(key);
    }

    @Override
    public Set<Entry<Interval, T>> entrySet() {
        return treeMap.entrySet();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        NggbIntervalTreeMap<?> that = (NggbIntervalTreeMap<?>) o;

        return !(minStartIndex != that.minStartIndex || maxEndIndex != that.maxEndIndex) && ((
                treeMap != null) ? treeMap.equals(that.treeMap) : (that.treeMap == null));

    }

    @Override
    public int hashCode() {
        return treeMap.hashCode();
    }

    @Override
    public T get(Object object) {
        return treeMap.get(object);
    }

    @Override
    public T get(Interval key) {
        return treeMap.get(key);
    }

    @Override
    public boolean isEmpty() {
        return treeMap.isEmpty();
    }

    @Override
    public T put(Interval key, T value) {
        return treeMap.put(key, value);
    }

    @Override
    public T remove(Object object) {
        return treeMap.remove(object);
    }

    @Override
    public T remove(Interval key) {
        return treeMap.remove(key);
    }

    @Override
    public int size() {
        return treeMap.size();
    }

    @Override
    public boolean containsOverlapping(Interval key) {
        return treeMap.containsOverlapping(key);
    }

    @Override
    public Collection<T> getOverlapping(Interval key) {
        return treeMap.getOverlapping(key);
    }

    @Override
    public boolean containsContained(Interval key) {
        return treeMap.containsContained(key);
    }

    @Override
    public Collection<T> getContained(Interval key) {
        return treeMap.getContained(key);
    }

    public IntervalTreeMap<T> getTreeMap() {
        return treeMap;
    }

    /**
     * @return minimum start index of features containing
     */
    public int getMinStartIndex() {
        return minStartIndex;
    }

    public void setMinStartIndex(int min) {
        this.minStartIndex = min;
    }

    /**
     * @return maximum end index of features containing
     */
    public int getMaxEndIndex() {
        return maxEndIndex;
    }

    public void setMaxEndIndex(int max) {
        this.maxEndIndex = max;
    }
}
