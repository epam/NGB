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

package com.epam.catgenome.manager.seg;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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

import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.seg.SegFile;
import com.epam.catgenome.entity.seg.SegRecord;
import com.epam.catgenome.entity.track.SampledTrack;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;

/**
 * Source:      SegManagerTest
 * Created:     10.06.16, 16:43
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class SegManagerTest {
    @Autowired
    private SegManager segManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private SegFileManager segFileManager;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private ApplicationContext context;

    private static final int TEST_END_INDEX = 247812431;
    private static final Double FULL_QUERY_SCALE_FACTOR = 1D;
    private static final int TEST_CHROMOSOME_SIZE = 247812500;
    private long referenceId;
    private Reference testReference;
    private Chromosome testChromosome;

    @Value("#{catgenome['files.base.directory.path']}")
    private String baseDirPath;

    @Before
    public void setup() throws Exception {
        testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setName("1");
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        testReference = EntityHelper.createNewReference(testChromosome, referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(testReference);
        referenceId = testReference.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterSeg() throws IOException, InterruptedException, NoSuchAlgorithmException {
        SegFile segFile = testRegisterSeg("classpath:templates/test_seg.seg");

        segManager.unregisterSegFile(segFile.getId());
        SegFile loadedSeg = segFileManager.loadSegFile(segFile.getId());
        Assert.assertNull(loadedSeg);
        List<BiologicalDataItem> dataItems = biologicalDataItemDao.loadBiologicalDataItemsByIds(
                Arrays.asList(segFile.getBioDataItemId(), segFile.getIndex().getId()));
        Assert.assertTrue(dataItems.isEmpty());

        File dir = new File(baseDirPath + "/42/seg/" + segFile.getId());
        Assert.assertFalse(dir.exists());

    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterSegCompressed() throws IOException, InterruptedException, NoSuchAlgorithmException {
        SegFile segFile = testRegisterSeg("classpath:templates/test_seg.seg.gz");
        Assert.assertNotNull(segFile);
    }

    private SegFile testRegisterSeg(String path) throws IOException, InterruptedException, NoSuchAlgorithmException {
        Resource resource = context.getResource(path);

        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(resource.getFile().getAbsolutePath());
        request.setReferenceId(referenceId);

        SegFile segFile = segManager.registerSegFile(request);
        Assert.assertNotNull(segFile);

        List<SegFile> bedFiles = segFileManager.loadSedFilesByReferenceId(referenceId);
        Assert.assertFalse(bedFiles.isEmpty());
        bedFiles.forEach(f -> {
            Assert.assertNotNull(f.getId());
            Assert.assertNotNull(f.getBioDataItemId());
            Assert.assertNotNull(f.getIndex());
            Assert.assertFalse(f.getPath().isEmpty());
        });

        SampledTrack<SegRecord> sampledTrack = new SampledTrack<>();
        sampledTrack.setScaleFactor(FULL_QUERY_SCALE_FACTOR);
        sampledTrack.setStartIndex(1);
        sampledTrack.setEndIndex(TEST_END_INDEX);
        sampledTrack.setChromosome(testChromosome);
        sampledTrack.setId(segFile.getId());

        sampledTrack = segManager.loadFeatures(sampledTrack);
        Assert.assertFalse(sampledTrack.getTracks().isEmpty());
        Assert.assertTrue(sampledTrack.getTracks().values().parallelStream().filter(t -> !t.isEmpty()).findAny()
                .isPresent());

        return segFile;
    }
}
