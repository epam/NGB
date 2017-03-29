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

package com.epam.catgenome.entity.bam;

import java.util.List;

import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.track.TrackType;
import com.epam.catgenome.entity.wig.Wig;

/**
 * Source:
 * Created:     2/29/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * Represents the track with BAM data
 */
public class BamTrack<T extends Block> extends Track<T> {

    private String referenceBuffer;

    private Integer minPosition;

    private List<Wig> downsampleCoverage;

    private List<SpliceJunctionsEntity> spliceJunctions;

    private List<BaseCoverage> baseCoverage;

    private List<Wig> regions;
    /**
     * Default constructor creates empty track with BAM type
     */
    public BamTrack() {
        super.setType(TrackType.BAM);
    }

    /**
     * Creates BAM track form an input track
     * @param track
     */
    public BamTrack(final Track track) {
        super(track);
        super.setType(TrackType.BAM);
    }

    public String getReferenceBuffer() {
        return referenceBuffer;
    }

    public void setReferenceBuffer(String referenceBuffer) {
        this.referenceBuffer = referenceBuffer;
    }

    public List<Wig> getDownsampleCoverage() {
        return downsampleCoverage;
    }

    public void setDownsampleCoverage(List<Wig> downsampleCoverage) {
        this.downsampleCoverage = downsampleCoverage;
    }

    public List<BaseCoverage> getBaseCoverage() {
        return baseCoverage;
    }

    public void setBaseCoverage(List<BaseCoverage> baseCoverage) {
        this.baseCoverage = baseCoverage;
    }

    public List<SpliceJunctionsEntity> getSpliceJunctions() {
        return spliceJunctions;
    }

    public void setSpliceJunctions(List<SpliceJunctionsEntity> spliceJunctions) {
        this.spliceJunctions = spliceJunctions;
    }

    public Integer getMinPosition() {
        return minPosition;
    }

    public void setMinPosition(Integer minPosition) {
        this.minPosition = minPosition;
    }

    public List<Wig> getRegions() {
        return regions;
    }

    public void setRegions(List<Wig> regions) {
        this.regions = regions;
    }
}
