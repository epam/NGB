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

package com.epam.catgenome.entity.vcf;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration {@code VariationType} represents supported Variation types
 */
public enum VariationType {
    SNV,
    INS,
    DEL,
    MNP,
    MIXED,

    /**
     * On smaller scale, just defines that a variation is present
     */
    STATISTIC,

    /**
     * Determines a structural variation - a long deletion
     */
    STRUCT_DEL,

    /**
     * Determines a structural variation - a long insertion
     */
    STRUCT_INS,

    /**
     * Determines a structural variation - a duplication
     */
    DUP,

    /**
     * Determines a structural variation - an inversion
     */
    INV,

    /**
     * Determines a structural variation - a bind
     */
    BND,

    // MAF variation types
    DNP,
    TNP,
    ONP,
    CONSOLIDATED,

    /**
     * Determines a variation with a name, not matching all the previous ones
     */
    UNK;

    private static final String CONSOLIDATED_NAME = "Consolidated";
    private static Map<String, VariationType> mafValueMap = new HashMap<>();
    private static Map<VariationType, String> mafFileValueMap = new EnumMap<>(VariationType.class);
    static {
        mafValueMap.put("SNP", SNV);
        mafValueMap.put("DNP", DNP);
        mafValueMap.put("TNP", TNP);
        mafValueMap.put("ONP", ONP);
        mafValueMap.put(INS.name(), INS);
        mafValueMap.put(DEL.name(), DEL);
        mafValueMap.put(CONSOLIDATED_NAME, CONSOLIDATED);
    }

    static {
        mafFileValueMap.put(SNV, "SNP");
        mafFileValueMap.put(DNP, DNP.name());
        mafFileValueMap.put(TNP, TNP.name());
        mafFileValueMap.put(ONP, ONP.name());
        mafFileValueMap.put(INS, INS.name());
        mafFileValueMap.put(DEL, DEL.name());
        mafFileValueMap.put(CONSOLIDATED, CONSOLIDATED_NAME);
    }

    public static VariationType forMafValue(String mafFileValue) {
        return mafValueMap.get(mafFileValue);
    }

    public String toMafFileValue() {
        return mafFileValueMap.get(this);
    }
}
