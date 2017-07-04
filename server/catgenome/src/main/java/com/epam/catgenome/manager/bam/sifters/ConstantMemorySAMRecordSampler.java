/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

import com.epam.catgenome.entity.bam.BasePosition;
import com.epam.catgenome.entity.bam.Read;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.manager.bam.BamTrackEmitter;
import com.epam.catgenome.util.BamUtil;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.RuntimeIOException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a (@code DownsamplingSifter) for the reads with random downsampling. Uses only {@code count * Read_size}
 * memory during downsampling. Implements reservoir sampling (Algorithm R).
 * Will write {@code count} downsampled reads to the given trackEmitter after processing of each frame.
 */
public class ConstantMemorySAMRecordSampler implements DownsamplingSifter<SAMRecord> {

    private final int frameSize;
    private final int maxElementsInFrame;
    private final int endTrack;
    private final boolean countOnly;
    private BamTrackEmitter trackEmitter;

    private final Random random;

    private final ArrayList<Read> frameBuffer;
    private final List<Wig> downsampleCoverage;

    private int recordsCount;
    private int startPosition;
    private int border;

    private boolean finished;

    /**
     * @param frame size of the frame for downsampling
     * @param count maximum number of reads left after downsampling for each frame
     * @param endTrack left border of the track interval
     * @param coverageOnly - if true, no reads will be send to emitter during processing
     * @param trackEmitter where to write reads
     */
    public ConstantMemorySAMRecordSampler(final int frame, final int count, final int endTrack, boolean coverageOnly,
                                          BamTrackEmitter trackEmitter) {
        this.frameSize = frame;
        this.maxElementsInFrame = count;
        this.endTrack = endTrack;
        this.countOnly = coverageOnly;
        this.trackEmitter = trackEmitter;

        this.random = new Random(System.nanoTime());
        this.frameBuffer = new ArrayList<>(count);
        this.downsampleCoverage = new ArrayList<>();

        this.startPosition = -1;
    }

    /**
     * @param record representing a read
     * @param start of the read
     * @param end of the read
     * @param differentBase list of read bases that differ from the reference nucleotides
     * @param headStr soft clipped start of the read
     * @param tailStr soft clipped end of the read
     * @throws IOException
     */
    @Override
    public void add(SAMRecord record, int start, int end, List<BasePosition> differentBase, String headStr,
                    String tailStr) throws IOException {
        assertNotFinished();

        recordsCount++;
        if (countOnly) {
            return;
        }

        checkLazyInit(start);

        if (start > border) {
            refreshForNewFrame(start);
        }

        final double randValue = random.nextDouble();

        int recordInResultPos = recordsCount <= maxElementsInFrame ?
                (recordsCount - 1) :
                (int) (randValue * recordsCount);

        if (recordInResultPos < maxElementsInFrame) {
            Read readFromRecord = BamUtil.createReadFromRecord(record, start, end, differentBase, headStr, tailStr);
            if (frameBuffer.size() <= recordInResultPos) {
                frameBuffer.add(readFromRecord);
            } else {
                frameBuffer.set(recordInResultPos, readFromRecord);
            }
        }
    }

    /**
     * Should be called if no more records will be added. Must be called only once
     */
    @Override
    public void finish() {
        try {
            sendBuffer();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        finished = true;
    }

    @Override
    public List<Wig> getDownsampleCoverageResult() {
        return downsampleCoverage;
    }

    private void assertNotFinished() {
        if (finished) {
            throw new IllegalStateException("Adding records was already finished");
        }
    }

    @Override
    public int getFilteredReadsCount() {
        //TODO: remove method from interface? It is unused
        throw new UnsupportedOperationException();
    }
    private void checkLazyInit(int start) {
        if (startPosition < 0) {
            startPosition = start;
            border = startPosition + frameSize - 1;
            border = border > endTrack ? endTrack : border;
        }
    }

    private void refreshForNewFrame(final int start) throws IOException {
        sendBuffer();

        if (frameBuffer.size() >= maxElementsInFrame) {
            downsampleCoverage.add(new Wig(startPosition, border, recordsCount - maxElementsInFrame - 1));
        }

        do {
            border += frameSize;
        } while (start >= border);
        startPosition = border - frameSize + 1;
        if (border > endTrack) {
            border = endTrack;
        }
        frameBuffer.clear();
        recordsCount = 1;
    }

    private void sendBuffer() throws IOException {
        for (Read read : frameBuffer) {
            trackEmitter.writeRecord(read);
        }
    }
}
