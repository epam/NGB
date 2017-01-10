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

package com.epam.catgenome.manager.vcf.reader;

import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.exception.VcfReadingException;

/**
 *  {@code VcfReader} provides an interface for
 *  reading files in VCF format, loading and finding variations
 *  int VCF files.
 */
public interface VcfReader {

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
    Track<Variation> readVariations(VcfFile vcfFile, Track<Variation> track, Chromosome chromosome,
                                    Integer sampleIndex, boolean loadInfo) throws VcfReadingException;

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
    Variation getNextOrPreviousVariation(int fromPosition, VcfFile vcfFile,
                                         Integer sampleIndex, Chromosome chromosome,
                                         boolean forward) throws VcfReadingException;
}
