/*
 * MIT License
 *
 * Copyright (c) 2017-2022 EPAM Systems
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
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.util.MultipartFileSender;
import com.epam.catgenome.controller.util.ResultReference;
import com.epam.catgenome.controller.util.UrlTestingUtils;
import com.epam.catgenome.controller.vo.ReadQuery;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.bam.BamQueryOption;
import com.epam.catgenome.entity.bam.BamTrack;
import com.epam.catgenome.entity.bam.BamTrackMode;
import com.epam.catgenome.entity.bam.Read;
import com.epam.catgenome.entity.bam.TrackDirectionType;
import com.epam.catgenome.entity.bucket.Bucket;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.manager.bucket.BucketManager;
import com.epam.catgenome.manager.parallel.TaskExecutorService;
import com.epam.catgenome.manager.reference.ReferenceManager;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Source:      BamManagerTest.java
 * Created:     12/3/2015
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Semen_Dmitriev
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BamManagerTest extends AbstractManagerTest {

    private final Logger logger = LoggerFactory.getLogger(BamManagerTest.class);

    @Autowired
    ApplicationContext context;

    @Autowired
    private BucketManager bucketManager;

    @Autowired
    private BamFileManager bamFileManager;

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    @InjectMocks
    private BamManager bamManager;

    @Autowired
    private TaskExecutorService taskExecutorService;

    private static final String TEST_NSAME = "BIG " + BamManagerTest.class.getSimpleName();
    private static final String TEST_REF_NAME = "//dm606.X.fa";
    private static final String TEST_BAM_NAME = "//agnX1.09-28.trim.dm606.realign.bam";
    private static final int TEST_START_INDEX_SMALL_RANGE = 12589188;
    private static final int TEST_START_INDEX_MEDIUM_RANGE = 12589188;
    private static final int TEST_START_INDEX_LARGE_RANGE = 12582200;
    private static final int TEST_END_INDEX_SMALL_RANGE = 12589228;
    private static final int TEST_END_INDEX_MEDIUM_RANGE = 12589228;
    private static final int TEST_END_INDEX_LARGE_RANGE = 12589228;
    private static final double SCALE_FACTOR_SMALL = 1.0;
    private static final double SCALE_FACTOR_LARGE = 0.0000625;
    private static final double SCALE_FACTOR_MEDIUM = 0.0105;
    private static final int TEST_FRAME_SIZE = 30;
    private static final int LARGE_FRAME_SIZE = 12589188;
    private static final int TEST_COUNT = 30;
    private static final int LARGE_TEST_COUNT = 10000000;
    private static final String BAI_EXTENSION = ".bai";

    private static final String PRETTY_NAME = "pretty";
    private static final long WRONG_FILE_ID = 123L;
    private static final String CHROMOSOME_NAME = "X";

    private Resource resource;
    private Reference testReference;
    private Chromosome testChromosome;

    @Value("${s3.bucket.test.name}")
    private String s3BucketName;
    @Value("${s3.access.test.key}")
    private String s3AccessKey;
    @Value("${s3.secret.test.key}")
    private String s3SecretKey;
    @Value("${s3.file.path}")
    private String s3FilePath;
    @Value("${s3.index.path}")
    private String s3IndexPath;

    @Value("${hdfs.file.path}")
    private String hdfsFilePath;
    @Value("${hdfs.index.path}")
    private String hdfsIndexPath;

    @Before
    public void setup() throws IOException {
        resource = context.getResource("classpath:templates");
        File fastaFile = new File(resource.getFile().getAbsolutePath() + TEST_REF_NAME);

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName(TEST_REF_NAME + biologicalDataItemDao.createBioItemId());
        request.setPath(fastaFile.getPath());

        taskExecutorService.setForceSequential(true);

        testReference = referenceManager.registerGenome(request);
        List<Chromosome> chromosomeList = testReference.getChromosomes();
        for (Chromosome chromosome : chromosomeList) {
            if (chromosome.getName().equals(CHROMOSOME_NAME)) {
                testChromosome = chromosome;
                break;
            }
        }
    }

    private void assertTrackLoading(BamFile bamFile, Consumer<BamQueryOption> optionsSetter,
                                    Consumer<BamTrack<Read>> trackAsserter) throws IOException {
        Track<Read> fullTrackQ = getBaseReadTrack(bamFile);

        BamQueryOption option = getBaseBamQueryOption();

        optionsSetter.accept(option);

        ResponseEmitterMock emitterMock = new ResponseEmitterMock();
        bamManager.sendBamTrackToEmitter(fullTrackQ, option, emitterMock);
        BamTrack<Read> fullTrack = emitterMock.getBamTrack();

        trackAsserter.accept(fullTrack);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveBamTest() throws IOException {
        final String path = resource.getFile().getAbsolutePath() + TEST_BAM_NAME;
        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(path);
        request.setIndexPath(path + BAI_EXTENSION);
        request.setName(TEST_NSAME);
        request.setReferenceId(testReference.getId());
        request.setPrettyName(PRETTY_NAME);
        request.setType(BiologicalDataItemResourceType.FILE);

        BamFile bamFile = bamManager.registerBam(request);
        assertNotNull(bamFile);
        final BamFile loadBamFile = bamFileManager.load(bamFile.getId());
        assertNotNull(loadBamFile);
        assertEquals(bamFile.getId(), loadBamFile.getId());
        assertEquals(bamFile.getName(), loadBamFile.getName());
        assertEquals(PRETTY_NAME, bamFile.getPrettyName());
        assertEquals(bamFile.getCreatedDate(), loadBamFile.getCreatedDate());
        assertEquals(bamFile.getReferenceId(), loadBamFile.getReferenceId());
        assertEquals(bamFile.getPath(), loadBamFile.getPath());
        assertEquals(bamFile.getIndex().getPath(), loadBamFile.getIndex().getPath());

        assertTrackLoading(bamFile,
            (option) -> {},
            (track) -> {
                testBamTrack(track);
                Read testRead = track.getBlocks().get(0);
                testRead(testRead);
            });

        assertTrackLoading(bamFile, (option) -> option.setTrackDirection(TrackDirectionType.RIGHT), this::testBamTrack);

        assertTrackLoading(bamFile,
            (option) -> option.setTrackDirection(TrackDirectionType.MIDDLE), this::testBamTrack);

        assertTrackLoading(bamFile, (option) -> option.setTrackDirection(null), this::testBamTrack);

        assertTrackLoading(bamFile, (option) -> option.setShowSpliceJunction(false), this::testBamTrack);

        assertTrackLoading(bamFile, (option) -> option.setShowSpliceJunction(null), this::testBamTrack);

        assertTrackLoading(bamFile, (option) -> option.setShowClipping(false), this::testBamTrack);

        assertTrackLoading(bamFile, (option) -> option.setShowClipping(null), this::testBamTrack);

        assertTrackLoading(bamFile, (option) -> option.setFilterDuplicate(true), this::testBamTrack);

        assertTrackLoading(bamFile, (option) -> {
            option.setFilterDuplicate(false);
            option.setFilterNotPrimary(true);
        }, this::testBamTrack);

        assertTrackLoading(bamFile, (option) -> {
            option.setFilterNotPrimary(false);
            option.setFilterVendorQualityFail(true);
        }, this::testBamTrack);

        assertTrackLoading(bamFile, (option) -> {
            option.setFilterVendorQualityFail(false);
            option.setFilterSupplementaryAlignment(true);
        }, this::testBamTrack);

        assertTrackLoading(bamFile, (option) -> {
            option.setFilterSupplementaryAlignment(false);
            option.setFrame(0);
            option.setDownSampling(false);
        }, (track) -> assertNull(track.getDownsampleCoverage()));

        assertTrackLoading(bamFile, (option) -> {
            option.setFrame(-TEST_FRAME_SIZE);
            option.setDownSampling(false);
        }, (track) -> assertNull(track.getDownsampleCoverage()));

        assertTrackLoading(bamFile, (option) -> {
            option.setCount(LARGE_TEST_COUNT);
            option.setDownSampling(false);
        }, (track) -> assertNull(track.getDownsampleCoverage()));

        assertTrackLoading(bamFile, (option) -> {
            option.setFrame(LARGE_FRAME_SIZE);
            option.setDownSampling(false);
        }, (track) -> assertNull(track.getDownsampleCoverage()));

        assertTrackLoading(bamFile, (option) -> {
            option.setCount(null);
            option.setDownSampling(false);
        }, (track) -> assertNull(track.getDownsampleCoverage()));

        assertTrackLoading(bamFile, (option) -> {
            option.setCount(null);
            option.setDownSampling(false);
        }, (track) -> assertNull(track.getDownsampleCoverage()));

        assertTrackLoading(bamFile, (option) -> {
            option.setTrackDirection(TrackDirectionType.MIDDLE);
        }, (track) -> {
                testBamTrack(track);
                Read testRead = track.getBlocks().get(0);
                testRead(testRead);
            }
        );

        assertTrackLoading(bamFile, (option) -> {
            option.setTrackDirection(TrackDirectionType.LEFT);
            option.setShowClipping(false);
            option.setShowSpliceJunction(true);
        }, (track) -> {
                assertFalse(track.getBlocks().isEmpty());
                Read testRead = track.getBlocks().get(0);
                testRead(testRead);
            }
        );

        assertTrackLoading(bamFile, (option) -> {
            option.setTrackDirection(TrackDirectionType.RIGHT);
            option.setShowClipping(true);
            option.setShowSpliceJunction(false);
        }, (track) -> {
                assertFalse(track.getBlocks().isEmpty());
                Read testRead = track.getBlocks().get(0);
                testRead(testRead);
            }
        );
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveBamExceptionHandlingTest() throws IOException {
        BamFile bamFile = new BamFile();
        bamFile.setId(WRONG_FILE_ID);

        Track<Read> fullTrackQ = getBaseReadTrack(bamFile);

        BamQueryOption option = new BamQueryOption();

        ResponseEmitterMock emitterMock = new ResponseEmitterMock();
        bamManager.sendBamTrackToEmitter(fullTrackQ, option, emitterMock);

        assertEquals(emitterMock.getResultStatus(), ResultReference.ResultStatus.ERROR.toString());
        assertFalse(emitterMock.getMessage().isEmpty());
    }

    @NotNull
    private Track<Read> getBaseReadTrack(BamFile bamFile) {
        Track<Read> fullTrackQ = new Track<>();
        fullTrackQ.setStartIndex(TEST_START_INDEX_SMALL_RANGE);
        fullTrackQ.setEndIndex(TEST_END_INDEX_SMALL_RANGE);
        fullTrackQ.setScaleFactor(SCALE_FACTOR_SMALL);
        fullTrackQ.setChromosome(new Chromosome(testChromosome.getId()));
        fullTrackQ.setId(bamFile.getId());
        return fullTrackQ;
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadRead() throws IOException {
        final String path = resource.getFile().getAbsolutePath() + TEST_BAM_NAME;
        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(path);
        request.setIndexPath(path + BAI_EXTENSION);
        request.setName(TEST_NSAME);
        request.setReferenceId(testReference.getId());
        request.setType(BiologicalDataItemResourceType.FILE);

        BamFile bamFile = bamManager.registerBam(request);
        assertNotNull(bamFile);

        Track<Read> fullTrackQ = getBaseReadTrack(bamFile);

        BamQueryOption option = getBaseBamQueryOption();

        ResponseEmitterMock emitterMock = new ResponseEmitterMock();
        bamManager.sendBamTrackToEmitter(fullTrackQ, option, emitterMock);
        BamTrack<Read> fullTrack = emitterMock.getBamTrack();

        assertFalse(fullTrack.getBlocks().isEmpty());
        Read read = fullTrack.getBlocks().get(0);

        ReadQuery query = new ReadQuery();
        query.setName(read.getName());
        query.setChromosomeId(testChromosome.getId());
        query.setStartIndex(read.getStartIndex());
        query.setEndIndex(read.getEndIndex());
        query.setId(bamFile.getId());

        Read loadedRead = bamManager.loadRead(query, null, null);
        assertNotNull(loadedRead);
        assertTrue(StringUtils.isNotBlank(loadedRead.getSequence()));
        assertFalse(loadedRead.getTags().isEmpty());
        assertTrue(StringUtils.isNotBlank(loadedRead.getQualities()));
        assertEquals(read.getName(), loadedRead.getName());
    }

    @NotNull
    private BamQueryOption getBaseBamQueryOption() {
        BamQueryOption option = new BamQueryOption();
        option.setTrackDirection(TrackDirectionType.LEFT);
        option.setShowSpliceJunction(true);
        option.setShowClipping(true);
        option.setFrame(TEST_FRAME_SIZE);
        option.setCount(TEST_COUNT);
        return option;
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadLocalNonRegisteredBam() throws Exception {
        final String bamPath = resource.getFile().getAbsolutePath() + TEST_BAM_NAME;
        final String indexPath = bamPath + BAI_EXTENSION;
        Track<Read> fullTrackQ = new Track<>();
        fullTrackQ.setStartIndex(TEST_START_INDEX_SMALL_RANGE);
        fullTrackQ.setEndIndex(TEST_END_INDEX_SMALL_RANGE);
        fullTrackQ.setScaleFactor(SCALE_FACTOR_SMALL);
        fullTrackQ.setChromosome(new Chromosome(testChromosome.getId()));

        BamQueryOption option = new BamQueryOption();
        option.setTrackDirection(TrackDirectionType.MIDDLE);
        option.setShowSpliceJunction(true);
        option.setShowClipping(true);
        option.setFrame(TEST_FRAME_SIZE);
        option.setCount(TEST_COUNT);

        ResponseEmitterMock emitterMock = new ResponseEmitterMock();
        bamManager.sendBamTrackToEmitterFromUrl(fullTrackQ, option, bamPath, indexPath, emitterMock);
        BamTrack<Read> fullTrack = emitterMock.getBamTrack();
        testBamTrack(fullTrack);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadUrl() throws Exception {
        final String path = "/agnX1.09-28.trim.dm606.realign.bam";
        String bamUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + path;
        String indexUrl = bamUrl + BAI_EXTENSION;

        Server server = new Server(UrlTestingUtils.TEST_FILE_SERVER_PORT);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request,
                                   HttpServletResponse response) throws IOException, ServletException {
                String uri = baseRequest.getRequestURI();
                logger.info(uri);
                File file = new File(resource.getFile().getAbsolutePath() + uri);
                MultipartFileSender fileSender = MultipartFileSender.fromFile(file);
                try {
                    fileSender.with(request).with(response).serveResource();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        try {
            server.start();

            Track<Read> fullTrackQ = new Track<>();
            fullTrackQ.setStartIndex(TEST_START_INDEX_SMALL_RANGE);
            fullTrackQ.setEndIndex(TEST_END_INDEX_SMALL_RANGE);
            fullTrackQ.setScaleFactor(SCALE_FACTOR_SMALL);
            fullTrackQ.setChromosome(new Chromosome(testChromosome.getId()));

            BamQueryOption option = new BamQueryOption();
            option.setTrackDirection(TrackDirectionType.MIDDLE);
            option.setShowSpliceJunction(true);
            option.setShowClipping(true);
            option.setFrame(TEST_FRAME_SIZE);
            option.setCount(TEST_COUNT);

            ResponseEmitterMock emitterMock = new ResponseEmitterMock();
            bamManager.sendBamTrackToEmitterFromUrl(fullTrackQ, option, bamUrl, indexUrl, emitterMock);
            BamTrack<Read> fullTrack = emitterMock.getBamTrack();

            testBamTrack(fullTrack);
            Read testRead = fullTrack.getBlocks().get(0);
            testRead(testRead);

            // Test loading read by url
            ReadQuery query = new ReadQuery();
            query.setName(testRead.getName());
            query.setChromosomeId(testChromosome.getId());
            query.setStartIndex(testRead.getStartIndex());
            query.setEndIndex(testRead.getEndIndex());

            Read loadedRead = bamManager.loadRead(query, bamUrl, indexUrl);
            assertNotNull(loadedRead);
            assertTrue(StringUtils.isNotBlank(loadedRead.getSequence()));
            assertFalse(loadedRead.getTags().isEmpty());
            assertTrue(StringUtils.isNotBlank(loadedRead.getQualities()));
            assertEquals(testRead.getName(), loadedRead.getName());
        } finally {
            server.stop();
        }
    }

    @Test
    @Ignore
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void s3Test() throws IOException {
        Bucket bucket = new Bucket();

        bucket.setBucketName(s3BucketName);
        bucket.setAccessKeyId(s3AccessKey);
        bucket.setSecretAccessKey(s3SecretKey);
        bucketManager.create(bucket);

        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(s3FilePath);
        request.setIndexPath(s3IndexPath);
        request.setName(TEST_NSAME);
        request.setReferenceId(testReference.getId());
        request.setType(BiologicalDataItemResourceType.S3);
        request.setS3BucketId(bucket.getId());
        request.setIndexS3BucketId(bucket.getId());
        request.setIndexType(BiologicalDataItemResourceType.S3);
        BamFile bamFile = bamManager.registerBam(request);
        assertNotNull(bamFile);
        BamFile loadBamFile = bamFileManager.load(bamFile.getId());
        assertNotNull(loadBamFile);

        bamManager.unregisterBamFile(loadBamFile.getId());
        loadBamFile = bamFileManager.load(bamFile.getId());
        assertNull(loadBamFile);

        List<BiologicalDataItem> items = biologicalDataItemDao.loadBiologicalDataItemsByIds(Arrays.asList(
                bamFile.getBioDataItemId(), bamFile.getIndex().getId()));
        assertTrue(items.isEmpty());
    }

    @Ignore
    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void hdfsTest() throws IOException {

        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(hdfsFilePath);
        request.setIndexPath(hdfsIndexPath);
        request.setName(TEST_NSAME);
        request.setReferenceId(testReference.getId());
        request.setType(BiologicalDataItemResourceType.HDFS);

        BamFile bamFile = bamManager.registerBam(request);
        assertNotNull(bamFile);
        BamFile loadBamFile = bamFileManager.load(bamFile.getId());
        assertNotNull(loadBamFile);
        bamManager.unregisterBamFile(loadBamFile.getId());
        loadBamFile = bamFileManager.load(bamFile.getId());
        assertNotNull(loadBamFile);

        List<BiologicalDataItem> items = biologicalDataItemDao.loadBiologicalDataItemsByIds(Arrays.asList(
                bamFile.getBioDataItemId(), bamFile.getIndex().getId()));
        assertTrue(items.isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testCalculateConsensusSequencePerformance() throws IOException {
        // Set isPerfomaceTest to true if you want to measure time
        boolean isPerformanceTest = true;

        BamFile bamFile = setUpTestFile();

        int i = 0;
        double avTime1 = 0.0;
        double avTime2 = 0.0;
        double avTime3 = 0.0;
        do {
            // Small range.
            Track<Sequence> track = new Track<>();
            track.setStartIndex(TEST_START_INDEX_SMALL_RANGE);
            track.setEndIndex(TEST_END_INDEX_SMALL_RANGE);
            track.setScaleFactor(SCALE_FACTOR_SMALL);
            track.setChromosome(testChromosome);
            track.setId(bamFile.getId());

            long start = System.currentTimeMillis();
            Track<Sequence> seq = bamManager.calculateConsensusSequence(track);
            long end = System.currentTimeMillis();
            assertNotNull(seq);
            avTime1 += (end - start);

            // Large range.
            track.setStartIndex(TEST_START_INDEX_LARGE_RANGE);
            track.setEndIndex(TEST_END_INDEX_LARGE_RANGE);
            track.setScaleFactor(SCALE_FACTOR_LARGE);

            start = System.currentTimeMillis();
            seq = bamManager.calculateConsensusSequence(track);
            end = System.currentTimeMillis();
            assertNotNull(seq);
            avTime2 += (end - start);

            // Medium range.
            track.setStartIndex(TEST_START_INDEX_MEDIUM_RANGE);
            track.setEndIndex(TEST_END_INDEX_MEDIUM_RANGE);
            track.setScaleFactor(SCALE_FACTOR_MEDIUM);

            start = System.currentTimeMillis();
            seq = bamManager.calculateConsensusSequence(track);
            end = System.currentTimeMillis();
            assertNotNull(seq);
            avTime3 += (end - start);

            i++;
            if (i == 10) {
                avTime1 = avTime1 / 10;
                System.out.println("BAM seq (small scale) tooks " + avTime1 + "ms");
                avTime2 = avTime2 / 10;
                System.out.println("BAM seq (large scale) tooks " + avTime2 + "ms");
                avTime3 = avTime3 / 10;
                System.out.println("BAM seq (medium scale) tooks " + avTime3 + "ms");
                isPerformanceTest = false;
            }
        } while (isPerformanceTest);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testGetRegions() throws IOException {
        final String path = resource.getFile().getAbsolutePath() + TEST_BAM_NAME;
        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(path);
        request.setIndexPath(path + BAI_EXTENSION);
        request.setName(TEST_NSAME);
        request.setReferenceId(testReference.getId());
        request.setType(BiologicalDataItemResourceType.FILE);

        BamFile bamFile = bamManager.registerBam(request);

        Track<Read> fullTrackQ = new Track<>();
        fullTrackQ.setStartIndex(1);
        fullTrackQ.setEndIndex(testChromosome.getSize());
        fullTrackQ.setScaleFactor(SCALE_FACTOR_SMALL);
        fullTrackQ.setChromosome(new Chromosome(testChromosome.getId()));
        fullTrackQ.setId(bamFile.getId());

        BamQueryOption option = getBaseBamQueryOption();
        option.setMode(BamTrackMode.REGIONS);

        ResponseEmitterMock emitterMock = new ResponseEmitterMock();
        bamManager.sendBamTrackToEmitter(fullTrackQ, option, emitterMock);
        BamTrack<Read> fullTrack = emitterMock.getBamTrack();

        assertFalse(fullTrack.getRegions().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testGetCoverage() throws IOException {
        final String path = resource.getFile().getAbsolutePath() + TEST_BAM_NAME;
        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(path);
        request.setIndexPath(path + BAI_EXTENSION);
        request.setName(TEST_NSAME);
        request.setReferenceId(testReference.getId());
        request.setType(BiologicalDataItemResourceType.FILE);

        BamFile bamFile = bamManager.registerBam(request);

        Track<Read> fullTrackQ = new Track<>();
        fullTrackQ.setStartIndex(TEST_START_INDEX_LARGE_RANGE);
        fullTrackQ.setEndIndex(TEST_END_INDEX_LARGE_RANGE);
        fullTrackQ.setScaleFactor(SCALE_FACTOR_SMALL);
        fullTrackQ.setChromosome(new Chromosome(testChromosome.getId()));
        fullTrackQ.setId(bamFile.getId());

        BamQueryOption option = new BamQueryOption();
        option.setTrackDirection(TrackDirectionType.MIDDLE);
        option.setShowSpliceJunction(true);
        option.setShowClipping(true);
        option.setFrame(TEST_FRAME_SIZE);
        option.setCount(TEST_COUNT);
        option.setMode(BamTrackMode.COVERAGE);

        ResponseEmitterMock emitterMock = new ResponseEmitterMock();
        bamManager.sendBamTrackToEmitter(fullTrackQ, option, emitterMock);
        BamTrack<Read> fullTrack = emitterMock.getBamTrack();

        assertFalse(fullTrack.getBaseCoverage().isEmpty());
        assertTrue(StringUtils.isBlank(fullTrack.getReferenceBuffer()));
        assertTrue(fullTrack.getBaseCoverage().stream().allMatch(c -> c.getValue() != 0));
        assertTrue(fullTrack.getBaseCoverage().stream().allMatch(c -> c.getaCov() == null
                && c.getaCov() == null && c.gettCov() == null && c.getgCov() == null && c.getnCov() == null
                && c.getDelCov() == null && c.getInsCov() == null));
        assertTrue(fullTrack.getBlocks().isEmpty());
        assertNull(fullTrack.getDownsampleCoverage());
    }

    private BamFile setUpTestFile() throws IOException {
        String path = resource.getFile().getAbsolutePath() + TEST_BAM_NAME;
        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(path);
        request.setIndexPath(path + BAI_EXTENSION);
        request.setName(TEST_BAM_NAME + biologicalDataItemDao.createBioItemId());
        request.setReferenceId(testReference.getId());
        request.setType(BiologicalDataItemResourceType.FILE);

        BamFile bamFile = bamManager.registerBam(request);
        assertNotNull(bamFile);
        return bamFile;
    }

    private void testRead(Read read) {
        assertNotNull(read);
        assertNotNull(read.getName());
        assertNotNull(read.getEndIndex());
        assertNotNull(read.getStartIndex());
        assertNotNull(read.getStand());
        assertNotNull(read.getCigarString());
        assertFalse(read.getCigarString().isEmpty());
        assertNotNull(read.getFlagMask());
        assertNotNull(read.getMappingQuality());
        assertNotNull(read.getReadGroup());
        assertNotNull(read.getTLen());
        assertNotNull(read.getPNext());
        assertNotNull(read.getRNext());
        assertFalse(read.getRNext().isEmpty());
    }

    private void testBamTrack(BamTrack<Read> fullTrack) {
        assertNotNull(fullTrack.getBlocks());
        assertFalse(fullTrack.getBlocks().isEmpty());
        assertNotNull(fullTrack.getBaseCoverage());
        assertFalse(fullTrack.getBaseCoverage().isEmpty());
        assertNotNull(fullTrack.getDownsampleCoverage());
        assertFalse(fullTrack.getDownsampleCoverage().isEmpty());
        assertNull(fullTrack.getSpliceJunctions());

    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRegisterBamWithHeaderWithoutSOTag() throws IOException {
        String bamWithHeaderWithoutSO = "header_without_SO.bam";
        registerFileWithoutSOTag("classpath:templates/" + bamWithHeaderWithoutSO);
        List<BiologicalDataItem> biologicalDataItems = biologicalDataItemDao.loadFilesByNameStrict(TEST_NSAME);
        assertFalse(biologicalDataItems.isEmpty());
    }

    private void registerFileWithoutSOTag(String path) throws IOException {
        Resource resource = context.getResource(path);
        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(resource.getFile().getAbsolutePath());
        request.setIndexPath(resource.getFile().getAbsolutePath() + BAI_EXTENSION);
        request.setName(TEST_NSAME);
        request.setReferenceId(testReference.getId());
        request.setType(BiologicalDataItemResourceType.FILE);
        bamManager.registerBam(request);
    }
}
