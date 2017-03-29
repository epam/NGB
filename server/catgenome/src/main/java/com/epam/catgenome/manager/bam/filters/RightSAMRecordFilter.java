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

package com.epam.catgenome.manager.bam.filters;

import java.io.IOException;
import java.util.List;

import com.epam.catgenome.entity.bam.BasePosition;
import com.epam.catgenome.manager.bam.sifters.DownsamplingSifter;
import htsjdk.samtools.SAMRecord;

/**
 * This implementation of (@code Filter) filters the reads by its left border (read's start). If the read's
 * start is strictly greater than the filter's border it will be added to the result, so reads
 * that intersect the left border will be filtered out.
 * After filtering the reads are downsampled by the inner {@code DownsamplingSifter}.
 */
public class RightSAMRecordFilter implements Filter<SAMRecord> {

    private final DownsamplingSifter<SAMRecord> sifter;
    private final int leftBorder;

    /**
     * @param start left border for filtering
     */
    public RightSAMRecordFilter(final int start, final DownsamplingSifter<SAMRecord> sifter) {
        this.sifter = sifter;
        this.leftBorder = start;
    }

    @Override
    public DownsamplingSifter<SAMRecord> getSifter() {
        return sifter;
    }

    /**
     * Reads that intersect the left border will be filtered out
     * @param record representing a read
     * @param start of the read
     * @param end of the read
     * @param differentBase list of read bases that differ from the reference nucleotides
     * @param headStr soft clipped start of the read
     * @param tailStr soft clipped end of the read
     * @throws IOException
     */
    @Override
    public void add(final SAMRecord record, final int start, final int end,
                    final List<BasePosition> differentBase, final String headStr, final String tailStr)
            throws IOException {
        if (leftBorder < start) {
            sifter.add(record, start, end, differentBase, headStr, tailStr);
        }
    }
}
