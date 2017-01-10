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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Source:      GtfFeature.java
 * Created:     7/12/15, 13:27
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A custom feature class, representing GeneFeature from GTF file
 * </p>
 */
public class GtfFeature extends GffFeature {
    private static final String GENE_NAME_KEY = "gene_name";
    private static final String GENE_SYMBOL_KEY = "gene_symbol";
    private static final String MRNA_NAME_KEY = "mRNA_name";
    private static final String MRNA_SYMBOL_KEY = "mRNA_symbol";
    private static final String TRANSCRIPT_NAME_KEY = "transcript_name";
    private static final String TRANSCRIPT_SYMBOL_KEY = "transcript_symbol";

    private static final Pattern PATTERN = Pattern.compile("([^;]*) (\")?([^;]*)\\2;([^\\t]|$)");

    public GtfFeature(String line) {
        super(line);
    }

    public String getTransriptId() {
        return attributes.get(TRANSCRIPT_ID_KEY);
    }

    public String getGroupValue(String attributeKey) {
        return attributes.get(attributeKey);
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    protected String parseGroupId(String line) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        Matcher matcher = PATTERN.matcher(line.substring(line.lastIndexOf('\t') + 1));
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(3);
            attributes.put(key, value);
        }
        return getGeneId();
    }

    @Override
    public String toString() {
        StringBuilder builder =  new StringBuilder().append(seqName)
                .append("\t")
                .append(source)
                .append("\t")
                .append(feature)
                .append("\t")
                .append(start)
                .append("\t")
                .append(end)
                .append("\t")
                .append(Float.compare(score, -1F) != 0 ? score : ".")
                .append("\t")
                .append(strand.getFileValue())
                .append("\t")
                .append(frame != -1 ? frame : ".")
                .append("\t");
        for (Map.Entry<String, String> attribute: attributes.entrySet()) {
            builder.append(attribute.getKey())
                    .append(" \"")
                    .append(attribute.getValue())
                    .append("\"; ");
        }

        return builder.toString();
    }

    @Override
    public String getFeatureName() {
        if (GENE_FEATURE_NAME.equalsIgnoreCase(this.feature)) {
            if (attributes.containsKey(GENE_NAME_KEY)) {
                return attributes.get(GENE_NAME_KEY);
            }
            return attributes.get(GENE_SYMBOL_KEY);
        }

        String transcriptName = getTranscriptName();
        if (transcriptName != null) {
            return transcriptName;
        }

        if (EXON_FEATURE_NAME.equalsIgnoreCase(this.feature)) {
            return getFeatureId();
        }
        return null;
    }

    private String getTranscriptName() {
        if (TRANSCRIPT_FEATURE_NAME.equalsIgnoreCase(this.feature) ||
            MRNA_FEATURE_NAME.equalsIgnoreCase(this.feature)) {
            if (attributes.containsKey(MRNA_NAME_KEY)) {
                return attributes.get(MRNA_NAME_KEY);
            }
            if (attributes.containsKey(TRANSCRIPT_NAME_KEY)) {
                return attributes.get(TRANSCRIPT_NAME_KEY);
            }
            if (attributes.containsKey(TRANSCRIPT_SYMBOL_KEY)) {
                return attributes.get(TRANSCRIPT_SYMBOL_KEY);
            }
            if (attributes.containsKey(MRNA_SYMBOL_KEY)) {
                return attributes.get(MRNA_SYMBOL_KEY);
            }
        }

        return null;
    }
}
