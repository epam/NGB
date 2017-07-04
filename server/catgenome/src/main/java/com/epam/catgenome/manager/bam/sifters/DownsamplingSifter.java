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

package com.epam.catgenome.manager.bam.sifters;

import java.io.IOException;
import java.util.List;

import com.epam.catgenome.entity.bam.BasePosition;
import com.epam.catgenome.entity.wig.Wig;

/**
 * Source:      DownsamplingSifter.java
 * Created:     7/4/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 * <p>
 * Represents interface for collecting and downsampling {@code Read} objects.
 * Downsampling is necessary for BAM tracks, where we have a coverage limit.
 * Provides methods for getting list of downsampled records and read coverage.
 * </p>
 */
public interface DownsamplingSifter<T> {
    /**
     * @param record representing a read
     * @param start of the read
     * @param end of the read
     * @param differentBase list of read bases that differ from the reference nucleotides
     * @param headStr soft clipped start of the read
     * @param tailStr soft clipped end of the read
     * @throws IOException
     */
    void add(T record, int start, int end, List<BasePosition> differentBase, String headStr,
             String tailStr) throws IOException;

    /**
     * Should be called if no more records will be added. Must be called only once
     */
    void finish();

    /**
     * @return (@code Wig )blocks of downsampled coverage
     */
    List<Wig> getDownsampleCoverageResult();

    int getFilteredReadsCount();
}
