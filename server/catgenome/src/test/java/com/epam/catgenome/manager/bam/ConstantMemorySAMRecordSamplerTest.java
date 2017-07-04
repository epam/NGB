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

package com.epam.catgenome.manager.bam;

import com.epam.catgenome.entity.bam.BamTrack;
import com.epam.catgenome.entity.bam.Read;
import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.manager.bam.sifters.ConstantMemorySAMRecordSampler;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class ConstantMemorySAMRecordSamplerTest {

    private static final int END_TRACK = 100_000;
    private static final int FRAME_SIZE = 30_000;
    private static final int DOWNSAMPLE_COUNT = 1000;

    private static final int READ_SIZE = 100;

    @Test
    public void test() throws IOException {
        ResponseEmitterMock emitterMock = new ResponseEmitterMock();
        BamTrackEmitter trackEmitter = new BamTrackEmitter(emitterMock);
        ConstantMemorySAMRecordSampler sampler = new ConstantMemorySAMRecordSampler(FRAME_SIZE, DOWNSAMPLE_COUNT,
                END_TRACK, false, trackEmitter);

        for (int i = 0; i < END_TRACK; i++) {
            sampler.add(new SAMRecord(new SAMFileHeader()), i, i + READ_SIZE, null, "head", "tail");
        }
        trackEmitter.writeTrackAndFinish(new BamTrack<>());

        Assert.assertNotNull(emitterMock.getBamTrack().getBlocks());
        List<Read> results = emitterMock.getBamTrack().getBlocks();

        Assert.assertTrue(results.size() < DOWNSAMPLE_COUNT * (END_TRACK / FRAME_SIZE) + 1);
        Assert.assertTrue(results.size() >= DOWNSAMPLE_COUNT * (END_TRACK / FRAME_SIZE));

        results.sort(Comparator.comparingInt(Block::getStartIndex));
    }
}