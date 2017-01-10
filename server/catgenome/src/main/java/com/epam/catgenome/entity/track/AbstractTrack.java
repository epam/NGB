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

package com.epam.catgenome.entity.track;

import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.reference.Chromosome;

/**
 * Source:      AbstractTrack
 * Created:     14.06.16, 16:56
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * Represents basic fields of Track entity
 * </p>
 */
public abstract class AbstractTrack extends BaseEntity {
    /**
     * {@code TrackType} specifies the type of current track.
     */
    protected TrackType type;

    /**
     * {@code Integer} represents an index of nucleotide inside chromosome. This index should
     * be considered in any current data retrieving operation as the end position
     */
    protected Integer endIndex;

    /**
     * {@code Integer} represents an index of nucleotide inside chromosome. This index should
     * be considered in any current data retrieving operation as the start
     */
    protected Integer startIndex;

    /**
     * {@code Float} specifies the scale factor that should be applied to a track blocks.
     */
    protected Double scaleFactor;

    /**
     * {@code Chromosome} represents a chromosome selected at the moment. Any data sets
     * of all tracks are aligned to it.
     * +
     */
    protected Chromosome chromosome;

    public TrackType getType() {
        return type;
    }

    public final void setType(TrackType type) {
        this.type = type;
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

    public Double getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(Double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public Chromosome getChromosome() {
        return chromosome;
    }

    public void setChromosome(Chromosome chromosome) {
        this.chromosome = chromosome;
    }
}
