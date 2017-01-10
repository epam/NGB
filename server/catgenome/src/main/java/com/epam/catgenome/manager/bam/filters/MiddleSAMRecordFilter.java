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

import com.epam.catgenome.entity.bam.BamQueryOption;
import com.epam.catgenome.entity.bam.BasePosition;
import com.epam.catgenome.entity.bam.Read;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.manager.bam.sifters.DownsamplingSifter;
import com.epam.catgenome.util.BamUtil;
import htsjdk.samtools.SAMRecord;

/**
 * This implementation of {@code Filter} doesn't filter the reads and adds all of them
 * to downsampling
 */
public class MiddleSAMRecordFilter implements Filter<SAMRecord> {
    private final DownsamplingSifter<SAMRecord> shifter;

    /**
     * @param end  for creating the wrapped {@code DownsamplingSifter}
     * @param options for creating the wrapped {@code DownsamplingSifter}
     */
    public MiddleSAMRecordFilter(final int end, final BamQueryOption options) {
        shifter = BamUtil.createSifter(end, options);
    }

    /**
     * No actual filtering is performed, all reads are passed to the inner  {@code DownsamplingSifter}
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
        shifter.add(record, start, end, differentBase, headStr, tailStr);
    }

    /**
     * @return list of reads after downsampling
     */
    @Override
    public List<Read> getReadListResult() {
        return shifter.getReadListResult();
    }

    /**
     * @return read coverage after downsampling
     */
    @Override
    public List<Wig> getDownsampleCoverageResult() {
        return shifter.getDownsampleCoverageResult();
    }
}
