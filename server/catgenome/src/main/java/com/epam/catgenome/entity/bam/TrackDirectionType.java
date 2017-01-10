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

package com.epam.catgenome.entity.bam;


import java.util.HashMap;
import java.util.Map;

/**
 * Source:      TrackDirectionType.java
 * Created:     7/5/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 * Represent a Track direction, three directions are supported: LEFT, MIDDLE, RIGHT. Direction is
 * used for reads filtering. Filtering is done according to these rules:
 * {@code LEFT} = reads within track interval and not intersecting the right border are shown
 * {@code RIGHT} = reads within track interval and not intersecting the left border are shown
 * {@code MIDDLE} = all reads within track interval
 */
public enum TrackDirectionType {
    LEFT(1),
    MIDDLE(2),
    RIGHT(3);

    private long id;

    private static Map<Long, TrackDirectionType> idMap = new HashMap<>(TrackDirectionType.values().length);

    static {
        idMap.put(LEFT.getId(), LEFT);
        idMap.put(MIDDLE.getId(), MIDDLE);
        idMap.put(RIGHT.getId(), RIGHT);
    }

    TrackDirectionType(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public static TrackDirectionType getById(Long id) {
        if (id == null) {
            return null;
        }
        return idMap.get(id);
    }

}
