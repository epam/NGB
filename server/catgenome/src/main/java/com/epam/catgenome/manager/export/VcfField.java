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
package com.epam.catgenome.manager.export;

import com.epam.catgenome.entity.index.VcfIndexEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public enum VcfField {
    VARIATION_TYPE("variationType", o -> String.valueOf(o.getVariationType())),
    GENE("gene", VcfIndexEntry::getGene),
    GENE_IDS("geneIds", VcfIndexEntry::getGeneIds),
    GENE_NAME("geneName", VcfIndexEntry::getGeneName),
    GENE_NAMES("geneNames", VcfIndexEntry::getGeneNames),
    FAILED_FILTER("failedFilter", VcfIndexEntry::getFailedFilter),
    IMPACT("impact", o -> String.valueOf(o.getImpact())),
    EFFECT("effect", o -> String.valueOf(o.getEffect())),
    QUALITY("quality", o -> String.valueOf(o.getQuality())),
    IS_EXON("isExon", o -> String.valueOf(o.getExon())),
    START_INDEX("startIndex", o -> String.valueOf(o.getStartIndex())),
    END_INDEX("endIndex", o -> String.valueOf(o.getEndIndex())),
    FEATURE_ID("featureId", o -> String.valueOf(o.getFeatureId())),
    CHROMOSOME("chromosome", o -> o.getChromosome().getName()),
    FEATURE_TYPE("featureType", o -> o.getFeatureType().getFileValue()),
    FEATURE_FILE_ID("featureFileId", o -> String.valueOf(o.getFeatureFileId())),
    FEATURE_NAME("featureName", VcfIndexEntry::getFeatureName);

    private final String field;
    private final Function<VcfIndexEntry, String> getter;
    private static final Map<String, VcfField> MAP = new HashMap<>();

    static {
        MAP.put(VARIATION_TYPE.field, VARIATION_TYPE);
        MAP.put(GENE.field, GENE);
        MAP.put(GENE_IDS.field, GENE_IDS);
        MAP.put(GENE_NAME.field, START_INDEX);
        MAP.put(FAILED_FILTER.field, FAILED_FILTER);
        MAP.put(IMPACT.field, IMPACT);
        MAP.put(EFFECT.field, EFFECT);
        MAP.put(QUALITY.field, QUALITY);
        MAP.put(IS_EXON.field, IS_EXON);
        MAP.put(START_INDEX.field, START_INDEX);
        MAP.put(END_INDEX.field, END_INDEX);
        MAP.put(FEATURE_ID.field, FEATURE_ID);
        MAP.put(CHROMOSOME.field, CHROMOSOME);
        MAP.put(FEATURE_TYPE.field, FEATURE_TYPE);
        MAP.put(FEATURE_FILE_ID.field, FEATURE_FILE_ID);
        MAP.put(FEATURE_NAME.field, FEATURE_NAME);
    }

    VcfField(final String field, final Function<VcfIndexEntry, String> getter) {
        this.field = field;
        this.getter = getter;
    }

    public static VcfField getByField(String field) {
        return MAP.get(field);
    }

    public Function<VcfIndexEntry, String> getGetter() {
        return getter;
    }
}
