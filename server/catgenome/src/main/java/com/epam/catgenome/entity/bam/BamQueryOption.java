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

import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.util.BamUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Source:
 * Created:     7/5/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 * Class represents the available options for track/get API
 */
public class BamQueryOption {
    private Boolean showClipping;
    private Boolean showSpliceJunction;
    private TrackDirectionType trackDirection;
    private BamTrackMode mode;
    private Integer frame;
    private Integer count;
    private boolean filterNotPrimary = false;
    private boolean filterVendorQualityFail = false;
    private boolean filterDuplicate = false;
    private boolean filterSupplementaryAlignment = false;

    @JsonIgnore
    private Long refID;
    @JsonIgnore
    private String chromosomeName;
    @JsonIgnore
    private boolean downSampling = false;

    /**
     * Default constructor for representing missing options
     */
    public BamQueryOption() {
        mode = BamTrackMode.FULL; // FULL query by default
        //        Empty constructor for initialization from the code
    }

    /**
     * Constructor for filling all options below
     * @param showClipping if true return the track with soft clipping
     * @param showSpliceJunction  if true return a information about splice junction
     * @param trackDirection represents the approach to read filtering with respect to track borders
     * @param frame size of frame for downsampling
     * @param count number of reads in the frame
     * @param chromosome reference sequence
     */
    public BamQueryOption(Boolean showClipping, Boolean showSpliceJunction, TrackDirectionType trackDirection,
                          Integer frame, Integer count, Chromosome chromosome) {
        this(showClipping, showSpliceJunction, trackDirection, frame, count);
        this.chromosomeName = chromosome.getName();
        this.refID = chromosome.getReferenceId();
        BamUtil.validateOptions(this, chromosome);
    }

    /**
     * Constructor without reference sequence
     * @param showClipping if true return the track with soft clipping
     * @param showSpliceJunction if true return a information about track junction
     * @param trackDirection represents the approach to read filtering with respect to frame borders
     * @param frame size of frame for downsampling
     * @param count number of reads in the frame
     */
    public BamQueryOption(Boolean showClipping, Boolean showSpliceJunction, TrackDirectionType trackDirection,
            Integer frame, Integer count) {
        this();
        this.showClipping = showClipping;
        this.count = count;
        this.frame = frame;
        this.showSpliceJunction = showSpliceJunction;
        this.trackDirection = trackDirection;
    }

    public Boolean getShowClipping() {
        return showClipping;
    }

    public void setShowClipping(Boolean showClipping) {
        this.showClipping = showClipping;
    }

    public TrackDirectionType getTrackDirection() {
        return trackDirection;
    }

    public void setTrackDirection(TrackDirectionType trackDirection) {
        this.trackDirection = trackDirection;
    }

    public String getChromosomeName() {
        return chromosomeName;
    }

    public void setChromosomeName(String chromosomeName) {
        this.chromosomeName = chromosomeName;
    }

    public Integer getFrame() {
        return frame;
    }

    public void setFrame(Integer frame) {
        this.frame = frame;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Boolean getShowSpliceJunction() {
        return showSpliceJunction;
    }

    public void setShowSpliceJunction(Boolean showSpliceJunction) {
        this.showSpliceJunction = showSpliceJunction;
    }

    public void setRefID(Long refID) {
        this.refID = refID;
    }

    public Long getRefID() {
        return refID;
    }

    public boolean isDownSampling() {
        return downSampling;
    }

    public void setDownSampling(boolean downSampling) {
        this.downSampling = downSampling;
    }

    public boolean isFilterNotPrimary() {
        return filterNotPrimary;
    }

    public void setFilterNotPrimary(boolean filterNotPrimary) {
        this.filterNotPrimary = filterNotPrimary;
    }

    public boolean isFilterVendorQualityFail() {
        return filterVendorQualityFail;
    }

    public void setFilterVendorQualityFail(boolean filterVendorQualityFail) {
        this.filterVendorQualityFail = filterVendorQualityFail;
    }

    public boolean isFilterDuplicate() {
        return filterDuplicate;
    }

    public void setFilterDuplicate(boolean filterDuplicate) {
        this.filterDuplicate = filterDuplicate;
    }

    public boolean isFilterSupplementaryAlignment() {
        return filterSupplementaryAlignment;
    }

    public void setFilterSupplementaryAlignment(boolean filterSupplementaryAlignment) {
        this.filterSupplementaryAlignment = filterSupplementaryAlignment;
    }

    public BamTrackMode getMode() {
        return mode;
    }

    public void setMode(BamTrackMode mode) {
        this.mode = mode;
    }
}
