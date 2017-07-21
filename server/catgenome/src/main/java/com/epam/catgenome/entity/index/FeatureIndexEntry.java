/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

import java.io.Serializable;
import java.util.UUID;

import com.epam.catgenome.entity.reference.Chromosome;


/**
 * Source:      FeatureIndexEntry
 * Created:     10.02.16, 14:16
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * Represents a feature in a feature index
 * </p>
 */
public class FeatureIndexEntry implements Serializable {

    /**
     * {@code Integer} represents the ending interval index, inclusive.
     */
    protected Integer endIndex;

    /**
     * {@code Integer} represents the beginning interval index, inclusive.
     */
    protected Integer startIndex;

    protected String featureId;
    protected Chromosome chromosome;
    protected FeatureType featureType;
    protected Long featureFileId;
    protected String featureName;

    private UUID uuid;

    public FeatureIndexEntry() {
        // no-op
    }

    public FeatureIndexEntry(FeatureIndexEntry origin) {
        chromosome = origin.getChromosome();
        featureId = origin.getFeatureId();
        featureType = origin.getFeatureType();
        startIndex = origin.getStartIndex();
        endIndex = origin.getEndIndex();
        featureName = origin.getFeatureName();
        uuid = origin.getUuid();
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public Integer getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(Integer endIndex) {
        this.endIndex = endIndex;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public Chromosome getChromosome() {
        return chromosome;
    }

    public void setChromosome(Chromosome chromosome) {
        this.chromosome = chromosome;
    }

    public FeatureType getFeatureType() {
        return featureType;
    }

    public void setFeatureType(FeatureType featureType) {
        this.featureType = featureType;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() ==  this.getClass() &&
                featureId.equals(((FeatureIndexEntry) obj).getFeatureId())
                && startIndex.equals(((FeatureIndexEntry) obj).getStartIndex())
                && endIndex.equals(((FeatureIndexEntry) obj).getEndIndex())
                && (featureType == null || featureType.equals(((FeatureIndexEntry) obj).getFeatureType()));
    }

    @Override
    public int hashCode() {
        return featureId.hashCode() + startIndex.hashCode() + endIndex.hashCode();
    }

    public Long getFeatureFileId() {
        return featureFileId;
    }

    public void setFeatureFileId(Long featureFileId) {
        this.featureFileId = featureFileId;
    }
}
