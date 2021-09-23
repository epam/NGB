/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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
package com.epam.catgenome.entity.heatmap;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum HeatmapDataType {
    INTEGER(0, "Integer"),
    DOUBLE(1, "Double"),
    STRING(2, "String");

    private final int id;
    private final String name;
    private static final Map<Integer, HeatmapDataType> ID_MAP = new HashMap<>();
    private static final Map<String, HeatmapDataType> NAME_MAP = new HashMap<>();

    static {
        ID_MAP.put(0, INTEGER);
        ID_MAP.put(1, DOUBLE);
        ID_MAP.put(2, STRING);
        NAME_MAP.put("Integer", INTEGER);
        NAME_MAP.put("Double", DOUBLE);
        NAME_MAP.put("String", STRING);
    }

    public static HeatmapDataType getById(int id) {
        return ID_MAP.get(id);
    }
    public static HeatmapDataType getByName(String name) {
        return NAME_MAP.get(name);
    }
}
