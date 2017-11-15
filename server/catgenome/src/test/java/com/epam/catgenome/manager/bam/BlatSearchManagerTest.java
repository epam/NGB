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

import com.epam.catgenome.entity.bam.PSLRecord;
import com.epam.catgenome.entity.reference.Species;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BlatSearchManagerTest {

    private static final String TEST_SEQUENSE = "CAGTATCGTCCTTACTATTACATAGTGTGGTAGCGATGCAGTCCCAGTGAAAAAAAAAAAAAAAAAAAC";
    private static final Species TEST_SPECIES = new Species(){{
            setName("human");
            setVersion("hg19");
        }};
    private static final String TEST_RECORD_NAME = "YourSeq";
    private static final String TEST_CHR = "chr5";
    private static final int TEST_START = 118149214;
    private static final int TEST_END = 118149241;
    private static final int TEST_MATCH = 27;
    private static final int EMPTY = 0;
    private static final int TEST_Q_SIZE = 69;
    private static final int TEST_SCORE = 27000 / 69;
    private static final List<PSLRecord> EXPECTED = mockPSLRecord();

    @Autowired
    private BlatSearchManager blatSearchManager;

    @Test
    public void testFind() throws IOException, ExternalDbUnavailableException {
        List<PSLRecord> actual = blatSearchManager.find(TEST_SEQUENSE, TEST_SPECIES);
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(EXPECTED.get(0), actual.get(0));
    }

    private static List<PSLRecord> mockPSLRecord() {
        PSLRecord record = new PSLRecord();
        record.setName(TEST_RECORD_NAME);
        record.setChr(TEST_CHR);
        record.setStartIndex(TEST_START);
        record.setEndIndex(TEST_END);
        record.setStrand(StrandSerializable.POSITIVE);
        record.setMatch(TEST_MATCH);
        record.setMismatch(EMPTY);
        record.setRepMatch(EMPTY);
        record.setNs(EMPTY);
        record.setqGapCount(EMPTY);
        record.setqGapBases(EMPTY);
        record.settGapCount(EMPTY);
        record.setqGapBases(EMPTY);
        record.setqSize(TEST_Q_SIZE);
        record.setScore(TEST_SCORE);
        return Collections.singletonList(record);
    }

}