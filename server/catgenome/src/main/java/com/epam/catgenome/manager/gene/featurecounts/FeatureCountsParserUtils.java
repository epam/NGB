/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.manager.gene.featurecounts;

public final class FeatureCountsParserUtils {
    public static final String TAB_DELIMITER = "\t";
    public static final String FIELD_DELIMITER = ";";
    public static final String EMPTY_VALUE = ".";
    public static final int GENE_ID_INDEX = 0;
    public static final int CHROMOSOMES_INDEX = 1;
    public static final int STARTS_INDEX = 2;
    public static final int ENDS_INDEX = 3;
    public static final int STRANDS_INDEX = 4;
    public static final int LENGTH_INDEX = 5;
    public static final int MANDATORY_COLUMNS_COUNT = 6;
    public static final String FEATURE_COUNTS_SOURCE = "featureCounts";
    public static final String LENGTH_ATTRIBUTE_NAME = "Length";
    public static final String PARENT_ATTRIBUTE_NAME = "Parent";
    public static final String GENE_ID_ATTRIBUTE_NAME = "gene_id";
    public static final String TRANSCRIPT_ID_ATTRIBUTE_NAME = "transcript_id";
    public static final String EXON_ID_ATTRIBUTE_NAME = "exon_id";
    public static final String ID_ATTRIBUTE_NAME = "ID";
    public static final String GENE_TYPE = "gene";
    public static final String EXON_TYPE = "exon";
    public static final String TRANSCRIPT_TYPE = "transcript";

    private FeatureCountsParserUtils() {
        // no op
    }
}
