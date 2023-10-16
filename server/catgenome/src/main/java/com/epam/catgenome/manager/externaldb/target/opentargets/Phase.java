/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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
package com.epam.catgenome.manager.externaldb.target.opentargets;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum Phase {
    PHASE_1_EARLY(0, "Phase I (Early)"),
    PHASE_1(1, "Phase I"),
    PHASE_2(2, "Phase II"),
    PHASE_3(3, "Phase III"),
    PHASE_4(4, "Phase IV"),
    NOT_AVAILABLE(5, "");

    private static final Map<Integer, Phase> ID_MAP = new HashMap<>();
    static {
        ID_MAP.put(0, PHASE_1_EARLY);
        ID_MAP.put(1, PHASE_1);
        ID_MAP.put(2, PHASE_2);
        ID_MAP.put(3, PHASE_3);
        ID_MAP.put(4, PHASE_4);
    }

    public static String getPhase(String value) {
        int val = (int) Float.parseFloat(value);
        return ID_MAP.getOrDefault(val, NOT_AVAILABLE).getLabel();
    }
    private final Integer value;
    private final String label;
}
