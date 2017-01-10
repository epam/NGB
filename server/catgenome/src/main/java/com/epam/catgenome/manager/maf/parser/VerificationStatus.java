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

package com.epam.catgenome.manager.maf.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents verification statuses of the mutations, supported by the MAF format
 */
public enum VerificationStatus {
    VERIFIED("Verified"),
    UNKNOWN("Unknown");

    private String fileValue;
    private static Map<String, VerificationStatus> valueMap = new HashMap<>();
    static {
        valueMap.put(VERIFIED.getFileValue(), VERIFIED);
        valueMap.put(UNKNOWN.getFileValue(), UNKNOWN);
    }

    /**
     * Creates a {@code VerificationStatus} from a {@code String} from a MAF file
     * @param fileValue from MAF file
     */
    VerificationStatus(String fileValue) {
        this.fileValue = fileValue;
    }

    /**
     * @param fileValue from MAF file
     * @return corresponding to {@code String} value {@code VerificationStatus} instance
     */
    public static VerificationStatus forValue(String fileValue) {
        return valueMap.get(fileValue);
    }

    public String getFileValue() {
        return fileValue;
    }
}
