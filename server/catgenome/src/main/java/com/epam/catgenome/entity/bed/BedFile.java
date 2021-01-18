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

package com.epam.catgenome.entity.bed;

import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.security.AclClass;
import com.epam.catgenome.manager.bed.parser.NggbBedCodec;
import com.epam.catgenome.manager.bed.parser.NggbBedFeature;
import com.epam.catgenome.manager.bed.parser.NggbMultiFormatBedCodec;
import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.Feature;

import java.util.*;

/**
 * Represents a Bed File metadata in the system
 */
public class BedFile extends FeatureFile {

    /**
     * Creates an empty {@code BedFile} record with a required type BED
     */
    public BedFile() {
        setFormat(BiologicalDataItemFormat.BED);
    }

    @Override
    public AclClass getAclClass() {
        return  AclClass.BED;
    }

    @Override
    public AsciiFeatureCodec<NggbBedFeature> getCodec() {
        BedFile.FileExtension extension = BedFile.FileExtension.byExtension(getExtension());
        if (extension == BedFile.FileExtension.BED) {
            return new NggbBedCodec();
        }
        return new NggbMultiFormatBedCodec(extension.getMapping());
    }

    public enum FileExtension {
        BED(
                new HashMap<Integer, String>() {{
                }},
                "bed"
        ),
        NARROW_PEAK(
                new HashMap<Integer, String>() {{
                    put(4, "name");
                    put(5, "score");
                    put(6, "strand");
                    put(7, "signalValue");
                    put(8, "pValue");
                    put(9, "qValue");
                    put(10, "peak");
                }},
                "narrowPeak", "nPk"
        ),
        BROAD_PEAK(
                new HashMap<Integer, String>() {{
                    put(4, "name");
                    put(5, "score");
                    put(6, "strand");
                    put(7, "signalValue");
                    put(8, "pValue");
                    put(9, "qValue");
                    put(10, "peak");
                }},
                "broadPeak", "bPk"
        ),
        RNA_ELEMENTS(
                new HashMap<Integer, String>() {{
                    put(4, "name");
                    put(5, "score");
                    put(6, "strand");
                    put(7, "level");
                    put(8, "signif");
                    put(9, "score2");
                }},
                "RNAelements", "RNAe"
        ),
        GAPPED_PEAK(
                new HashMap<Integer, String>() {{
                    put(4, "name");
                    put(5, "score");
                    put(6, "strand");
                    put(7, "thickStart");
                    put(8, "thickEnd");
                    put(9, "itemRgb");
                    put(10, "blockCount");
                    put(11, "blockSizes");
                    put(12, "blockStarts");
                    put(13, "signalValue");
                    put(14, "pValue");
                    put(15, "qValue");
                }},
                "gappedPeak", "gPk"
        ),
        TAG_ALIGN(
                new HashMap<Integer, String>() {{
                    put(4, "sequence");
                    put(5, "score");
                    put(6, "strand");
                }},
                "tagAlign", "tAlgn"
        ),
        PAIRED_TAG_ALIGN(
                new HashMap<Integer, String>() {{
                    put(4, "sequence");
                    put(5, "score");
                    put(6, "strand");
                    put(7, "seq1");
                    put(8, "seq2");
                }},
                "pairedTagAlign", "ptAlgn"
        ),
        PEPTIDE_MAPPING(
                new HashMap<Integer, String>() {{
                    put(4, "sequence");
                    put(5, "score");
                    put(6, "strand");
                    put(7, "rawScore");
                    put(8, "spectrumId");
                    put(9, "peptideRank");
                    put(10, "peptideRepeatCount");
                }},
                "peptideMapping", "pMap"
        );

        private Map<Integer, String> mapping;
        private List<String> values;

        FileExtension(final Map<Integer, String> mapping, final String... values) {
            this.mapping = mapping;
            this.values = Arrays.asList(values.clone());
        }

        public static FileExtension byExtension(final String value) {
            return Arrays.stream(FileExtension.values())
                    .findFirst()
                    .filter(ext -> ext.values.contains(value))
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Wrong extension %s", value)));
        }

        public Map<Integer, String> getMapping() {
            return mapping;
        }
    }
}
