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

package com.epam.catgenome.controller.gene;

import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.vcf.Variation;

/**
 * Class used in querying protein sequences with considering of gene variations.
 */
public class ProteinSequenceVariationQuery {

    /**
     * Track of variations, which should be taking into account during protein sequence reconstruction.
     * This track of variations can be obtained through POST /vcf/variation/load service.
     */
    private Track<Variation> variations;

    /**
     * Track query of interested protein sequence reconstruction.
     */
    private TrackQuery trackQuery;

    public ProteinSequenceVariationQuery() {
        // no-op
    }

    /**
     * Constructs a new ProteinSequenceVariationQuery instance
     * @param variations Track of variations, which should be taking into account during protein sequence
     *                   reconstruction. This track of variations can be obtained through
     *                   POST /vcf/variation/load service.
     * @param trackQuery Track query of interested protein sequence reconstruction.
     */
    public ProteinSequenceVariationQuery(final Track<Variation> variations, final TrackQuery trackQuery) {
        this.variations = variations;
        this.trackQuery = trackQuery;
    }

    public void setVariations(final Track<Variation> variations) {
        this.variations = variations;
    }

    public void setTrackQuery(final TrackQuery trackQuery) {
        this.trackQuery = trackQuery;
    }

    /**
     * Track of variations, which should be taking into account during protein sequence reconstruction.
     * This track of variations can be obtained through POST /vcf/variation/load service.
     */
    public Track<Variation> getVariations() {
        return variations;
    }

    /**
     * Track query of interested protein sequence reconstruction.
     */
    public TrackQuery getTrackQuery() {
        return trackQuery;
    }
}
