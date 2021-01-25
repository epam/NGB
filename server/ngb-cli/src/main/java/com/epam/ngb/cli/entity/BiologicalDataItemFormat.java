/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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
    REFERENCE(1, false, true),
    VCF(2, false, true),
    BAM(3, true),
    GENE(4, false, true),
    WIG(5, false, true),
    VCF_INDEX(6),
    GENE_INDEX(7),
    BAM_INDEX(8),
    BED_INDEX(9),
    BED(10, false, true),
    SEG(11, false, true),
    SEG_INDEX(12),
    MAF(13, false, true),
    MAF_INDEX(14),
    VG(15);

    private long id;
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
        EXTENSIONS_MAP.put("maf", MAF);
        EXTENSIONS_MAP.put("vg", VG);
        EXTENSIONS_MAP.put("bdg", WIG);
        EXTENSIONS_MAP.put("bg", WIG);
        EXTENSIONS_MAP.put("bedGraph", WIG);
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
     * @param id
     */
    BiologicalDataItemFormat(long id) {
        this(id, false, false);
    }

    BiologicalDataItemFormat(long id, boolean requireIndex) {
        this(id, requireIndex, false);
    }

    BiologicalDataItemFormat(long id, boolean requireIndex, boolean supportGZip) {
        this.id = id;
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
