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

package com.epam.catgenome.manager.maf;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.maf.MafFile;
import com.epam.catgenome.entity.maf.MafRecord;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;

/**
 * Source:      MafManagerTest
 * Created:     07.07.16, 15:30
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class MafManagerTest extends AbstractManagerTest {
    @Autowired
    private MafManager mafManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private ApplicationContext context;

    @Value("#{catgenome['files.base.directory.path']}")
    private String baseDirPath;

    private static final int TEST_END_INDEX = 247812431;
    private static final Double FULL_QUERY_SCALE_FACTOR = 1D;
    private static final int TEST_CHROMOSOME_SIZE = 248956422;
    private static final int CHROMOSOME_COUNT = 23;
    private long referenceId;
    private Reference testReference;
    private Chromosome testChromosome;

    @Before
    public void setup() throws Exception {
        testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setName("1");
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);

        List<Chromosome> chromosomes = new ArrayList<>();
        chromosomes.add(testChromosome);
        for (int i = 2; i < CHROMOSOME_COUNT; i++) {
            Chromosome chromosome = EntityHelper.createNewChromosome();
            chromosome.setName(Integer.toString(i));
            chromosome.setSize(TEST_CHROMOSOME_SIZE);

            chromosomes.add(chromosome);
        }

        Chromosome chromosome = EntityHelper.createNewChromosome();
        chromosome.setName("X");
        chromosome.setSize(TEST_CHROMOSOME_SIZE);
        chromosomes.add(chromosome);

        chromosome = EntityHelper.createNewChromosome();
        chromosome.setName("Y");
        chromosome.setSize(TEST_CHROMOSOME_SIZE);
        chromosomes.add(chromosome);

        testReference = EntityHelper.createNewReference(chromosomes, referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(testReference);
        referenceId = testReference.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterMaf() throws IOException, NoSuchAlgorithmException {
        // sort a file: sort -t$'\t' -k 5,5 -k 6,6n TCGA.GBM.mutect.4b176a7b-a5c3-457e-af95-992018b6f3d7.somatic.maf -o
        // TCGA.GBM.mutect.4b176a7b-a5c3-457e-af95-992018b6f3d7.somatic.sorted.maf
        Resource resource = context.getResource("classpath:templates/maf/" +
                "TCGA.ACC.mutect.abbe72a5-cb39-48e4-8df5-5fd2349f2bb2.somatic.sorted.maf.gz");

        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(resource.getFile().getAbsolutePath());
        request.setReferenceId(referenceId);

        MafFile mafFile = mafManager.registerMafFile(request);
        Assert.assertNotNull(mafFile);

        Track<MafRecord> track = new Track<>();
        track.setScaleFactor(FULL_QUERY_SCALE_FACTOR);
        track.setStartIndex(1);
        track.setEndIndex(TEST_END_INDEX);
        track.setChromosome(testChromosome);
        track.setId(mafFile.getId());

        track = mafManager.loadFeatures(track);
        Assert.assertFalse(track.getBlocks().isEmpty());

        Track<MafRecord> shortTrack = new Track<>();
        shortTrack.setScaleFactor(FULL_QUERY_SCALE_FACTOR);
        shortTrack.setStartIndex(1);
        shortTrack.setEndIndex(track.getBlocks().get(2).getEndIndex() + 1);
        shortTrack.setChromosome(testChromosome);
        shortTrack.setId(mafFile.getId());

        shortTrack = mafManager.loadFeatures(shortTrack);
        Assert.assertFalse(shortTrack.getBlocks().isEmpty());

        // Test BigMaf
        resource = context.getResource("classpath:templates/maf");
        request.setPath(resource.getFile().getAbsolutePath());
        request.setName("Big Maf");
        //request.setPath("/home/kite/Documents/sampleData/maf");

        mafFile = mafManager.registerMafFile(request);
        Assert.assertNotNull(mafFile);

        track = new Track<>();
        track.setScaleFactor(FULL_QUERY_SCALE_FACTOR);
        track.setStartIndex(1);
        track.setEndIndex(TEST_END_INDEX);
        track.setChromosome(testChromosome);
        track.setId(mafFile.getId());

        track = mafManager.loadFeatures(track);
        Assert.assertFalse(track.getBlocks().isEmpty());

        shortTrack = new Track<>();
        shortTrack.setScaleFactor(FULL_QUERY_SCALE_FACTOR);
        shortTrack.setStartIndex(1);
        shortTrack.setEndIndex(track.getBlocks().get(2).getEndIndex() + 1);
        shortTrack.setChromosome(testChromosome);
        shortTrack.setId(mafFile.getId());

        shortTrack = mafManager.loadFeatures(shortTrack);
        Assert.assertFalse(shortTrack.getBlocks().isEmpty());

        mafManager.updateMafFile(mafFile.getId());
        track = mafManager.loadFeatures(track);
        Assert.assertFalse(track.getBlocks().isEmpty());

        mafManager.unregisterMafFile(mafFile.getId());

        File dir = new File(baseDirPath + "/42/maf/" + mafFile.getId());
        Assert.assertFalse(dir.exists());
    }
}
