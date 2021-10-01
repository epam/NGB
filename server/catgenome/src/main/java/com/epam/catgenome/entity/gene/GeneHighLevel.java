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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created: 3/21/2016
 * Project: CATGenome Browser
 *
 * <p>
 * Value objects, used for representing the highest level of gene hierarchy
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
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
    private String uid;

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
        uid = geneFeature.getUid();
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
        uid = gene.getUid();
    }
}
