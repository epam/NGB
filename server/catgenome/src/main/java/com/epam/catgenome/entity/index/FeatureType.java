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

package com.epam.catgenome.entity.index;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Source:      FeatureType
 * Created:     17.02.16, 12:57
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * Enum, representing type of feature index entry
 * </p>
 */
public enum FeatureType {
    /**
     * A gene feature from GFF/GTF file
     */
    GENE("gene"),

    /**
     * A transcript/mrna feature from GFF/GTF file
     */
    MRNA("mrna"),

    /**
     * A rrna feature from GFF/GTF file
     */
    RRNA("rrna"),

    /**
     * A cds feature from GFF/GTF file
     */
    CDS("cds"),

    /**
     * An exon feature from GFF/GTF file
     */
    EXON("exon"),

    /**
     * A 5utr feature from GFF/GTF file
     */
    UTR5("5utr"),

    /**
     * A 3utr feature from GFF/GTF file
     */
    UTR3("3utr"),

    /**
     * A utr feature from GFF/GTF file
     */
    UTR("utr"),

    /**
     * A ncrna feature from GFF/GTF file
     */
    NCRNA("ncrna"),

    /**
     * A tmrna feature from GFF/GTF file
     */
    TMRNA("tmrna"),

    /**
     * A trna feature from GFF/GTF file
     */
    TRNA("trna"),

    /**
     * A operon feature from GFF/GTF file
     */
    OPERON("operon"),

    /**
     * A region feature from GFF/GTF file
     */
    REGION("region"),

    /**
     * A regulatory feature from GFF/GTF file
     */
    REGULATORY("regulatory"),

    /**
     * A generic feature from GFF/GTF file
     */
    GENERIC_GENE_FEATURE("generic_gene"),

    /**
     * A start_codon feature from GFF/GTF file
     */
    START_CODON("start_codon"),

    /**
     * A stop_codon feature from GFF/GTF file
     */
    STOP_CODON("stop_codon"),

    /**
     * A variation from VCF file
     */
    VARIATION("vcf"),

    /**
     * A project's bookmark
     */
    BOOKMARK("bookmark"),

    /**
     * A gene feature from GFF/GTF file
     */
    BED_FEATURE("bed_feature");

    public static final int NUMBER_OF_TYPES = 12;
    private String fileValue;
    private static Map<String, FeatureType> namesMap = new HashMap<>(NUMBER_OF_TYPES);

    static {
        namesMap.put(GENE.getFileValue().toUpperCase(), GENE);
        namesMap.put(MRNA.getFileValue().toUpperCase(), MRNA);
        namesMap.put(CDS.getFileValue().toUpperCase(), CDS);
        namesMap.put(EXON.getFileValue().toUpperCase(), EXON);
        namesMap.put(UTR5.getFileValue().toUpperCase(), UTR5);
        namesMap.put(UTR3.getFileValue().toUpperCase(), UTR3);
        namesMap.put(UTR.getFileValue().toUpperCase(), UTR);
        namesMap.put(NCRNA.getFileValue().toUpperCase(), NCRNA);
        namesMap.put(TMRNA.getFileValue().toUpperCase(), TMRNA);
        namesMap.put(TRNA.getFileValue().toUpperCase(), TRNA);
        namesMap.put(OPERON.getFileValue().toUpperCase(), OPERON);
        namesMap.put(REGION.getFileValue().toUpperCase(), REGION);
        namesMap.put(REGULATORY.getFileValue().toUpperCase(), REGULATORY);
        namesMap.put(START_CODON.getFileValue().toUpperCase(), START_CODON);
        namesMap.put(STOP_CODON.getFileValue().toUpperCase(), STOP_CODON);
        namesMap.put(VARIATION.getFileValue().toUpperCase(), VARIATION);
        namesMap.put(BOOKMARK.getFileValue().toUpperCase(), BOOKMARK);
        namesMap.put(BED_FEATURE.getFileValue().toUpperCase(), BED_FEATURE);
    }

    FeatureType(String s) {
        fileValue = s;
    }

    /**
     * Returns FeatureType instance for specified value from index
     * @param indexValue feature type value from index
     * @return FeatureType instance
     */
    public static FeatureType forValue(String indexValue) {
        final FeatureType featureType = namesMap.get(indexValue.toUpperCase());
        return featureType != null ? featureType : GENERIC_GENE_FEATURE;
    }

    public String getFileValue() {
        return StringUtils.upperCase(fileValue);
    }
}
