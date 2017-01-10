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

package com.epam.catgenome.manager.gene.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * Source:      StrandSerializable.java
 * Created:     7/12/15, 13:27
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * Represents strand field from GFF/GTF file
 * </p>
 */
public enum StrandSerializable {
    POSITIVE("+"),
    NEGATIVE("-"),
    NONE("!");

    private static Map<String, StrandSerializable> namesMap = new HashMap<>(3);

    static {
        namesMap.put("+", POSITIVE);
        namesMap.put("-", NEGATIVE);
        namesMap.put("!", NONE);
    }

    private String fileValue;

    StrandSerializable(String s) {
        fileValue = s;
    }

    /**
     * Construct StrandSerializable value from string value form gene file.
     * Acceptable strings: "+", "-", "!"
     * @param value String to convert to StrandSerializable object
     * @return a valid StrandSerializable object
     */
    public static StrandSerializable forValue(String value) {
        StrandSerializable retVal = namesMap.get(value.toLowerCase());
        return retVal != null ? retVal : NONE;
    }

    /**
     * Converts StrandSerializable object to string value, equal to gene file value
     * @return a gene file value of StrandSerializable
     */
    public String toValue() {
        for (Map.Entry<String, StrandSerializable> entry : namesMap.entrySet()) {
            if (entry.getValue() == this)  {
                return entry.getKey();
            }
        }
        return null; // or fail
    }

    @Override
    public String toString() {
        return fileValue;
    }

    public String getFileValue() {
        return fileValue;
    }
}
