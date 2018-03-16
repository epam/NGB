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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.bam.PSLRecord;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.Species;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.epam.catgenome.manager.externaldb.ParameterNameValue;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.reference.ReferenceManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    private static final String TEST_REF_NAME = "//dm606.X.fa";

    @Autowired
    private ApplicationContext context;

    @Mock
    private HttpDataManager httpDataManager;

    @Autowired
    @InjectMocks
    private BlatSearchManager blatSearchManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private ReferenceManager referenceManager;

    private Reference testReference;

    @Before
    public void setUp() throws Exception {
        Resource resource = context.getResource("classpath:templates");
        File fastaFile = new File(resource.getFile().getAbsolutePath() + TEST_REF_NAME);

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName(TEST_REF_NAME + biologicalDataItemDao.createBioItemId());
        request.setPath(fastaFile.getPath());

        testReference = referenceManager.registerGenome(request);

        Mockito.when(
            httpDataManager.fetchData(Mockito.any(), Mockito.any(ParameterNameValue[].class))
        ).thenReturn(readMockedResponse());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testFind() throws IOException, ExternalDbUnavailableException {
        List<PSLRecord> actual = blatSearchManager.find(TEST_SEQUENSE, TEST_SPECIES);
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(EXPECTED.get(0), actual.get(0));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testFindBlatReadSequence() throws IOException, ExternalDbUnavailableException {
        Species testSpecies = new Species();
        testSpecies.setName("human");
        testSpecies.setVersion("hg19");

        referenceGenomeManager.registerSpecies(testSpecies);
        referenceGenomeManager.updateSpecies(testReference.getId(), testSpecies.getVersion());

        String readSequence = "CAGTATCGTCCTTACTATTACATAGTGTGGTAGCGATGCAGTCCCAGTGAAAAAAAAAAAAAAAAAAAC";
        List<PSLRecord> records = blatSearchManager.findBlatReadSequence(testReference.getId(), readSequence);
        Assert.assertNotNull(records);
        Assert.assertEquals(1, records.size());
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testFindBlatReadSequenceEmptySpecies() throws IOException, ExternalDbUnavailableException {
        referenceGenomeManager.updateSpecies(testReference.getId(), "hg19");
        String readSequence = "CAGTATCGTCCTTACTATTACATAGTGTGGTAGCGATGCAGTCCCAGTGAAAAAAAAAAAAAAAAAAAC";
        blatSearchManager.findBlatReadSequence(testReference.getId(), readSequence);
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

    private String readMockedResponse() throws IOException {
        Resource resource = context.getResource("classpath:blat//data//testResponse.html");
        String pathStr = resource.getFile().getPath();
        return new String(Files.readAllBytes(Paths.get(pathStr)), Charset.defaultCharset());
    }

}