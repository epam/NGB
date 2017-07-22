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

package com.epam.catgenome.manager.wig;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.vo.registration.FileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.entity.wig.WigFile;
import com.epam.catgenome.exception.FeatureFileReadingException;
import com.epam.catgenome.manager.reference.ReferenceManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Source:      BedGraphProcessorTest.java
 * Created:     1/26/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BedGraphProcessorTest extends AbstractManagerTest {

    public static final String PRETTY_NAME = "pretty";

    @Autowired
    ApplicationContext context;

    @Autowired
    private FacadeWigManager wigManager;

    @Autowired
    private WigFileManager wigFileManager;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private ReferenceManager referenceManager;

    private static final String TEST_NSAME = "BIG " + BedGraphProcessorTest.class.getSimpleName();
    private static final String TEST_REF = "/dm606.X.fa";
    private static final String TEST_BDG = "/bedGraph.bdg";
    private static final String TEST_BDG_GZ = "/bedGraph.bdg.gz";
    private Resource resource;
    private Reference testReference;
    private long testChromosomeId;
    private String chromosomeName = "X";
    private static final int TEST_START_INDEX = 1;
    private static final int TEST_END_INDEX = 788486;
    private static final double TEST_SCALE_FACTOR = 0.01;

    @Before
    public void setup() throws IOException {

        resource = context.getResource("classpath:templates");
        File fastaFile = new File(resource.getFile().getAbsolutePath() + TEST_REF);
        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName(TEST_NSAME);
        request.setPath(fastaFile.getPath());
        testReference = referenceManager.registerGenome(request);

        List<Chromosome> chromosomeList = testReference.getChromosomes();
        for (Chromosome chromosome : chromosomeList) {
            if (chromosome.getName().equals(chromosomeName)) {
                testChromosomeId = chromosome.getId();
                break;
            }
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveBedGraphTest() throws IOException, NoSuchAlgorithmException {
        final String path = resource.getFile().getAbsolutePath() + TEST_BDG;
        FileRegistrationRequest request = new FileRegistrationRequest();
        request.setPath(path);
        request.setReferenceId(testReference.getId());
        request.setName(TEST_BDG);
        request.setPrettyName(PRETTY_NAME);

        WigFile wigFile = wigManager.registerWigFile(request);
        Assert.assertNotNull(wigFile);
        WigFile loadWigFile = wigFileManager.loadWigFile(wigFile.getId());
        Assert.assertNotNull(loadWigFile);
        Assert.assertTrue(wigFile.getId().equals(loadWigFile.getId()));
        Assert.assertTrue(wigFile.getName().equals(loadWigFile.getName()));
        Assert.assertTrue(wigFile.getCreatedBy().equals(loadWigFile.getCreatedBy()));
        Assert.assertTrue(wigFile.getCreatedDate().equals(loadWigFile.getCreatedDate()));
        Assert.assertTrue(wigFile.getReferenceId().equals(loadWigFile.getReferenceId()));
        Assert.assertTrue(wigFile.getPath().equals(loadWigFile.getPath()));
        Assert.assertEquals(wigFile.getPrettyName(), loadWigFile.getPrettyName());

        Track<Wig> wigTrack = new Track<>();
        wigTrack.setChromosome(new Chromosome(testChromosomeId));
        wigTrack.setStartIndex(TEST_START_INDEX);
        wigTrack.setEndIndex(TEST_END_INDEX);
        wigTrack.setScaleFactor(TEST_SCALE_FACTOR);
        wigTrack.setId(loadWigFile.getId());
        wigManager.getWigTrack(wigTrack);
        Assert.assertFalse(wigTrack.getBlocks().isEmpty());
        Assert.assertTrue(wigTrack.getBlocks().stream().allMatch(wig -> wig.getValue() != null));

        wigManager.unregisterWigFile(loadWigFile.getId());
        loadWigFile = wigFileManager.loadWigFile(wigFile.getId());
        Assert.assertNull(loadWigFile);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveBedGraphGzTest() throws IOException, NoSuchAlgorithmException {
        final String path = resource.getFile().getAbsolutePath() + TEST_BDG_GZ;
        FileRegistrationRequest request = new FileRegistrationRequest();
        request.setPath(path);
        request.setReferenceId(testReference.getId());
        request.setName(TEST_BDG_GZ);
        request.setPrettyName(PRETTY_NAME);

        WigFile wigFile = wigManager.registerWigFile(request);
        Assert.assertNotNull(wigFile);
        WigFile loadWigFile = wigFileManager.loadWigFile(wigFile.getId());
        Assert.assertNotNull(loadWigFile);
        Assert.assertTrue(wigFile.getId().equals(loadWigFile.getId()));
        Assert.assertTrue(wigFile.getName().equals(loadWigFile.getName()));
        Assert.assertTrue(wigFile.getCreatedBy().equals(loadWigFile.getCreatedBy()));
        Assert.assertTrue(wigFile.getCreatedDate().equals(loadWigFile.getCreatedDate()));
        Assert.assertTrue(wigFile.getReferenceId().equals(loadWigFile.getReferenceId()));
        Assert.assertTrue(wigFile.getPath().equals(loadWigFile.getPath()));
        Assert.assertEquals(wigFile.getPrettyName(), loadWigFile.getPrettyName());

        Track<Wig> wigTrack = new Track<>();
        wigTrack.setChromosome(new Chromosome(testChromosomeId));
        wigTrack.setStartIndex(TEST_START_INDEX);
        wigTrack.setEndIndex(TEST_END_INDEX);
        wigTrack.setScaleFactor(TEST_SCALE_FACTOR);
        wigTrack.setId(loadWigFile.getId());
        wigManager.getWigTrack(wigTrack);
        Assert.assertFalse(wigTrack.getBlocks().isEmpty());
        Assert.assertTrue(wigTrack.getBlocks().stream().allMatch(wig -> wig.getValue() != null));

        wigManager.unregisterWigFile(loadWigFile.getId());
        loadWigFile = wigFileManager.loadWigFile(wigFile.getId());
        Assert.assertNull(loadWigFile);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterWrongFormat()
            throws IOException, FeatureFileReadingException {

        String invalidBedGraph = "invalid.bdgg";
        Assert.assertTrue(testRegisterInvalidFile("classpath:templates/invalid/" + invalidBedGraph));
        //check that name is not reserved
        Assert.assertTrue(biologicalDataItemDao
                .loadFilesByNameStrict(invalidBedGraph).isEmpty());
    }

    private boolean testRegisterInvalidFile(String path) throws IOException {
        try {
            Resource resource = context.getResource(path);
            FileRegistrationRequest request = new FileRegistrationRequest();
            request.setPath(resource.getFile().getAbsolutePath());
            request.setReferenceId(testReference.getId());
            wigManager.registerWigFile(request);
        } catch (IllegalArgumentException | AssertionError e) {
            return true;
        }
        return false;
    }
}
