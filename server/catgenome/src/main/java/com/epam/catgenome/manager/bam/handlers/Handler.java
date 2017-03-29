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

package com.epam.catgenome.manager.bam.handlers;

import java.io.IOException;
import java.util.List;

import com.epam.catgenome.entity.bam.BaseCoverage;
import com.epam.catgenome.entity.bam.SpliceJunctionsEntity;
import com.epam.catgenome.manager.bam.filters.Filter;
import com.epam.catgenome.manager.bam.sifters.DownsamplingSifter;

/**
 * Interface for handling list of reads from a BAM file and getting supplementary data from it.
 */
public interface Handler<T> {

    /**
     * @return minimum left coordinate from the added reads
     */
    Integer getMinPosition();

    /**
     * Adds a record to processing
     * @param record
     * @throws IOException
     */
    void add(T record) throws IOException;

    /**
     * @return list of {@SpliceJunctionsEntity} calculated from the added records
     */
    List<SpliceJunctionsEntity> getSpliceJunctions();

    /**
     * @return base coverage calculated from the added records
     */
    List<BaseCoverage> getBaseCoverage(double scaleFactor);

    /**
     * @return reference sequence for the interval covered by the added records
     */
    String getReferenceBuff();

    /**
     * @return handler's wrapped sifter
     */
    DownsamplingSifter<T> getSifter();

    /**
     * @return handler's wrapped filter
     */
    Filter<T> getFilter();
}
