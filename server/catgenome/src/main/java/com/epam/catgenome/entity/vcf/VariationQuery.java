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

package com.epam.catgenome.entity.vcf;

/**
 * {@code VariationQuery} represents a plain VO used to handle query parameters
 * that describe a single variation.
 */
public class VariationQuery {

    /**
     * {@code Long} ID of a VCF track in the system.
     */
    private Long id;

    /**
     * {@code Long} ID of a sample that's available for a VCF track. By default the first
     * sample on a track is used.
     */
    private Long sampleId;

    /**
     * {@code Integer} specifies a position of a variation in chromosome coordinates.
     */
    private Integer position;

    /**
     * {@code Long}  ID of a chromosome in the system.
     */
    private Long chromosomeId;

    /**
     * A optional field to load genes, affected by variation
     */
    private Long projectId;  // TODO: remove

    public final Long getId() {
        return id;
    }

    public final void setId(final Long id) {
        this.id = id;
    }

    public final Long getSampleId() {
        return sampleId;
    }

    public final void setSampleId(final Long sampleId) {
        this.sampleId = sampleId;
    }

    public final Integer getPosition() {
        return position;
    }

    public final void setPosition(final Integer position) {
        this.position = position;
    }

    public final Long getChromosomeId() {
        return chromosomeId;
    }

    public final void setChromosomeId(final Long chromosomeId) {
        this.chromosomeId = chromosomeId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
}
