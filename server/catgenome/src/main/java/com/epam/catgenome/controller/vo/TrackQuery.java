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

package com.epam.catgenome.controller.vo;

import com.epam.catgenome.entity.bam.BamQueryOption;

/**
 * Source:      TrackQuery
 * Created:     10/20/2015
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A value object, designed to pass parameters for track query requests through REST controllers
 * </p>
 */
public class TrackQuery {

    /**
     * {@code Long} ID of a track in the system
     */
    private Long id;

    /**
     * {@code Long}  ID of a chromosome in the system
     */
    private Long chromosomeId;

    /**
     * {@code Integer} represents an index of nucleotide inside chromosome. This index should
     * be considered in any current data retrieving operation as the start position
     */
    private Integer startIndex;

    /**
     * {@code Integer} represents an index of nucleotide inside chromosome. This index should
     * be considered in any current data retrieving operation as the end position
     */
    private Integer endIndex;

    /**
     * {@code Float} specifies the scale factor that should be applied to a track blocks.
     */
    private Double scaleFactor;

    private BamQueryOption option;

    private Boolean collapsed;

    public BamQueryOption getOption() {
        return option;
    }

    public void setOption(BamQueryOption option) {
        this.option = option;
    }

    public final Long getId() {
        return id;
    }

    public final void setId(final Long id) {
        this.id = id;
    }

    public final Long getChromosomeId() {
        return chromosomeId;
    }

    public final void setChromosomeId(final Long chromosomeId) {
        this.chromosomeId = chromosomeId;
    }

    public final Integer getStartIndex() {
        return startIndex;
    }

    public final void setStartIndex(final Integer startIndex) {
        this.startIndex = startIndex;
    }

    public final Integer getEndIndex() {
        return endIndex;
    }

    public final void setEndIndex(final Integer endIndex) {
        this.endIndex = endIndex;
    }

    public final Double getScaleFactor() {
        return scaleFactor;
    }

    public final void setScaleFactor(final Double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public Boolean getCollapsed() {
        return collapsed;
    }

    public void setCollapsed(Boolean collapsed) {
        this.collapsed = collapsed;
    }
}
