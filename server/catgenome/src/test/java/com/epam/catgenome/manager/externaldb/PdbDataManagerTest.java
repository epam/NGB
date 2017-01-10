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

package com.epam.catgenome.manager.externaldb;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.Alignment;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.Dasalignment;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.PdbBlock;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.Segment;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.Dataset;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.Record;

/**
 * Source:
 * Created:     7/26/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 *
 * @author Semen_Dmitriev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class PdbDataManagerTest {

    private static final String TRANSFERASE_TRANSCRIPTION = "Transferase/Transcription";
    private static final String CLASSIFICATION = TRANSFERASE_TRANSCRIPTION;
    private static final String COMPOUND = "Histone acetyltransferase p300";
    private static final String STRUCTURE_ID = "2K8F";
    private static final String CHAIN_ID = "A";
    private static final String STRUCTURE_TITLE = "Structural Basis for the Regulation of p53 Function by p300";
    @Autowired
    private ApplicationContext context;

    @Mock
    private HttpDataManager httpDataManager;

    @InjectMocks
    @Spy
    private PdbDataManager pdbDataManager;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testParse() throws IOException, ExternalDbUnavailableException, InterruptedException, JAXBException {
        // arrange
        String fetchRes = readFile("pbd_id_2k8d.xml");

        Mockito.when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(ParameterNameValue[].class))
        ).thenReturn(fetchRes);

        // act
        final Dataset dataset = pdbDataManager.fetchRCSBEntry("STRUCTURE_ID");
        Assert.assertNotNull(dataset);
        Assert.assertNotNull(dataset.getRecord());
        final List<Record> recordList = dataset.getRecord();
        Assert.assertFalse(recordList.isEmpty());
        final Record record = recordList.get(0);
        Assert.assertNotNull(record);
        Assert.assertTrue(record.getChainId().equals(CHAIN_ID));
        Assert.assertTrue(record.getClassification().equals(CLASSIFICATION));
        Assert.assertTrue(record.getCompound().equals(COMPOUND));
        Assert.assertTrue(record.getStructureId().equals(STRUCTURE_ID));
        Assert.assertTrue(record.getStructureTitle().equals(STRUCTURE_TITLE));
    }


    @Test
    public void testParseMapPdp() throws IOException, ExternalDbUnavailableException, InterruptedException,
            JAXBException {
        // arrange
        String fetchRes = readFile("pbd_map_id_2k8d.xml");

        Mockito.when(
                httpDataManager.fetchData(Mockito.any(String.class), Mockito.any(ParameterNameValue[].class))
        ).thenReturn(fetchRes);

        final Dasalignment dataset = pdbDataManager.fetchPdbMapEntry("2K8F");
        Assert.assertNotNull(dataset);
        Assert.assertNotNull(dataset.getAlignment());
        final List<Alignment> alignmentList = dataset.getAlignment();
        Assert.assertFalse(alignmentList.isEmpty());
        final Alignment alignment = alignmentList.get(0);
        Assert.assertNotNull(alignment);
        Assert.assertFalse(alignment.getBlock().isEmpty());
        Assert.assertNotNull(alignment.getBlock());
        final PdbBlock pdbBlock = alignment.getBlock().get(0);
        Assert.assertNotNull(pdbBlock);
        Assert.assertNotNull(pdbBlock.getSegment());
        Assert.assertFalse(pdbBlock.getSegment().isEmpty());
        final Segment segment = pdbBlock.getSegment().get(0);
        Assert.assertNotNull(segment);
        Assert.assertNotNull(segment.getIntObjectId());
        Assert.assertNotNull(segment.getStart());
        Assert.assertNotNull(segment.getEnd());
    }

    private String readFile(String filename) throws IOException {
        Resource resource = context.getResource("classpath:externaldb//data//" + filename);
        String pathStr = resource.getFile().getPath();
        return new String(Files.readAllBytes(Paths.get(pathStr)), Charset.defaultCharset());
    }
}
