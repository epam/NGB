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
import java.util.Objects;

import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.manager.gene.parser.GeneFeature;
import com.epam.catgenome.manager.gene.parser.GffFeature;
import com.epam.catgenome.manager.gene.parser.GtfFeature;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Source:      Gene
 * Created:     05.12.15, 15:16
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * A block VO, describing gene feature
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
public class Gene extends Block {
    public enum Origin {
        GFF, GTF
    }

    private String seqName;
    private String source;
    private String feature;
    private Float score;
    private StrandSerializable strand;
    private Integer frame;
    private String groupId;
    /**
     * For GFF genes assembly only
     */
    private String parentId;
    private Origin origin;
    @JsonIgnore
    private String gffId;

    private Map<String, String> attributes;
    private Long exonsCount;
    private Long aminoacidLength;
    private List<Gene> items;
    private List<Transcript> transcripts;
    private boolean mapped = true;
    private boolean canonical = false;
    private String featureName;
    private String featureId;

    private int featureCount = 1;
    private String uid;

    /**
     * Create Gene as a copy of other Gene
     * @param gene Gene to copy
     */
    public Gene(final Gene gene) {
        super(gene);
        seqName = gene.getSeqName();
        source = gene.getSource();
        feature = gene.getFeature();
        score = gene.getScore();
        strand = gene.getStrand();
        frame = gene.getFrame();
        groupId = gene.getGroupId();
        parentId = gene.getParentId();
        origin = gene.getOrigin();
        gffId = gene.getGffId();
        attributes = gene.getAttributes();
        exonsCount = gene.getExonsCount();
        aminoacidLength = gene.getAminoacidLength();
        items = gene.getItems();
        transcripts = gene.getTranscripts();
        featureId = gene.getFeatureId();
        featureName = gene.getFeatureName();
        uid = gene.getUid();
    }

    /**
     * Convert Gene from {@code GeneFeature} object
     */
    public Gene(final GeneFeature geneFeature) {
        setStartIndex(geneFeature.getStart());
        setEndIndex(geneFeature.getEnd());

        seqName = geneFeature.getSeqName();
        source = geneFeature.getSource();
        feature = geneFeature.getFeature();
        score = geneFeature.getScore();
        strand = geneFeature.getStrand();
        frame = geneFeature.getFrame();
        groupId = geneFeature.getGroupId();
        uid = geneFeature.getUid();

        if (geneFeature instanceof GtfFeature) {
            parentId = groupId;
            origin = Origin.GTF;
        } else {
            GffFeature gffFeature = (GffFeature) geneFeature;
            parentId = gffFeature.getParentId();
            origin = Origin.GFF;
            gffId = gffFeature.getId();
        }

        attributes = geneFeature.getAttributes();

        featureName = geneFeature.getFeatureName();
        featureId = geneFeature.getFeatureId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(feature, getStartIndex(), getEndIndex(), attributes.hashCode());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (this.getClass() == obj.getClass()) {
            Gene otherGene = (Gene) obj;
            return Objects.equals(otherGene.getStartIndex(), this.getStartIndex()) &&
                    Objects.equals(otherGene.getEndIndex(), this.getEndIndex()) &&
                    Objects.equals(otherGene.getFeature(), this.getFeature()) &&
                    Objects.equals(otherGene.getGroupId(), this.getGroupId());
        } else {
            return false;
        }
    }
}
