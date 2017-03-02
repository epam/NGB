package com.epam.catgenome.util;


import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * A custom {@link Collector} class, that collects objects into a {@link Map} of {@link List}s
 */
public class ListMapCollector<V, K> implements Collector<V, Map<K, List<V>>, Map<K, List<V>>> {
    private Function<V, K> keyFunction;

    public ListMapCollector(Function<V, K> keyFunction) {
        this.keyFunction = keyFunction;
    }

    @Override
    public Supplier<Map<K, List<V>>> supplier() {
        return HashMap::new;
    }

    @Override
    public BiConsumer<Map<K, List<V>>, V> accumulator() {
        return (longListMap, value) -> {
            K key = keyFunction.apply(value);
            if (!longListMap.containsKey(key)) {
                longListMap.put(key, new ArrayList<>());
            }

            longListMap.get(key).add(value);
        };
    }

    @Override
    public BinaryOperator<Map<K, List<V>>> combiner() {
        return (longListMap, longListMap2) -> {
            for (Map.Entry<K, List<V>> e : longListMap2.entrySet()) {
                if (longListMap.containsKey(e.getKey())) {
                    longListMap.get(e.getKey()).addAll(e.getValue());
                } else {
                    longListMap.put(e.getKey(), e.getValue());
                }
            }

            return longListMap;
        };
    }

    @Override
    public Function<Map<K, List<V>>, Map<K, List<V>>> finisher() {
        return longListMap -> longListMap;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH);
    }
}
