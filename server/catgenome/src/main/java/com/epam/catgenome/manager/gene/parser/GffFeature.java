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

import com.epam.catgenome.entity.index.GeneIndexEntry;
import com.epam.catgenome.manager.gene.GeneUtils;
import org.thymeleaf.util.MapUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Source:      GffFeature.java
 * Created:     7/12/15, 13:27
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A custom feature class, representing GeneFeature from GFF file
 * </p>
 */
public class GffFeature implements GeneFeature {
    public static final String GENE_FEATURE_NAME = "gene";
    public static final String MRNA_FEATURE_NAME = "mRNA";
    public static final String TRANSCRIPT_FEATURE_NAME = "transcript";
    public static final String EXON_FEATURE_NAME = "exon";

    public static final String GENE_ID_KEY = "gene_id";
    public static final String TRANSCRIPT_ID_KEY = "transcript_id";
    public static final String EXON_ID_KEY = "exon_id";
    public static final String MRNA_ID_KEY = "mrna_id";

    public static final int NON_CODING_EXON_FRAME_VALUE = -1;
    protected String seqName;
    protected String source;
    protected String feature;
    protected int start;
    protected int end;
    protected float score;
    protected StrandSerializable strand;
    protected int frame;
    protected Map<String, String> attributes;

    /**
     * Note that editing this field may be managed only through {@link GffFeature#setGroupId(String)}
     */
    private String groupId;

    private static final String PARENT_KEY = "Parent";
    private static final String ID_KEY = "ID";

    /**
     * Constructs GffFeature object from a GFF file line
     * @param line a line from GFF file
     */
    public GffFeature(String line) {
        String[] tokens = line.split("\t");
        int idx = 0;
        this.seqName = tokens[idx++];
        this.source = tokens[idx++];
        this.feature = tokens[idx++];
        this.start = Integer.parseInt(tokens[idx++]);
        this.end = Integer.parseInt(tokens[idx++]);
        this.score = parseFloatOrDot(tokens[idx++]);
        this.strand = StrandSerializable.forValue(tokens[idx++]);
        this.frame = parseIntOrDot(tokens[idx]);
        this.groupId = parseGroupId(line);
    }

    public GffFeature(final GeneIndexEntry indexEntry) {
        this.seqName = indexEntry.getChromosome().getName();
        this.source = indexEntry.getSource();
        this.feature = indexEntry.getFeature();
        this.start = indexEntry.getStartIndex();
        this.end = indexEntry.getEndIndex();
        this.score = indexEntry.getScore();
        this.strand = StrandSerializable.forValue(indexEntry.getStrand());
        this.frame = indexEntry.getFrame();
        this.attributes = MapUtils.isEmpty(indexEntry.getAttributes()) ? new HashMap<>() : indexEntry.getAttributes();
        this.groupId = getGeneId();
    }

    /**
     * Returns value of gene_id attribute
     * @return value of gene_id attribute
     */
    public String getGeneId() {
        if (attributes == null) {
            return null;
        }

        return attributes.get(GENE_ID_KEY);
    }

    /**
     * Returns value of id attribute
     * @return value of id attribute
     */
    public String getId() {
        return GeneUtils.findAttribute(ID_KEY, attributes);
    }

    /**
     * Returns value of Parent Id attribute
     * @return value of Parent Id attribute
     */
    public String getParentId() {
        return GeneUtils.findAttribute(PARENT_KEY, attributes);
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public String getFeatureId() {
        return GeneUtils.getFeatureId(feature, attributes);
    }

    @Override
    public String getFeatureName() {
        return GeneUtils.findAttribute(GeneUtils.NAME_FIELD, attributes);
    }

    private int parseIntOrDot(String s) {
        if (s.isEmpty()) {
            return -1;
        }

        return s.charAt(0) == '.' ? -1 : Integer.parseInt(s);
    }

    private float parseFloatOrDot(String s) {
        if (s.isEmpty()) {
            return -1;
        }

        return s.charAt(0) == '.' ? -1 : Float.parseFloat(s);
    }

    protected String parseGroupId(String line) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        String[] groups = line.substring(line.lastIndexOf('\t') + 1).split(";");
        for (String group : groups) {
            String[] keyValue = group.split("=");
            if (keyValue.length == 2) {
                attributes.put(decodeAttribute(keyValue[0]), decodeAttribute(keyValue[1]));
            }
        }
        return getGeneId();
    }

    @Override
    public String getSeqName() {
        return seqName;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getFeature() {
        return feature;
    }

    @Override
    public float getScore() {
        return score;
    }

    @Override
    public StrandSerializable getStrand() {
        return strand;
    }

    @Override
    public int getFrame() {
        return frame;
    }

    @Override
    public boolean isCodingExon() {
        return frame == NON_CODING_EXON_FRAME_VALUE;
    }

    @Override
    public String getChr() {
        return getContig();
    }

    @Override
    public String getContig() {
        return seqName;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getEnd() {
        return end;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append(seqName)
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
                .append(strand != null ? strand.getFileValue() :
                        StrandSerializable.NONE.getFileValue())
                .append("\t")
                .append(frame != -1 ? frame : ".")
                .append("\t");

        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            builder.append(attribute.getKey())
                    .append('=')
                    .append(attribute.getValue())
                    .append(';');
        }

        return builder.toString();
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public int hashCode() {
        return 31 * Integer.hashCode(start) + feature.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }

        GffFeature f = (GffFeature) obj;

        if (strand != f.strand) {
            return false;
        }
        if (start != f.start) {
            return false;
        }
        if (end != f.end) {
            return false;
        }
        if (Float.compare(score, f.score) != 0) {
            return false;
        }
        if (frame != f.frame) {
            return false;
        }
        if (!source.equals(f.source)) {
            return false;
        }
        if (!seqName.equals(f.seqName)) {
            return false;
        }
        return feature.equals(f.feature);
    }

    private String decodeAttribute(final String attribute) {
        try {
            return Objects.nonNull(attribute) ? URLDecoder.decode(attribute, StandardCharsets.UTF_8.toString()) : null;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Cannot decode GFF attribute", e);
        }
    }
}
