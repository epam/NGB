/*
 * MIT License
 *
 * Copyright (c) 2016-2022 EPAM Systems
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

package com.epam.ngb.cli.entity;

import static com.epam.ngb.cli.constants.MessageConstants.ERROR_INDEX_FORMAT_DOES_NOT_MATCH;
import static com.epam.ngb.cli.constants.MessageConstants.ERROR_UNSUPPORTED_FORMAT;
import static com.epam.ngb.cli.constants.MessageConstants.ERROR_UNSUPPORTED_ZIP;
import static com.epam.ngb.cli.constants.MessageConstants.getMessage;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents data formats supported by NGB server and CLi and provides several methods
 * for verifying file formats and indexes. Data format is determined by the file extension.
 */
public enum BiologicalDataItemFormat {
    REFERENCE(false, true),
    VCF(false, true),
    BAM(true),
    GENE(false, true),
    WIG(false, true),
    VCF_INDEX,
    GENE_INDEX,
    BAM_INDEX,
    BED_INDEX,
    BED(false, true),
    SEG(false, true),
    SEG_INDEX,
    VG,
    GENBANK,
    GBK,
    GB,
    FEATURE_COUNTS,
    HEATMAP,
    LINEAGE_TREE,
    PATHWAY;

    // The constants above used to carry a numeric id as their first constructor argument. Nothing
    // ever read it - there was no getter and the field was assigned and never used - and the values
    // were simply the declaration order, 1 to 22 back when MAF and MAF_INDEX were still declared
    // here. Removing those two shifted the ordinals of everything below them, which is harmless for
    // the same reason: neither the old ids nor the ordinals are read by anything. The
    // format is sent to the server by name, not by id.
    /**
     * If true format requires index for registration, otherwise index is optional
     */
    private boolean requireIndex;
    /**
     * If true format supports GZIP compression, otherwise compressed files are not supported
     */
    private boolean supportGZip;

    private static final Logger LOGGER = LoggerFactory.getLogger(BiologicalDataItemFormat.class);

    /**
     * Default GZIP format
     */
    private static final String GZ_EXTENSION = "gz";

    /**
     * Default tabix index format fro tab-delimited files
     */
    public static final String TABIX_INDEX_EXTENSION = "tbi";

    public static final String IDX_INDEX_EXTENSION = "idx";

    /**
     * Represents mapping of the file extensions to {@code BiologicalDataItemFormat}
     */
    private static final Map<String, BiologicalDataItemFormat> EXTENSIONS_MAP = new HashMap<>();
    static {
        EXTENSIONS_MAP.put("vcf", VCF);
        EXTENSIONS_MAP.put("gff", GENE);
        EXTENSIONS_MAP.put("gtf", GENE);
        EXTENSIONS_MAP.put("gff3", GENE);
        EXTENSIONS_MAP.put("bam", BAM);
        EXTENSIONS_MAP.put("cram", BAM);
        EXTENSIONS_MAP.put("seg", SEG);
        EXTENSIONS_MAP.put("bw", WIG);
        EXTENSIONS_MAP.put("bigwig", WIG);
        EXTENSIONS_MAP.put("bed", BED);
        EXTENSIONS_MAP.put("vg", VG);
        EXTENSIONS_MAP.put("bdg", WIG);
        EXTENSIONS_MAP.put("bg", WIG);
        EXTENSIONS_MAP.put("bedGraph", WIG);
        EXTENSIONS_MAP.put("genbank", GENE);
        EXTENSIONS_MAP.put("gb", GENE);
        EXTENSIONS_MAP.put("gbk", GENE);
        EXTENSIONS_MAP.put("gbf", GENE);
        EXTENSIONS_MAP.put("gp", GENE);
        EXTENSIONS_MAP.put("genepred", GENE);
        EXTENSIONS_MAP.put("genePred", GENE);
        EXTENSIONS_MAP.put("featureCounts", GENE);
        EXTENSIONS_MAP.put("txt", GENE);
    }

    /**
     * Represents a mapping from format to supported index extension
     */
    private static final Map<BiologicalDataItemFormat, Set<String>> INDEX_EXTENSION_MAP =
            new EnumMap<>(BiologicalDataItemFormat.class);


    static {
        INDEX_EXTENSION_MAP.put(BAM, new HashSet<>(Arrays.asList("bai", "crai")));
        INDEX_EXTENSION_MAP.put(WIG, new HashSet<>(Arrays.asList(TABIX_INDEX_EXTENSION, IDX_INDEX_EXTENSION)));
        INDEX_EXTENSION_MAP.put(VCF, new HashSet<>(Arrays.asList(TABIX_INDEX_EXTENSION, IDX_INDEX_EXTENSION)));
        INDEX_EXTENSION_MAP.put(BED, new HashSet<>(Arrays.asList(TABIX_INDEX_EXTENSION, IDX_INDEX_EXTENSION)));
        INDEX_EXTENSION_MAP.put(GENE, new HashSet<>(Arrays.asList(TABIX_INDEX_EXTENSION, IDX_INDEX_EXTENSION)));
    }

    /**
     * By default index is not required and gzip compression is not supported
     */
    BiologicalDataItemFormat() {
        this(false, false);
    }

    BiologicalDataItemFormat(boolean requireIndex) {
        this(requireIndex, false);
    }

    BiologicalDataItemFormat(boolean requireIndex, boolean supportGZip) {
        this.requireIndex = requireIndex;
        this.supportGZip = supportGZip;
    }

    public boolean isRequireIndex() {
        return requireIndex;
    }

    /**
     * Verifies that input path to the index file corresponds to a supported index format
     * and checks if this index should be passed to the NGB server.
     * If index format isn't supported throws an exception
     * @param indexPath to an index file
     * @return true if server supports this index, otherwise - false
     * @throws IllegalArgumentException if index format doesn't match the file format
     */
    public boolean verifyIndex(final String indexPath) {
        final Set<String> expectedIndexFormat = INDEX_EXTENSION_MAP.get(this);
        if (expectedIndexFormat == null) {
            LOGGER.error(String.format("Provided index file won't be used, since server creates its own "
                    + "index for %s format.", this.name()));
            return false;
        }
        final String indexExtension = FilenameUtils.getExtension(indexPath);
        if (!expectedIndexFormat.contains(indexExtension)) {
            throw new IllegalArgumentException(getMessage(ERROR_INDEX_FORMAT_DOES_NOT_MATCH,
                    indexPath, this.name()));
        }
        return true;
    }

    /**
     * Determines {@code BiologicalDataItemFormat} by a path to the file
     * @param path to the file
     * @param additionalFormats additional formats that NGB supports (f.e bed-like narrowPeak and broadPeak)
     * @return defined {@code BiologicalDataItemFormat}
     * @throws IllegalArgumentException if file format is not supported or GZIP compression is not
     *          supported for a format
     */
    public static BiologicalDataItemFormat getByFilePath(
            final String path,
            final Map<String, BiologicalDataItemFormat> additionalFormats) {
        String extension = FilenameUtils.getExtension(path);
        boolean isZipped = false;
        if (GZ_EXTENSION.equals(extension)) {
            extension = FilenameUtils.getExtension(path.substring(0, path.length() -
                    GZ_EXTENSION.length() - 1));
            isZipped = true;
        }
        BiologicalDataItemFormat format = EXTENSIONS_MAP.get(extension);
        if (format == null) {
            format = additionalFormats.get(extension);
            if (format == null) {
                throw new IllegalArgumentException(getMessage(ERROR_UNSUPPORTED_FORMAT, extension));
            }
        }
        if (!format.supportGZip && isZipped) {
            throw new IllegalArgumentException(getMessage(ERROR_UNSUPPORTED_ZIP, format.name()));
        }
        return format;
    }
}
