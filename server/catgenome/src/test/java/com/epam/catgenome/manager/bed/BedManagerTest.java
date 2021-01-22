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
import java.util.Map;
import java.util.function.Predicate;

import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.exception.FeatureIndexException;
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
import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
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

import htsjdk.tribble.TribbleException;
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

    public static final String GENES_SORTED_BED_PATH = "classpath:templates/genes_sorted.bed";
    public static final String NARROWPEAK_BED_PATH = "classpath:templates/NarrowPeak.narrowPeak";
    public static final String BROADPEAK_BED_PATH = "classpath:templates/BroadPeak.broadPeak";
    public static final String GAPPEDPEAK_BED_PATH = "classpath:templates/GappedPeak.gPk";
    public static final String TAGALIGN_BED_PATH = "classpath:templates/tagAlign.tagAlign";

    public static final String GENES_SORTED_BED_GZ_PATH = "classpath:templates/genes_sorted.bed.gz";
    public static final String PRETTY_NAME = "pretty";

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

        referenceGenomeManager.create(testReference);
        referenceId = testReference.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterBed() throws IOException, FeatureFileReadingException {
        BedFile bedFile = testRegisterBed(GENES_SORTED_BED_PATH);
        Assert.assertTrue(testLoadBedRecords(bedFile));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterNarrowPeak() throws IOException, FeatureFileReadingException {
        BedFile bedFile = testRegisterBed(NARROWPEAK_BED_PATH);
        Assert.assertTrue(
                testLoadMultiFormatBedRecords(
                        bedFile,
                        b -> {
                            Map<String, Object> additional = b.getAdditional();
                            return !additional.isEmpty()
                            && additional.containsKey("peak")
                            && additional.containsKey("pValue")
                            && additional.containsKey("qValue")
                            && additional.containsKey("signalValue")
                            && additional.containsKey("strand")
                            && additional.containsKey("name")
                            && additional.containsKey("score");
                        }
                )
        );
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterBroadPeak() throws IOException, FeatureFileReadingException {
        BedFile bedFile = testRegisterBed(BROADPEAK_BED_PATH);
        Assert.assertTrue(
                testLoadMultiFormatBedRecords(
                        bedFile,
                        b -> {
                            Map<String, Object> additional = b.getAdditional();
                            return !additional.isEmpty()
                                    && additional.containsKey("pValue")
                                    && additional.containsKey("qValue")
                                    && additional.containsKey("signalValue")
                                    && additional.containsKey("strand")
                                    && additional.containsKey("name")
                                    && additional.containsKey("score");
                        }
                )
        );
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterTagAlign() throws IOException, FeatureFileReadingException {
        BedFile bedFile = testRegisterBed(TAGALIGN_BED_PATH);
        Assert.assertTrue(
                testLoadMultiFormatBedRecords(
                        bedFile,
                        b -> {
                            Map<String, Object> additional = b.getAdditional();
                            return !additional.isEmpty()
                                    && additional.containsKey("strand")
                                    && additional.containsKey("sequence")
                                    && additional.containsKey("score");
                        }
                )
        );
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterGappedPeakWithSmallExtension() throws IOException, FeatureFileReadingException {
        BedFile bedFile = testRegisterBed(GAPPEDPEAK_BED_PATH);
        Assert.assertTrue(
                testLoadMultiFormatBedRecords(
                        bedFile,
                        b -> {
                            Map<String, Object> additional = b.getAdditional();
                            return !additional.isEmpty()
                                    && additional.containsKey("thickStart")
                                    && additional.containsKey("pValue")
                                    && additional.containsKey("qValue")
                                    && additional.containsKey("signalValue")
                                    && additional.containsKey("strand")
                                    && additional.containsKey("name")
                                    && additional.containsKey("thickEnd")
                                    && additional.containsKey("itemRgb")
                                    && additional.containsKey("blockCount")
                                    && additional.containsKey("blockSizes")
                                    && additional.containsKey("blockStarts")
                                    && additional.containsKey("score");
                        }
                )
        );
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterZippedBed() throws IOException, FeatureFileReadingException {
        BedFile bedFile = testRegisterBed(GENES_SORTED_BED_GZ_PATH);
        Assert.assertTrue(testLoadBedRecords(bedFile));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadHistogram()
            throws IOException, HistogramReadingException {
        BedFile bedFile = testRegisterBed(GENES_SORTED_BED_PATH);

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

            bedFile = bedFileManager.load(bedFile.getId());
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
        request.setPrettyName(PRETTY_NAME);

        BedFile bedFile = bedManager.registerBed(request);
        Assert.assertNotNull(bedFile);

        BedFile loadedBedFile = bedFileManager.load(bedFile.getId());
        Assert.assertNotNull(loadedBedFile.getId());
        Assert.assertNotNull(loadedBedFile.getBioDataItemId());
        Assert.assertEquals(PRETTY_NAME, loadedBedFile.getPrettyName());
        Assert.assertNotNull(loadedBedFile.getIndex());
        Assert.assertFalse(loadedBedFile.getPath().isEmpty());
        Assert.assertFalse(loadedBedFile.getIndex().getPath().isEmpty());

        return loadedBedFile;
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

    private boolean testLoadMultiFormatBedRecords(BedFile bedFile, Predicate<BedRecord> check)
            throws FeatureFileReadingException {
        Track<BedRecord> track = new Track<>();
        track.setScaleFactor(FULL_QUERY_SCALE_FACTOR);
        track.setStartIndex(1);
        track.setEndIndex(TEST_END_INDEX);
        track.setChromosome(testChromosome);
        track.setId(bedFile.getId());

        track = bedManager.loadFeatures(track);
        Assert.assertFalse(track.getBlocks().isEmpty());
        return track.getBlocks().stream().allMatch(check);
    }


    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testUnregisterBedFile() throws Exception {
        BedFile bedFile = testRegisterBed(GENES_SORTED_BED_PATH);

        bedManager.unregisterBedFile(bedFile.getId());
        Assert.assertNull(bedFileManager.load(bedFile.getId()));
        List<BiologicalDataItem> dataItems = biologicalDataItemDao.loadBiologicalDataItemsByIds(
            Arrays.asList(bedFile.getBioDataItemId(), bedFile.getIndex().getId()));
        Assert.assertTrue(dataItems.isEmpty());

        File dir = new File(baseDirPath + "/42/bed/" + bedFile.getId());
        Assert.assertFalse(dir.exists());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterUnsorted()
            throws IOException, FeatureFileReadingException {
        String unsortedBed = "unsorted.bed";
        testRegisterInvalidBed("classpath:templates/invalid/" + unsortedBed,
                MessageHelper.getMessage(MessagesConstants.ERROR_UNSORTED_FILE));
        //check that name is not reserved
        Assert.assertTrue(biologicalDataItemDao
                .loadFilesByNameStrict(unsortedBed).isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterInvalidChromosome()
            throws IOException, FeatureFileReadingException {
        BedFile bedFile = testRegisterBed("classpath:templates/invalid/extra_chr.bed");
        Assert.assertTrue(testLoadBedRecords(bedFile));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testDeleteBedWithIndex() throws IOException, InterruptedException, FeatureIndexException {
        Resource resource = context.getResource(GENES_SORTED_BED_PATH);
        BedFile bedFile = registerTestBed(resource, referenceId, bedManager);
        Assert.assertNotNull(bedFile);
        Assert.assertNotNull(bedFile.getId());
        try {
            referenceGenomeManager.updateReferenceAnnotationFile(referenceId, bedFile.getBioDataItemId(), false);
            bedFileManager.delete(bedFile);
            //expected exception
        } catch (IllegalArgumentException e) {
            //remove file correctly as expected
            referenceGenomeManager.updateReferenceAnnotationFile(referenceId, bedFile.getBioDataItemId(), true);
            bedFileManager.delete(bedFile);
        }
    }

    public static BedFile registerTestBed(Resource bedFile, Long referenceId, BedManager bedManager)
            throws IOException {
        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(bedFile.getFile().getAbsolutePath());

        return bedManager.registerBed(request);
    }

    private void testRegisterInvalidBed(String path, String expectedMessage) throws IOException {
        String errorMessage = "";
        try {
            Resource resource = context.getResource(path);
            IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
            request.setPath(resource.getFile().getAbsolutePath());
            request.setReferenceId(referenceId);
            bedManager.registerBed(request);
        } catch (TribbleException | IllegalArgumentException | AssertionError e) {
            errorMessage = e.getMessage();
        }
        //check that we received an appropriate message
        Assert.assertTrue(errorMessage.contains(expectedMessage));
    }
}
