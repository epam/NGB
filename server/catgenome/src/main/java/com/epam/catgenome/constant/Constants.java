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

package com.epam.catgenome.constant;

/**
 * Source:
 * Created:     10/14/2015
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code Constants} represents different constants shared between different application modules
 * </p>
 */
public final class Constants {

    public static final int MASK_LOW_BYTE = 0x0F;
    public static final int MASK_HIGH_BYTE = 0xF0;

    public static final int CODING_ARRAY_LENGTH = Byte.MAX_VALUE - Byte.MIN_VALUE;

    /**
     * Step in GC tree
     */
    public static final int GC_CONTENT_STEP = 10;

    /**
     * Maximum GC tree levels count
     */
    public static final int GC_CONTENT_MAX_TREE_LVL_COUNT = 10;

    /**
     * Approximate screen width for GC calculation
     */
    public static final int SCREEN_WIDTH = 1000;

    /**
     * Minimum top level GC length
     */
    public static final int GC_CONTENT_MIN_LENGTH = SCREEN_WIDTH * GC_CONTENT_STEP;

    // reference constants
    /**
     * Indicates scale factor threshold, that enables reference nucleotides browsing
     */
    public static final double GC_FORMAT_FACTOR = 0.5;

    /**
     * Scale factor threshold, from which we start showing AA sequences
     */
    public static final float AA_SHOW_FACTOR = 5F;

    // for BAM
    /**
     * Value of an attribute, indicated that BAM is sorted
     */
    public static final String BAM_FILE_ATTRIBUTE_SORTED_VALUE = "coordinate";
    /**
     * Name of an attribute, indicated that BAM is sorted
     */
    public static final String BAM_FILE_ATTRIBUTE_SORTED_NAME = "SO";

    /**
     * Maximum value of end index of track, that is read to validate BAM file
     */
    public static final int MAX_BAM_END_INDEX_TEST = 1000;

    /**
     * Start index of track, that is read to validate BAM file
     */
    public static final int BAM_START_INDEX_TEST = 10;

    public static final int REFERENCE_STEP = 100;

    /**
     * Minimum value of BAM downsampling parameter frame
     */
    public static final int BAM_DOWNSAMPLING_MIN_FRAME_SIZE = 20;

    /**
     * Minimum value of BAM downsampling count parameter
     */
    public static final int BAM_DOWNSAMPLING_MIN_COUNT = 5;

    /**
     * Maximum value of BAM downsampling count parameter
     */
    public static final int BAM_DOWNSAMPLING_MAX_COUNT = 10000;


    // for WIG
    /**
     * Maximum buffer size when reading Wig files
     */
    public static final int MAX_WIG_BUFFER_SIZE = 10000;

    // for Vcf/Gff Managers
    /**
     * Indicates the size of a chunk to read while jumping to the previous feature
     */
    public static final int PREV_FEATURE_OFFSET = 10001;

    // for String
    /**
     * Chromosome name prefix in genomic data files
     */
    public static final String CHROMOSOME_PREFIX = "chr";

    // for GA4GH
    public static final int GA4GH_MAX_BASE_SIZE = 1000000;
    public static final int GA4GH_MAX_SIZE = 100000;
    public static final String URL_GOOGLE_GENOMIC_API = "https://genomics.googleapis.com/v1/";
    public static final String VARIANTS_PAGE_SIZE = "10000";
    public static final String URL_REFERENCE_START = "&start=";
    public static final String URL_REFERENCE_END = "&end=";
    public static final String URL_REFERENCE_BASES = "/bases/";

    /**
     * Google API key for GA4GH, should be replaced with user's
     */
    public static final String GOOGLE_API_KEY = "";

    /**
     * Reference Sets service URL part
     */
    public static final String URL_REFERENCE_SET = "referencesets/";
    public static final String URL_REFERENCE = "references/";
    public static final String URL_VARIANTS = "variants/";
    public static final String URL_VARIANT_SETS = "variantsets/";
    public static final String URL_CALL_SETS = "callsets/";
    public static final String URL_SEARCH = "search";
    //fields GA4GH for POST request
    public static final String CALL_SET_ID = "callSetIds";
    public static final String VARIANT_SET_ID = "variantSetIds";
    public static final String END_POSITION = "end";
    public static final String START_POSITION = "start";
    public static final String PAGE_SIZE = "pageSize";
    public static final String REFERENCE_NAME = "referenceName";
    public static final String PAGE_TOKEN = "pageToken";

    // for query
    /**
     * Indicates maximum interval in bps, displayed on track. Used only for tracks, which are limited in interval size,
     * e.g. BAM track
     */
    public static final int MAX_INTERVAL = 70000;

    /**
     * Amount of bytes in Kb
     */
    public static final int KILO_BYTE_SIZE = 1024;

    private Constants() {
        //No-op
    }

}
