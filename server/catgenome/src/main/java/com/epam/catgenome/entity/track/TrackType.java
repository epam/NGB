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

/**
 * Source:      TrackType.java
 * Created:     10/8/15, 8:22 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code TrackType} defines persistent types of tracks that are supported by VCF browser
 *  and describes their major properties.
 * </p>
 */
public enum TrackType {

    /**
     * for reference tracks
     */
    REF,

    /**
     * for VCF tracks
     */
    VCF,

    /**
     * for BAM tracks
     */
    BAM,

    /**
     * for GFF tracks
     */
    GFF,

    /**
     * for cytoband tracks
     */
    CYTOBAND,

    /**
     * for WIG/BIGWIG tracks
     */
    WIG,

    /**
     * for GA4GH tracks
     */
    GA4GH,

    /**
     * for BED tracks
     */
    BED,

    /**
     * for SEG tracks
     */
    SEG,

    /**
     * for MAF tracks
     */
    MAF,

    /**
     * for VG tracks
     */
    VG
}
