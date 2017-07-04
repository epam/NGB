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

package com.epam.catgenome.manager.parallel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

/**
 * Source:      MyTreeMultiset
 * Created:     13.09.16, 12:16
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
public class TreeListMultiset<K, V> implements Iterable<V>, Collection<V> {
    private Function<V, K> keyFunction;
    private TreeMap<K, List<V>> listTreeMap;

    public TreeListMultiset(Function<V, K> keyFunction) {
        listTreeMap = new TreeMap<>();
        this.keyFunction = keyFunction;
    }

    public int count(V element) {
        K key = keyFunction.apply(element);
        if (listTreeMap.containsKey(key)) {
            return listTreeMap.get(key).size();
        }
        return 0;
    }

    @Override
    public Iterator<V> iterator() {
        return new MyTreeMultisetIterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        ArrayList<V> arrayList = new ArrayList<>();
        listTreeMap.values().stream().forEach(arrayList::addAll);
        return arrayList.toArray();
    }

    @NotNull
    @Override
    public Object[] toArray(Object[] a) {
        ArrayList<V> arrayList = new ArrayList<>();
        listTreeMap.values().stream().forEach(arrayList::addAll);
        return arrayList.toArray();
    }

    @Override
    public int size() {
        return listTreeMap.values().stream().collect(Collectors.summingInt(List::size));
    }

    @Override
    public boolean isEmpty() {
        return listTreeMap.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return listTreeMap.containsKey(keyFunction.apply((V) o));
    }

    @Override
    public boolean add(V element) {
        K key = keyFunction.apply(element);
        boolean contains = listTreeMap.containsKey(key);

        if (!contains) {
            listTreeMap.put(key, new ArrayList<>());
        }

        listTreeMap.get(key).add(element);
        return contains;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return !c.stream().anyMatch(o -> !contains(c));
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        return c.stream().map(this::add).allMatch(r -> r);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {
        listTreeMap.clear();
    }

    public V floor(V element) {
        K floorKey = listTreeMap.floorKey(keyFunction.apply(element));
        if (floorKey != null) {
            List<V> list = listTreeMap.get(floorKey);
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }

        return null;
    }

    public class MyTreeMultisetIterator implements Iterator<V> {
        private Iterator<K> keyIterator = listTreeMap.keySet().iterator();
        private Iterator<V> valueIterator;

        @Override
        public boolean hasNext() {
            return keyIterator.hasNext() || (valueIterator != null && valueIterator.hasNext());
        }

        @Override
        public V next() {
            if (valueIterator == null || !valueIterator.hasNext()) {
                if (hasNext()) {
                    valueIterator = listTreeMap.get(keyIterator.next()).iterator();
                } else {
                    return null;
                }
            }
            return valueIterator.next();
        }
    }
}
