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

package com.epam.catgenome.manager.bed;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.server.Server;
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
import com.epam.catgenome.controller.util.UrlTestingUtils;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.bed.BedFile;
import com.epam.catgenome.entity.bed.BedRecord;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.exception.FeatureFileReadingException;
import com.epam.catgenome.exception.HistogramReadingException;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;

/**
 * Source:      BedManagerTest
 * Created:     17.05.16, 18:09
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BedManagerTest extends AbstractManagerTest {
    @Autowired
    private BedManager bedManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private BedFileManager bedFileManager;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private ApplicationContext context;

    @Value("#{catgenome['files.base.directory.path']}")
    private String baseDirPath;

    private static final int TEST_END_INDEX = 239107476;
    private static final Double FULL_QUERY_SCALE_FACTOR = 1D;
    private static final int TEST_CHROMOSOME_SIZE = 239107476;
    private long referenceId;
    private Reference testReference;
    private Chromosome testChromosome;

    @Before
    public void setup() throws Exception {
        testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        testReference = EntityHelper.createNewReference(testChromosome, referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(testReference);
        referenceId = testReference.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterBed() throws IOException, FeatureFileReadingException {
        BedFile bedFile = testRegisterBed("classpath:templates/genes_sorted.bed");
        Assert.assertTrue(testLoadBedRecords(bedFile));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterZippedBed() throws IOException, FeatureFileReadingException {
        BedFile bedFile = testRegisterBed("classpath:templates/genes_sorted.bed.gz");
        Assert.assertTrue(testLoadBedRecords(bedFile));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadHistogram()
            throws IOException, HistogramReadingException {
        BedFile bedFile = testRegisterBed("classpath:templates/genes_sorted.bed");

        Track<Wig> histogram = new Track<>();
        histogram.setId(bedFile.getId());
        histogram.setChromosome(testChromosome);

        bedManager.loadHistogram(histogram);
        Assert.assertFalse(histogram.getBlocks().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterUrl() throws Exception {
        String bedUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + "/genes_sorted.bed";
        String indexUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + "/genes_sorted.bed.tbi";

        Server server = UrlTestingUtils.getFileServer(context);
        try {
            server.start();

            IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
            request.setPath(bedUrl);
            request.setType(BiologicalDataItemResourceType.URL);
            request.setIndexType(BiologicalDataItemResourceType.URL);
            request.setIndexPath(indexUrl);
            request.setReferenceId(referenceId);

            BedFile bedFile = bedManager.registerBed(request);
            Assert.assertNotNull(bedFile);

            bedFile = bedFileManager.loadBedFile(bedFile.getId());
            Assert.assertNotNull(bedFile.getId());
            Assert.assertNotNull(bedFile.getBioDataItemId());
            Assert.assertNotNull(bedFile.getIndex());
            Assert.assertFalse(bedFile.getPath().isEmpty());
            Assert.assertFalse(bedFile.getIndex().getPath().isEmpty());

            testLoadBedRecords(bedFile);
        } finally {
            server.stop();
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testUrlNotRegistered() throws Exception {
        String bedUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + "/genes_sorted.bed";
        String indexUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + "/genes_sorted.bed.tbi";

        Server server = UrlTestingUtils.getFileServer(context);
        try {
            server.start();

            Track<BedRecord> track = new Track<>();
            track.setScaleFactor(FULL_QUERY_SCALE_FACTOR);
            track.setStartIndex(1);
            track.setEndIndex(TEST_END_INDEX);
            track.setChromosome(testChromosome);

            track = bedManager.loadFeatures(track, bedUrl, indexUrl);
            Assert.assertFalse(track.getBlocks().isEmpty());
            track.getBlocks().stream().forEach(b -> {
                if (b.getThickStart() != null) {
                    Assert.assertEquals(b.getStartIndex(), b.getThickStart());
                }
                if (b.getThickEnd() != null) {
                    Assert.assertEquals(b.getEndIndex(), b.getThickEnd());
                }
            });
        } finally {
            server.stop();
        }
    }

    private BedFile testRegisterBed(String path) throws IOException {
        Resource resource = context.getResource(path);

        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(resource.getFile().getAbsolutePath());
        request.setReferenceId(referenceId);

        BedFile bedFile = bedManager.registerBed(request);
        Assert.assertNotNull(bedFile);

        List<BedFile> bedFiles = bedFileManager.loadBedFilesByReferenceId(referenceId);
        Assert.assertFalse(bedFiles.isEmpty());
        bedFiles.forEach(f -> {
            Assert.assertNotNull(f.getId());
            Assert.assertNotNull(f.getBioDataItemId());
            Assert.assertNotNull(f.getIndex());
            Assert.assertFalse(f.getPath().isEmpty());
            Assert.assertFalse(f.getIndex().getPath().isEmpty());
        });

        return bedFile;
    }

    private boolean testLoadBedRecords(BedFile bedFile) throws FeatureFileReadingException {
        Track<BedRecord> track = new Track<>();
        track.setScaleFactor(FULL_QUERY_SCALE_FACTOR);
        track.setStartIndex(1);
        track.setEndIndex(TEST_END_INDEX);
        track.setChromosome(testChromosome);
        track.setId(bedFile.getId());

        track = bedManager.loadFeatures(track);
        Assert.assertFalse(track.getBlocks().isEmpty());
        track.getBlocks().stream().forEach(b -> {
            if (b.getThickStart() != null) {
                Assert.assertEquals(b.getStartIndex(), b.getThickStart());
            }
            if (b.getThickEnd() != null) {
                Assert.assertEquals(b.getEndIndex(), b.getThickEnd());
            }
        });

        return true;
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testUnregisterBedFile() throws Exception {
        BedFile bedFile = testRegisterBed("classpath:templates/genes_sorted.bed");

        bedManager.unregisterBedFile(bedFile.getId());
        Assert.assertNull(bedFileManager.loadBedFile(bedFile.getId()));
        List<BiologicalDataItem> dataItems = biologicalDataItemDao.loadBiologicalDataItemsByIds(
            Arrays.asList(bedFile.getBioDataItemId(), bedFile.getIndex().getId()));
        Assert.assertTrue(dataItems.isEmpty());

        File dir = new File(baseDirPath + "/42/bed/" + bedFile.getId());
        Assert.assertFalse(dir.exists());
    }
}
