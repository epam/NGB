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

import java.util.Map;

import htsjdk.tribble.Feature;

/**
 * Source:      GeneFeature.java
 * Created:     7/12/15, 13:27
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * A custom feature interface, representing GeneFeature to parse GFF/GTF files
 * </p>
 *
 *
 *
 */
public interface GeneFeature extends Feature {

    /**
     * Returns Gene feature's sequence name. Equals sequence name field from gene file.
     * @return Gene feature's sequence name
     */
    String getSeqName();

    /**
     * Returns Gene feature's source. Equals source field from gene file.
     * @return Gene feature's source
     */
    String getSource();

    /**
     * Returns Gene feature's feature type. Equals feature field from gene file.
     * @return Gene feature's feature type
     */
    String getFeature();

    /**
     * Returns Gene feature's score. Equals score field from gene file.
     * @return Gene feature's score
     */
    float getScore();

    /**
     * Returns Gene feature's strand. Equals strand field from gene file.
     * @return Gene feature's strand
     */
    StrandSerializable getStrand();

    /**
     * Returns Gene feature's frame. Equals frame field from gene file.
     * @return Gene feature's frame
     */
    int getFrame();

    /**
     * Checks if gene feature is a coding exon. Based on gene file's frame field
     * @return {@code true} if feature is a coding exon
     */
    boolean isCodingExon();

    /**
     * Returns Gene feature's group ID. Equals feature's attribute geneId from gene file
     * @return feature's group ID
     */
    String getGroupId();

    @Override
    String getContig();

    /**
     * Returns Gene feature's attributes, parsed from gene file
     * @return Gene feature's attributes
     */
    Map<String, String> getAttributes();

    /**
     * Returns feature ID. Returns gene_id, transcript_id or exon_id attributes, depending on feature type
     * @return feature ID
     */
    String getFeatureId();

    /**
     * Returns feature name. Returns the value of gene_name or transcript_name attribute from GTF files or Name
     * attribute from GFF files
     * @return Returns feature name
     */
    String getFeatureName();
}
