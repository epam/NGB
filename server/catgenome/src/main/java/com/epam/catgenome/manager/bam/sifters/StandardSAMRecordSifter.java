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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.epam.catgenome.entity.bam.BasePosition;
import com.epam.catgenome.entity.bam.Read;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.util.BamUtil;
import htsjdk.samtools.SAMRecord;

/**
 * Source:
 * Created:     7/22/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 * Represents a (@code DownsamplingSifter) for the reads with random downsampling.
 * All added reads are shuffled and only (@code count) first reads will be returned as a result of
 * downsampling. This implementation assumes that reads are added in a sorted by start coordinate order.
 */
public class StandardSAMRecordSifter implements DownsamplingSifter<SAMRecord> {

    private final List<Read> resultList = new ArrayList<>();
    private final List<Read> bufferResult = new ArrayList<>();
    private final List<Wig> downsampledCoverage = new ArrayList<>();
    private final int frame;
    private final int count;
    private final int endTrack;

    private int statPosition = -1;
    private int border;

    /**
     * Indicates if BAM records should be saved to memory or just counted. Is set when filteredReadsCount
     * exceeds maxReadCount
     */
    private boolean exceedsMaxReadCount = false;

    //private int maxReadCount;
    private int filteredReadsCount = 0;
    private int totalReadsCount = 0;

    /**
     * @param frame size of the frame for downsampling
     * @param count maximum number of reads left after downsampling for each frame
     * @param endTrack left border of the track interval
     */
    public StandardSAMRecordSifter(final int frame, final int count, final int endTrack, boolean coverageOnly) {
        // TODO: int maxReadCount - decide reads or coverage by by read count
        this.frame = frame;
        this.count = count;
        this.endTrack = endTrack;
        this.exceedsMaxReadCount = coverageOnly;
        //this.maxReadCount = maxReadCount;
    }

    /**
     * Reads must by added sorted by read's start coordinate. If a read intersects the frame,
     * it will be added to the right frame.
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
        if (statPosition < 0) {
            statPosition = start;
            border = statPosition + frame - 1;
            border = border > endTrack ? endTrack : border;
        }
        if (start > border) {
            flushList();
            refreshForNewFrame(start);
        }
        bufferResult.add(BamUtil.createReadFromRecord(record, start, end, differentBase, headStr, tailStr));
        totalReadsCount++;
    }

    /**
     * @return downsampled list of the reads, added to the sifter
     */
    @Override
    public List<Read> getReadListResult() {
        flushList();
        return resultList;
    }

    /**
     * @return downsampled coverage calculated from the reads, added to the sifter
     */
    @Override
    public List<Wig> getDownsampleCoverageResult() {
        flushList();
        return downsampledCoverage;
    }

    @Override
    public int getFilteredReadsCount() {
        flushList();
        return filteredReadsCount;
    }

    private void shuffleArr(final int[] arr, final int minSize) {
        Random rnd = new Random();
        for (int i = 0; i < minSize - 1; i++) {
            int index = rnd.nextInt(arr.length - i) + i;
            int a = arr[index];
            arr[index] = arr[i];
            arr[i] = a;
        }
    }

    private void flushList() {
        final int buffSize = bufferResult.size();
        if (buffSize > count) {
            final int[] helpArr = new int[buffSize];
            for (int i = 0; i < helpArr.length; i++) {
                helpArr[i] = i;
            }
            //get random read's
            shuffleArr(helpArr, count);
            // because our client don't need it
            if (!exceedsMaxReadCount && buffSize - count > 0) {
                downsampledCoverage.add(new Wig(statPosition, border, buffSize - count));
            }

            for (int i = 0; i < count; i++) {
                if (!exceedsMaxReadCount) {
                    resultList.add(bufferResult.get(helpArr[i]));
                }

                filteredReadsCount++;
                //checkExceedsLimit(); TODO: uncomment
            }
        } else {
            if (!exceedsMaxReadCount) {
                resultList.addAll(bufferResult);
            }

            filteredReadsCount += bufferResult.size();
            //checkExceedsLimit(); TODO: uncomment
        }
    }

    /*private void checkExceedsLimit() {
        if (!exceedsMaxReadCount && filteredReadsCount > maxReadCount) {
            exceedsMaxReadCount = true;
            bufferResult.clear();
            resultList.clear();
        }
    }*/

    private void refreshForNewFrame(final int start) {
        do {
            border += frame;
        } while (start >= border);
        statPosition = border - frame + 1;
        if (border > endTrack) {
            border = endTrack;
        }
        bufferResult.clear();
    }

    public int getTotalReadsCount() {
        return totalReadsCount;
    }
}
