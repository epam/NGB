/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.manager.externaldb;

import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.Alignment;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.Dasalignment;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.PdbBlock;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.Segment;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.Dataset;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.Record;
import com.epam.catgenome.manager.externaldb.pdb.PdbDataManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Source:
 * Created:     7/26/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 *
 * @author Semen_Dmitriev
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class PdbDataManagerTest {
    private static final String CLASSIFICATION = "Transferase/Transcription";
    private static final String COMPOUND = "Histone acetyltransferase p300";
    private static final String STRUCTURE_ID = "2K8F";
    private static final String CHAIN_ID = "A";
    private static final String STRUCTURE_TITLE = "Structural Basis for the Regulation of p53 Function by p300";

    @Spy
    private PdbDataManager pdbDataManager;

    @Test
    public void testParse() throws ExternalDbUnavailableException {
        final Dataset dataset = pdbDataManager.fetchRCSBEntry(STRUCTURE_ID);
        assertNotNull(dataset);
        assertNotNull(dataset.getRecord());

        final List<Record> recordList = dataset.getRecord();
        assertFalse(recordList.isEmpty());

        final Record record = recordList.get(0);
        assertNotNull(record);
        assertEquals(record.getChainId(), CHAIN_ID);
        assertEquals(record.getClassification(), CLASSIFICATION);
        assertEquals(record.getCompound(), COMPOUND);
        assertEquals(record.getStructureId(), STRUCTURE_ID);
        assertEquals(record.getStructureTitle(), STRUCTURE_TITLE);
    }

    @Test
    public void testParseMapPdp() throws ExternalDbUnavailableException {
        final Dasalignment dataset = pdbDataManager.fetchPdbMapEntry(STRUCTURE_ID);
        assertNotNull(dataset);
        assertNotNull(dataset.getAlignment());

        final List<Alignment> alignmentList = dataset.getAlignment();
        assertFalse(alignmentList.isEmpty());

        final Alignment alignment = alignmentList.get(0);
        assertNotNull(alignment);
        assertNotNull(alignment.getBlock());
        assertFalse(alignment.getBlock().isEmpty());

        final PdbBlock pdbBlock = alignment.getBlock().get(0);
        assertNotNull(pdbBlock);
        assertNotNull(pdbBlock.getSegment());
        assertFalse(pdbBlock.getSegment().isEmpty());

        final Segment segment = pdbBlock.getSegment().get(0);
        assertNotNull(segment);
        assertNotNull(segment.getIntObjectId());
        assertNotNull(segment.getStart());
        assertNotNull(segment.getEnd());
    }
}
