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

package com.epam.catgenome.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.track.TrackType;
import htsjdk.samtools.SAMRecord;

/**
 * Created: 6/3/2016
 * Project: catgenome
 *
 * @author Nina_Lukashina
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class ConsensusSequenceUtilsTest {

    private static final String READ_1 = "ATTGC";
    private static final String READ_2 = "TTTGC";
    private static final String READ_3 = "TTTTT";
    private static final String READ_4 = "GGGCC";
    private static final String READ_5 = "ATGCC";
    private static final String READ_6 = "ATGGC";
    private static final String READ_7 = "CTGA";
    private static final int TRACK_START_INDEX = 1;
    private static final int SEQUENCE_1_START_INDEX = 1;
    private static final int SEQUENCE_2_START_INDEX = 6;
    private static final int SEQUENCE_3_START_INDEX = 11;
    private static final int TRACK_END_INDEX = 14;
    private static final int SEQUENCE_1_END_INDEX = 5;
    private static final int SEQUENCE_2_END_INDEX = 10;
    private static final int SEQUENCE_3_END_INDEX = 14;
    private static final int INITIAL_CAPACITY = 3;

    @Test
    public void testCalculateConsensusSequence() {
        Track<Sequence> track = new Track<>(TrackType.BAM);
        track.setStartIndex(TRACK_START_INDEX);
        track.setEndIndex(TRACK_END_INDEX);
        Sequence sequence1 = new Sequence(SEQUENCE_1_START_INDEX, SEQUENCE_1_END_INDEX);
        Sequence sequence2 = new Sequence(SEQUENCE_2_START_INDEX, SEQUENCE_2_END_INDEX);
        Sequence sequence3 = new Sequence(SEQUENCE_3_START_INDEX, SEQUENCE_3_END_INDEX);
        track.setBlocks(Arrays.asList(sequence1, sequence2, sequence3));

        // Init test reads.
        Map<Sequence, List<SAMRecord>> blockToReadsMap = new HashMap<>(INITIAL_CAPACITY);
        SAMRecord samRecord1 = Mockito.mock(SAMRecord.class);
        Mockito.when(samRecord1.getReadString()).thenReturn(READ_1);
        SAMRecord samRecord2 = Mockito.mock(SAMRecord.class);
        Mockito.when(samRecord2.getReadString()).thenReturn(READ_2);
        SAMRecord samRecord3 = Mockito.mock(SAMRecord.class);
        Mockito.when(samRecord3.getReadString()).thenReturn(READ_3);
        SAMRecord samRecord4 = Mockito.mock(SAMRecord.class);
        Mockito.when(samRecord4.getReadString()).thenReturn(READ_4);
        SAMRecord samRecord5 = Mockito.mock(SAMRecord.class);
        Mockito.when(samRecord5.getReadString()).thenReturn(READ_5);
        SAMRecord samRecord6 = Mockito.mock(SAMRecord.class);
        Mockito.when(samRecord6.getReadString()).thenReturn(READ_6);
        SAMRecord samRecord7 = Mockito.mock(SAMRecord.class);
        Mockito.when(samRecord7.getReadString()).thenReturn(READ_7);

        blockToReadsMap.put(sequence1, Arrays.asList(samRecord1, samRecord2, samRecord3));
        blockToReadsMap.put(sequence2, Arrays.asList(samRecord4, samRecord5));
        blockToReadsMap.put(sequence3, Arrays.asList(samRecord6, samRecord7));

        ConsensusSequenceUtils.calculateConsensusSequence(track, blockToReadsMap);

        Assert.assertNotNull(track);
        Assert.assertNotNull(track.getBlocks());
        Assert.assertNotNull(track.getBlocks().get(0));
        Assert.assertNotNull(track.getBlocks().get(1));
        Assert.assertNotNull(track.getBlocks().get(2));
        Assert.assertEquals(track.getBlocks().get(0).getText(), "T");
        Assert.assertEquals(track.getBlocks().get(1).getText(), "[GC]");
        Assert.assertEquals(track.getBlocks().get(2).getText(), "N");
    }

}
