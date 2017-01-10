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
 * Represents variations classification of the mutations, supported by the MAF format
 */
public enum VariantClassification {
    FRAME_SHIFT_DEL("Frame_Shift_Del"),
    FRAME_SHIFT_INS("Frame_Shift_Ins"),
    IN_FRAME_DEL("In_Frame_Del"),
    IN_FRAME_INS("In_Frame_Ins"),
    MISSENSE_MUTATION("Missense_Mutation"),
    NONSENSE_MUTATION("Nonsense_Mutation"),
    SILENT("Silent"),
    SPLICE_SITE("Splice_Site"),
    TRANSLATION_START_SITE("Translation_Start_Site"),
    NONSTOP_MUTATION("Nonstop_Mutation"),
    UTR_3("3'UTR"),
    FLANK_3("3'Flank"),
    UTR_5("5'UTR"),
    FLANK_5("5'Flank"),
    IGR("IGR"),
    INTRON("Intron"),
    RNA("RNA"),
    TARGETED_REGION("Targeted_Region");

    private static Map<String, VariantClassification> valueMap = new HashMap<>();

    static {
        valueMap.put(FRAME_SHIFT_DEL.getFileValue(), FRAME_SHIFT_DEL);
        valueMap.put(FRAME_SHIFT_INS.getFileValue(), FRAME_SHIFT_INS);
        valueMap.put(IN_FRAME_DEL.getFileValue(), IN_FRAME_DEL);
        valueMap.put(IN_FRAME_INS.getFileValue(), IN_FRAME_INS);
        valueMap.put(MISSENSE_MUTATION.getFileValue(), MISSENSE_MUTATION);
        valueMap.put(NONSENSE_MUTATION.getFileValue(), NONSENSE_MUTATION);
        valueMap.put(SILENT.getFileValue(), SILENT);
        valueMap.put(SPLICE_SITE.getFileValue(), SPLICE_SITE);
        valueMap.put(TRANSLATION_START_SITE.getFileValue(), TRANSLATION_START_SITE);
        valueMap.put(NONSTOP_MUTATION.getFileValue(), NONSTOP_MUTATION);
        valueMap.put(UTR_3.getFileValue(), UTR_3);
        valueMap.put(FLANK_3.getFileValue(), FLANK_3);
        valueMap.put(UTR_5.getFileValue(), UTR_5);
        valueMap.put(FLANK_5.getFileValue(), FLANK_5);
        valueMap.put(IGR.getFileValue(), IGR);
        valueMap.put(INTRON.getFileValue(), INTRON);
        valueMap.put(RNA.getFileValue(), RNA);
        valueMap.put(TARGETED_REGION.getFileValue(), TARGETED_REGION);
    }

    private String fileValue;

    /**
     * Creates a {@code VariantClassification} from a {@code String} from a MAF file
     * @param value from MAF file
     */
    VariantClassification(String value) {
        this.fileValue = value;
    }

    /**
     * @param fileValue from MAF file
     * @return corresponding to {@code String} value {@code VariantClassification} instance
     */
    public static VariantClassification forValue(String fileValue) {
        return valueMap.get(fileValue);
    }

    public String getFileValue() {
        return fileValue;
    }

}
