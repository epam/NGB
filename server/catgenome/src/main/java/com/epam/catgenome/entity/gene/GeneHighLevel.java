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

package com.epam.catgenome.entity.gene;

import java.util.List;
import java.util.Map;

import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.manager.gene.parser.GeneFeature;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;

/**
 * Created: 3/21/2016
 * Project: CATGenome Browser
 *
 * <p>
 * Value objects, used for representing the highest level of gene hierarchy
 * </p>
 */
public class GeneHighLevel  extends Block {
    private String seqName;
    private String source;
    private String feature;
    private Float score;
    private StrandSerializable strand;
    private Integer frame;
    private String groupId;
    private Map<String, String> attributes;
    private Boolean mapped;

    private List<GeneLowLevel> items;

    public GeneHighLevel() {
        // No-op
    }

    /**
     * Create GeneHighLevel from {@code GeneFeature} object
     */
    public GeneHighLevel(GeneFeature geneFeature) {
        setStartIndex(geneFeature.getStart());
        setEndIndex(geneFeature.getEnd());

        seqName = geneFeature.getSeqName();
        source = geneFeature.getSource();
        feature = geneFeature.getFeature();
        score = geneFeature.getScore();
        strand = geneFeature.getStrand();
        frame = geneFeature.getFrame();
        groupId = geneFeature.getGroupId();

        attributes = geneFeature.getAttributes();
    }

    /**
     * Create GeneHighLevel from {@code Gene} object
     */
    public GeneHighLevel(Gene gene) {
        setStartIndex(gene.getStartIndex());
        setEndIndex(gene.getEndIndex());

        seqName = gene.getSeqName();
        source = gene.getSource();
        feature = gene.getFeature();
        score = gene.getScore();
        strand = gene.getStrand();
        frame = gene.getFrame();
        groupId = gene.getGroupId();
        mapped = gene.isMapped();

        attributes = gene.getAttributes();
    }

    public String getSeqName() {
        return seqName;
    }

    public void setSeqName(String seqName) {
        this.seqName = seqName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public StrandSerializable getStrand() {
        return strand;
    }

    public void setStrand(StrandSerializable strand) {
        this.strand = strand;
    }

    public Integer getFrame() {
        return frame;
    }

    public void setFrame(Integer frame) {
        this.frame = frame;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public List<GeneLowLevel> getItems() {
        return items;
    }

    public void setItems(List<GeneLowLevel> items) {
        this.items = items;
    }

    public Boolean getMapped() {
        return mapped;
    }

    public void setMapped(Boolean mapped) {
        this.mapped = mapped;
    }
}
