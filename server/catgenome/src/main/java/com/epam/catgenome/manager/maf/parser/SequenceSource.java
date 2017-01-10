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
 * Represents sequence sources of the mutations, supported by the MAF format
 */
public enum SequenceSource {
    WGS("WGS"),
    WGA("WGA"),
    WXS("WXS"),
    RNA_SEQ("RNA-Seq"),
    MIRNA_SEQ("miRNA-Seq"),
    BISULFITE_SEQ("Bisulfite-Seq"),
    VALIDATION("VALIDATION"),
    OTHER("Other"),
    NCRNA_SEQ("ncRNA-Seq"),
    WCS("WCS"),
    CLONE("CLONE"),
    POOLCLONE("POOLCLONE"),
    AMPLICON("AMPLICON"),
    CLONEEND("CLONEEND"),
    FINISHING("FINISHING"),
    CHIP_SEQ("ChIP-Seq"),
    MNASE_SEQ("MNase-Seq"),
    DNASE_HYPERSENSIVITY("DNase-Hypersensitivity"),
    EST("EST"),
    FL_CDNA("FL-cDNA"),
    CTS("CTS"),
    MRE_SEQ("MRE-Seq"),
    MEDIP_SEQ("MeDIP-Seq"),
    MBD_SEQ("MBD-Seq"),
    TN_SEQ("Tn-Seq"),
    FAIRE_SEQ("FAIRE-seq"),
    SELEX("SELEX"),
    RIP_SEQ("RIP-Seq"),
    CHIA_PET("ChIA-PET");

    private String fileValue;
    private static Map<String, SequenceSource> valueMap = new HashMap<>();
    static {
        for (SequenceSource sequenceSource : SequenceSource.values()) {
            valueMap.put(sequenceSource.getFileValue(), sequenceSource);
        }
    }

    /**
     * Creates a {@code SequenceSource} from a {@code String} from a MAF file
     * @param fileValue from MAF file
     */
    SequenceSource(String fileValue) {
        this.fileValue = fileValue;
    }

    /**
     * @param fileValue from MAF file
     * @return corresponding to {@code String} value {@code SequenceSource} instance
     */
    public static SequenceSource forValue(String fileValue) {
        return valueMap.get(fileValue);
    }

    public String getFileValue() {
        return fileValue;
    }
}
