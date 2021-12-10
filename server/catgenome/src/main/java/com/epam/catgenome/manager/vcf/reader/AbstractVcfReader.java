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

package com.epam.catgenome.manager.vcf.reader;

import static com.epam.catgenome.entity.BiologicalDataItemResourceType.GA4GH;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.exception.VcfReadingException;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;

/**
 *  {@code AbstractVcfReader} provides an abstract implementation of
 *  {@code VcfReader}. It defines several constants for VCf format
 *   and provides a factory method for creating {@code VcfReader} instances.
 */
public abstract class AbstractVcfReader implements VcfReader {
    protected static final List<Pattern> BIND_PATTERNS = new ArrayList<>();
    protected static final String BIND_CHR_ATTRIBUTE = "BIND_CHR";
    protected static final String BIND_POS_ATTRIBUTE = "BIND_POS";
    protected static final String STRUCT_DEL_TAG = "<DEL>";
    protected static final String STRUCT_INS_TAG = "<INS>";
    protected static final Pattern DUP_TAG = Pattern.compile("<DUP.*>");
    protected static final String INV_TAG = "<INV>";

    static {
        BIND_PATTERNS.add(Pattern.compile("\\w*\\[(\\w++):(\\d++)\\[\\w*"));
        BIND_PATTERNS.add(Pattern.compile("\\w*\\](\\w++):(\\d++)\\]\\w*"));
    }

    protected ReferenceGenomeManager referenceGenomeManager;

    protected Variation findLastNotBndVariation(List<Variation> variations) {
        for (int i = variations.size() - 1; i >= 0; i--) {
            if (variations.get(i).getType() != VariationType.BND) {
                return variations.get(i);
            }
        }
        return null;
    }

    /**
     * Factory method for creating a {@code VcfReader} instance according to the data resource type.
     * File system and GA4GH services are supported for VCF files.
     * @param resourceType determines the class of created {@code VcfReader}
     * @param httpDataManager for access to VCF data in the GA4GH service
     * @param fileManager for access to VCF file in the file system
     * @param referenceGenomeManager for loading reference data
     * @return a {@code VcfReader} instance for reading VCF data
     */
    public static VcfReader createVcfReader(final BiologicalDataItemResourceType resourceType, final HttpDataManager
            httpDataManager, final FileManager fileManager, final ReferenceGenomeManager referenceGenomeManager) {
        return resourceType == GA4GH ? new VcfGa4ghReader(httpDataManager, referenceGenomeManager) :
                new VcfFileReader(fileManager, referenceGenomeManager);
    }

    /**
     * Reads the variations data from the VCF source and loads it into a track
     * @param vcfFile data source
     * @param track for loading data
     * @param chromosome reference sequence
     * @param sampleIndex determines fro which sample from the file variations are loaded
     * @param loadInfo if true data from the INFO fields from VCF file will be loaded into the track,
     *                 otherwise it will be ignored
     * @return {@code Track} filled with variations from a specified {@code VcfFile}
     * @throws VcfReadingException
     */
    @Override
    public abstract Track<Variation> readVariations(VcfFile vcfFile, Track<Variation> track, Chromosome chromosome,
                            Integer sampleIndex, boolean loadInfo, boolean collapse,
                                                    EhCacheBasedIndexCache indexCache) throws VcfReadingException;

    /**
     * Allows navigating between the neighbouring variations
     * @param fromPosition start position for finding a neighbouring variation
     * @param vcfFile data source
     * @param sampleIndex determines fro which sample from the file variations are loaded
     * @param chromosome reference sequence
     * @param forward if true the next variation is loaded, otherwise the previous
     * @return
     * @throws VcfReadingException
     */
    @Override
    public abstract Variation getNextOrPreviousVariation(int fromPosition, VcfFile vcfFile,
                                                Integer sampleIndex, Chromosome chromosome,
                                                boolean forward, EhCacheBasedIndexCache indexCache)
            throws VcfReadingException;
}
