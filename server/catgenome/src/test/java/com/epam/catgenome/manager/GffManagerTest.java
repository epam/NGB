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

package com.epam.catgenome.manager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import com.epam.catgenome.controller.util.UrlTestingUtils;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.server.Server;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
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

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.externaldb.DimEntity;
import com.epam.catgenome.entity.externaldb.DimStructure;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneTranscript;
import com.epam.catgenome.entity.gene.Transcript;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.GeneReadingException;
import com.epam.catgenome.exception.HistogramReadingException;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.externaldb.EnsemblDataManager;
import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.epam.catgenome.manager.externaldb.ParameterNameValue;
import com.epam.catgenome.manager.externaldb.PdbDataManager;
import com.epam.catgenome.manager.externaldb.UniprotDataManager;
import com.epam.catgenome.manager.gene.GeneFileManager;
import com.epam.catgenome.manager.gene.GeneUtils;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.util.Utils;
import htsjdk.samtools.util.IntervalTree;
import htsjdk.tribble.TribbleException;

/**
 * Source:      GffManagerTest
 * Created:     02.12.15, 16:15
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class GffManagerTest extends AbstractManagerTest {
    private static final int START_INDEX_ASTRA = 17084954;
    private static final int END_INDEX_ASTRA = 17084954;
    private static final String PBD_TEST_ID = "2K8F";
    private static final String PROTEIN_CODING = "protein_coding";
    private static final int TEST_UNMAPPED_END_INDEX = 14405;
    private static final int TEST_FEATURE_COUNT = 58;

    private static final String GENES_SORTED_GTF_PATH = "classpath:templates/genes_sorted.gtf";
    private static final int TEST_END_INDEX = 239107476;
    private static final Double FULL_QUERY_SCALE_FACTOR = 1D;
    private static final Double SMALL_SCALE_FACTOR = 0.0009;
    private static final int TEST_CHROMOSOME_SIZE = 239107476;
    private static final int TEST_CENTER_POSITION = 109836;
    private static final int TEST_VIEW_PORT_SIZE = 30000;
    private static final int TEST_INTRON_LENGTH = 100;

    @Autowired
    ApplicationContext context;

    @Mock
    private HttpDataManager httpDataManager;

    @Spy
    @InjectMocks
    private PdbDataManager pBDataManager;

    @Spy
    @InjectMocks
    private EnsemblDataManager ensemblDataManager;

    @Spy
    @InjectMocks
    private UniprotDataManager uniprotDataManager;

    @InjectMocks
    @Autowired
    @Spy
    private GffManager gffManager;

    @Autowired
    private GeneFileManager geneFileManager;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private FileManager fileManager;

    @Value("#{catgenome['files.base.directory.path']}")
    private String baseDirPath;

    private long referenceId;
    private Reference testReference;
    private Chromosome testChromosome;

    private Logger logger = LoggerFactory.getLogger(GffManagerTest.class);

    @Before
    public void setup() throws Exception {

        MockitoAnnotations.initMocks(this);
        Assert.assertNotNull(pBDataManager);
        Assert.assertNotNull(uniprotDataManager);
        Assert.assertNotNull(ensemblDataManager);

        testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        testReference = EntityHelper.createNewReference(testChromosome, referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(testReference);
        referenceId = testReference.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRegisterGtf() throws Exception {
        Assert.assertTrue(testRegister(GENES_SORTED_GTF_PATH));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterGff() throws InterruptedException, FeatureIndexException, IOException,
                                         NoSuchAlgorithmException, HistogramReadingException, GeneReadingException {
        Assert.assertTrue(testRegister("classpath:templates/genes_sorted.gff3"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterZippedGtf() throws InterruptedException, FeatureIndexException, IOException,
                                         NoSuchAlgorithmException, HistogramReadingException, GeneReadingException {
        Assert.assertTrue(testRegister("classpath:templates/genes_sorted.gtf.gz"));
    }

    @Test
    @Ignore
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterUrl() throws Exception {
        final String path = "/genes_sorted.gtf";
        String fileUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + path;
        String indexUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + "/genes_sorted.gtf.tbi";

        Server server = UrlTestingUtils.getFileServer(context);
        try {
            server.start();

            FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
            request.setReferenceId(referenceId);
            request.setPath(fileUrl);
            request.setIndexPath(indexUrl);
            request.setType(BiologicalDataItemResourceType.URL);
            request.setIndexType(BiologicalDataItemResourceType.URL);

            GeneFile geneFile = gffManager.registerGeneFile(request);
            Assert.assertNotNull(geneFile);
            Assert.assertNotNull(geneFile.getId());

            Track<Wig> histogram = new Track<>();
            histogram.setId(geneFile.getId());
            histogram.setChromosome(testChromosome);
            histogram.setScaleFactor(1.0);

            gffManager.loadHistogram(histogram);
            Assert.assertTrue(histogram.getBlocks().isEmpty());
        } finally {
            server.stop();
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testGetNextFeature() throws IOException, InterruptedException, FeatureIndexException,
                                            NoSuchAlgorithmException, GeneReadingException {
        Resource resource = context.getResource(GENES_SORTED_GTF_PATH);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        Track<Gene> track = new Track<>();
        track.setId(geneFile.getId());
        track.setStartIndex(1);
        track.setEndIndex(TEST_END_INDEX);
        track.setChromosome(testChromosome);
        track.setScaleFactor(FULL_QUERY_SCALE_FACTOR);

        Track<Gene> featureList = gffManager.loadGenes(track, false);
        Assert.assertNotNull(featureList);
        Assert.assertFalse(featureList.getBlocks().isEmpty());

        List<Gene> exons = new ArrayList<>();
        for (Gene gene : featureList.getBlocks()) {
            if (gene.getItems() != null) {
                for (Gene mRna : gene.getItems()) {
                    if (mRna.getItems() != null) {
                        exons.addAll(mRna.getItems().stream().filter(s -> "exon".equals(s.getFeature()))
                                .collect(Collectors.toList()));
                    }
                }
            }
        }

        Assert.assertFalse(exons.isEmpty());

        Collections.sort(exons, (o1, o2) -> o1.getStartIndex().compareTo(o2.getStartIndex()));
        int middle = exons.size() / 2;
        Gene firstExon = exons.get(middle);
        Gene secondExon = exons.get(middle + 1);

        double time1 = Utils.getSystemTimeMilliseconds();
        Gene loadedNextExon = gffManager.getNextOrPreviousFeature(firstExon.getEndIndex(), geneFile.getId(),
                testChromosome.getId(), true);
        double time2 = Utils.getSystemTimeMilliseconds();
        logger.info("next feature took {} ms", time2 - time1);
        Assert.assertNotNull(loadedNextExon);
        Assert.assertEquals(secondExon.getStartIndex(), loadedNextExon.getStartIndex());
        Assert.assertEquals(secondExon.getEndIndex(), loadedNextExon.getEndIndex());

        time1 = Utils.getSystemTimeMilliseconds();
        Gene loadPrevExon = gffManager.getNextOrPreviousFeature(secondExon.getStartIndex(), geneFile.getId(),
                testChromosome.getId(), false);
        time2 = Utils.getSystemTimeMilliseconds();
        logger.info("prev feature took {} ms", time2 - time1);

        Assert.assertNotNull(loadPrevExon);
        Assert.assertEquals(firstExon.getStartIndex(), loadPrevExon.getStartIndex());
        Assert.assertEquals(firstExon.getEndIndex(), loadPrevExon.getEndIndex());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadExonsInViewPort() throws InterruptedException, NoSuchAlgorithmException, FeatureIndexException,
            IOException {
        Resource resource = context.getResource(GENES_SORTED_GTF_PATH);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        double time1 = Utils.getSystemTimeMilliseconds();
        List<Block> exons = gffManager.loadExonsInViewPort(geneFile.getId(), testChromosome.getId(),
                TEST_CENTER_POSITION, TEST_VIEW_PORT_SIZE, TEST_INTRON_LENGTH);
        double time2 = Utils.getSystemTimeMilliseconds();
        Assert.assertFalse(exons.isEmpty());
        logger.info("Loading exons took {} ms", time2 - time1);
        List<Block> exonsBelow = exons.stream()
                .filter(e -> e.getEndIndex() < TEST_CENTER_POSITION)
                .collect(Collectors.toList());
        List<Block> exonsAbove = exons.stream()
                .filter(e -> e.getStartIndex() > TEST_CENTER_POSITION)
                .collect(Collectors.toList());
        Assert.assertFalse(exonsBelow.isEmpty());
        Assert.assertFalse(exonsAbove.isEmpty());

        testOverlapping(exons);

        final Map<String, Pair<Integer, Integer>> metaMap = fileManager.loadIndexMetadata(geneFile);
        Pair<Integer, Integer> bounds = metaMap.get(testChromosome.getName());
        if (bounds == null) {
            bounds = metaMap.get(Utils.changeChromosomeName(testChromosome.getName()));
        }

        testFillRange(exonsBelow, TEST_VIEW_PORT_SIZE / 2, TEST_CENTER_POSITION, false, bounds.getLeft());
        testFillRange(exonsAbove, TEST_VIEW_PORT_SIZE / 2, TEST_CENTER_POSITION, true, bounds.getRight());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadExonsInTrack() throws IOException, FeatureIndexException, InterruptedException,
            NoSuchAlgorithmException {
        Resource resource = context.getResource(GENES_SORTED_GTF_PATH);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        List<Block> exons = gffManager.loadExonsInViewPort(geneFile.getId(), testChromosome.getId(),
                TEST_CENTER_POSITION, TEST_VIEW_PORT_SIZE, TEST_INTRON_LENGTH);

        List<Block> exons2 = gffManager.loadExonsInTrack(geneFile.getId(), testChromosome.getId(), exons.get(0)
                .getStartIndex(), exons.get(exons.size() - 1).getEndIndex(), TEST_INTRON_LENGTH);

        Assert.assertFalse(exons2.isEmpty());
        Assert.assertEquals(exons.size(), exons2.size());
        for (int i = 0; i < exons2.size(); i++) {
            Assert.assertEquals(exons.get(i).getStartIndex(), exons2.get(i).getStartIndex());
            Assert.assertEquals(exons.get(i).getEndIndex(), exons2.get(i).getEndIndex());
        }

        testOverlapping(exons2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRegisterGffFail() throws IOException, FeatureIndexException, InterruptedException,
            NoSuchAlgorithmException {
        Resource resource = context.getResource("classpath:templates/Felis_catus.Felis_catus_6.2.81.gtf");

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        boolean failed = true;
        try {
            gffManager.registerGeneFile(request);
        } catch (TribbleException.MalformedFeatureFile e) {
            failed = false;
        }

        Assert.assertFalse("Not failed on unsorted file", failed);

        /*Resource fakeIndex = context.getResource("classpath:templates/fake_gtf_index.tbi");
        request.setIndexPath(fakeIndex.getFile().getAbsolutePath());

        failed = true;
        try {
            gffManager.registerGeneFile(request);
        } catch (Exception e) {
            failed = false;
        }

        Assert.assertFalse("Not failed on unsorted file", failed);*/
    }


    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadGenesTranscript() throws IOException, InterruptedException, FeatureIndexException,
            NoSuchAlgorithmException, ExternalDbUnavailableException {
        MockitoAnnotations.initMocks(this);
        String fetchRes1 = readFile("ensembl_id_ENSG00000177663.json");
        String fetchRes2 = readFile("uniprot_id_ENST00000319363.xml");
        String fetchRes3 = readFile("uniprot_id_ENST00000319363.xml");
        Mockito.when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(ParameterNameValue[].class)))
                .thenReturn(fetchRes1)
                .thenReturn(fetchRes2)
                .thenReturn(fetchRes3);


        Chromosome otherChromosome = EntityHelper.createNewChromosome("22");
        otherChromosome.setSize(TEST_CHROMOSOME_SIZE);
        Reference otherReference = EntityHelper.createNewReference(otherChromosome,
                referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(otherReference);
        Long otherReferenceId = otherReference.getId();

        Resource resource = context.getResource("classpath:templates/Homo_sapiens.GRCh38.83.sorted.chr21-22.gtf");


        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(otherReferenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        Track<Gene> track = new Track<>();
        track.setId(geneFile.getId());
        track.setStartIndex(START_INDEX_ASTRA);
        track.setEndIndex(END_INDEX_ASTRA);
        track.setChromosome(otherChromosome);
        track.setScaleFactor(FULL_QUERY_SCALE_FACTOR);
        try {
            Track<GeneTranscript> featureList = gffManager.loadGenesTranscript(track, null, null);
            Assert.assertNotNull(featureList);
            Assert.assertFalse(featureList.getBlocks().isEmpty());
            Gene testGene = featureList.getBlocks().get(0);
            Assert.assertNotNull(testGene);
            Assert.assertFalse(testGene.getTranscripts().isEmpty());
            Transcript testTranscript = testGene.getTranscripts().get(1);

            Assert.assertTrue(testTranscript.getBioType().equals(PROTEIN_CODING));

            Assert.assertFalse(testTranscript.getDomain().isEmpty());
            Assert.assertFalse(testTranscript.getExon().isEmpty());
            Assert.assertFalse(testTranscript.getSecondaryStructure().isEmpty());
            Assert.assertFalse(testTranscript.getPdb().isEmpty());
        } catch (GeneReadingException e) {
            logger.info("database unavailable");
        }
    }


    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testPBD() throws IOException, InterruptedException, ExternalDbUnavailableException, JAXBException {

        MockitoAnnotations.initMocks(this);

        String fetchRes1 = readFile("pbd_id_2k8d.xml");
        String fetchRes2 = readFile("pbd_map_id_2k8d.xml");
        Mockito.when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(ParameterNameValue[].class)))
                .thenReturn(fetchRes1)
                .thenReturn(fetchRes2);


        final DimStructure dimStructurelist = gffManager.getPBDItemsFromBD(PBD_TEST_ID);
        Assert.assertNotNull(dimStructurelist);
        Assert.assertNotNull(dimStructurelist.getEntities());
        Assert.assertFalse(dimStructurelist.getEntities().isEmpty());
        Assert.assertNotNull(dimStructurelist.getStructureTitle());
        Assert.assertNotNull(dimStructurelist.getClassification());
        Assert.assertNotNull(dimStructurelist.getStructureId());
        final DimEntity entity = dimStructurelist.getEntities().get(0);
        Assert.assertNotNull(entity.getCompound());
        Assert.assertNotNull(entity.getChainId());
        Assert.assertNotNull(entity.getPdbEnd());
        Assert.assertNotNull(entity.getPdbStart());
        Assert.assertNotNull(entity.getUnpEnd());
        Assert.assertNotNull(entity.getUnpStart());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testUnmappedGenes() throws InterruptedException, NoSuchAlgorithmException, FeatureIndexException,
                                           IOException, GeneReadingException, HistogramReadingException {
        Chromosome testChr1 = EntityHelper.createNewChromosome();
        testChr1.setName("chr1");
        testChr1.setSize(TEST_CHROMOSOME_SIZE);
        Reference testRef = EntityHelper.createNewReference(testChr1, referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(testRef);
        Long testRefId = testReference.getId();

        Resource resource = context.getResource("classpath:templates/mrna.sorted.chunk.gtf");

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(testRefId);
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        Track<Gene> track = new Track<>();
        track.setId(geneFile.getId());
        track.setStartIndex(1);
        track.setEndIndex(TEST_UNMAPPED_END_INDEX);
        track.setChromosome(testChr1);
        track.setScaleFactor(FULL_QUERY_SCALE_FACTOR);

        double time1 = Utils.getSystemTimeMilliseconds();
        Track<Gene> featureList = gffManager.loadGenes(track, false);
        double time2 = Utils.getSystemTimeMilliseconds();
        logger.info("genes loading : {} ms", time2 - time1);
        Assert.assertNotNull(featureList);
        Assert.assertFalse(featureList.getBlocks().isEmpty());
        Assert.assertTrue(featureList.getBlocks().stream().allMatch(g -> !g.isMapped()));

        Track<Gene> smallScaleFactorTrack = new Track<>();
        smallScaleFactorTrack.setId(geneFile.getId());
        smallScaleFactorTrack.setStartIndex(1);
        smallScaleFactorTrack.setEndIndex(TEST_UNMAPPED_END_INDEX);
        smallScaleFactorTrack.setChromosome(testChr1);
        smallScaleFactorTrack.setScaleFactor(SMALL_SCALE_FACTOR);

        time1 = Utils.getSystemTimeMilliseconds();
        featureList = gffManager.loadGenes(smallScaleFactorTrack, false);
        time2 = Utils.getSystemTimeMilliseconds();
        logger.info("genes large scale loading : {} ms", time2 - time1);
        Assert.assertNotNull(featureList);
        Assert.assertFalse(featureList.getBlocks().isEmpty());
        Assert.assertTrue(featureList.getBlocks().stream().allMatch(g -> !g.isMapped()));
        int groupedGenesCount = featureList.getBlocks().stream().collect(Collectors.summingInt(Gene::getFeatureCount));
        logger.debug("{} features total", groupedGenesCount);
        Assert.assertEquals(TEST_FEATURE_COUNT, groupedGenesCount);

        Track<Wig> histogram = new Track<>();
        histogram.setId(geneFile.getId());
        histogram.setChromosome(testChr1);
        histogram.setScaleFactor(1.0);

        gffManager.loadHistogram(histogram);
        Assert.assertFalse(histogram.getBlocks().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testCollapsedGtf() throws InterruptedException, NoSuchAlgorithmException, FeatureIndexException,
                                          IOException, GeneReadingException {
        Assert.assertTrue(testCollapsed(GENES_SORTED_GTF_PATH));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testCollapsedGff() throws InterruptedException, NoSuchAlgorithmException, FeatureIndexException,
                                          IOException, GeneReadingException {
        Assert.assertTrue(testCollapsed("classpath:templates/genes_sorted.gff3"));
    }

    private boolean testCollapsed(String path) throws IOException, FeatureIndexException, InterruptedException,
                                                   NoSuchAlgorithmException, GeneReadingException {
        Resource resource = context.getResource(path);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        Track<Gene> track = new Track<>();
        track.setId(geneFile.getId());
        track.setStartIndex(1);
        track.setEndIndex(TEST_END_INDEX);
        track.setChromosome(testChromosome);
        track.setScaleFactor(FULL_QUERY_SCALE_FACTOR);

        Track<Gene> featureList = gffManager.loadGenes(track, false);
        Assert.assertTrue(featureList.getBlocks().stream().anyMatch(g -> g.getItems().size() > 1));

        track = new Track<>();
        track.setId(geneFile.getId());
        track.setStartIndex(1);
        track.setEndIndex(TEST_END_INDEX);
        track.setChromosome(testChromosome);
        track.setScaleFactor(FULL_QUERY_SCALE_FACTOR);

        double time1 = Utils.getSystemTimeMilliseconds();
        Track<Gene> featureListCollapsed = gffManager.loadGenes(track, true);
        double time2 = Utils.getSystemTimeMilliseconds();
        logger.info("genes loading : {} ms", time2 - time1);
        Assert.assertNotNull(featureListCollapsed);
        Assert.assertFalse(featureListCollapsed.getBlocks().isEmpty());
        Assert.assertTrue(featureListCollapsed.getBlocks().stream().filter(GeneUtils::isGene)
                .allMatch(g -> g.getItems().size() == 1));
        Assert.assertFalse(featureListCollapsed.getBlocks().stream().filter(GeneUtils::isGene)
                .allMatch(g -> g.getItems().get(0).getItems().isEmpty()));

        return true;
    }

    private void testOverlapping(List<Block> exons) {
        IntervalTree<Block> intervalTree = new IntervalTree<>();
        for (Block exon : exons) {
            Iterator<IntervalTree.Node<Block>> nodeIterator = intervalTree.overlappers(exon.getStartIndex(),
                                                                                       exon.getEndIndex());
            Assert.assertFalse("Should be no overlapping exons", nodeIterator.hasNext());
            intervalTree.put(exon.getStartIndex(), exon.getEndIndex(), exon);
        }
    }

    private void testFillRange(List<Block> exons, int viewPortSize, int centerPosition, boolean forward, int bound) {
        int totalLength = exons.stream()
            .collect(Collectors.summingInt(e -> gffManager.calculateExonLength(e, centerPosition, forward)));

        boolean isBounded;
        if (forward) {
            isBounded = exons.get(exons.size() - 1).getEndIndex() >= bound;
        } else {
            isBounded = exons.get(0).getStartIndex() <= bound;
        }

        Assert.assertTrue(totalLength >= viewPortSize || isBounded);
    }

    private boolean testRegister(String path) throws IOException, InterruptedException, FeatureIndexException,
                                                  NoSuchAlgorithmException, HistogramReadingException,
                                                  GeneReadingException {
        Resource resource = context.getResource(path);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        Track<Wig> histogram = new Track<>();
        histogram.setId(geneFile.getId());
        histogram.setChromosome(testChromosome);
        histogram.setScaleFactor(1.0);

        gffManager.loadHistogram(histogram);
        Assert.assertFalse(histogram.getBlocks().isEmpty());

        Track<Gene> track = new Track<>();
        track.setId(geneFile.getId());
        track.setStartIndex(1);
        track.setEndIndex(TEST_END_INDEX);
        track.setChromosome(testChromosome);
        track.setScaleFactor(FULL_QUERY_SCALE_FACTOR);

        double time1 = Utils.getSystemTimeMilliseconds();
        Track<Gene> featureList = gffManager.loadGenes(track, false);
        double time2 = Utils.getSystemTimeMilliseconds();
        logger.info("genes loading : {} ms", time2 - time1);
        Assert.assertNotNull(featureList);
        Assert.assertFalse(featureList.getBlocks().isEmpty());
        logger.info("{} genes", featureList.getBlocks().size());

        featureList.getBlocks().stream().filter(g -> g.getItems() != null)
                .forEach(g -> Assert.assertTrue(g.getItems().stream()
                        .filter(GeneUtils::isTranscript)
                        .allMatch(i -> i.getExonsCount() != null && i.getAminoacidLength() != null
                                && i.getExonsCount() == i.getItems().stream()
                                .filter(ii -> "exon".equalsIgnoreCase(ii.getFeature())).count())));

        Track<Gene> track2 = new Track<>();
        track2.setId(geneFile.getId());
        track2.setStartIndex(1);
        track2.setEndIndex(TEST_END_INDEX);
        track2.setChromosome(testChromosome);
        track2.setScaleFactor(SMALL_SCALE_FACTOR);

        time1 = Utils.getSystemTimeMilliseconds();
        Track<Gene> featureListLargeScale = gffManager.loadGenes(track2, false);
        time2 = Utils.getSystemTimeMilliseconds();
        logger.info("genes loading large scale: {} ms", time2 - time1);

        Assert.assertEquals(featureListLargeScale.getBlocks().size(), featureList.getBlocks().stream()
                .filter(Gene::isMapped).count());
        Assert.assertFalse(featureListLargeScale.getBlocks().isEmpty());
        Assert.assertTrue(featureListLargeScale.getBlocks().stream().allMatch(g -> g.getItems() == null));

        // unregister:
        gffManager.unregisterGeneFile(geneFile.getId());

        boolean failed = false;
        try {
            geneFileManager.loadGeneFile(geneFile.getId());
        } catch (IllegalArgumentException e) {
            failed = true;
        }
        Assert.assertTrue(failed);
        List<BiologicalDataItem> dataItems = biologicalDataItemDao.loadBiologicalDataItemsByIds(
                Arrays.asList(geneFile.getBioDataItemId(), geneFile.getIndex().getId()));
        Assert.assertTrue(dataItems.isEmpty());

        File dir = new File(baseDirPath + "/42/genes/" + geneFile.getId());
        Assert.assertFalse(dir.exists());

        return true;
    }

    public void checkGenesCorrectness(List<Gene> genes) {
        Assert.assertTrue(genes.stream().allMatch(g -> MapUtils.isNotEmpty(g.getAttributes())));
        Assert.assertTrue(genes.stream().allMatch(g -> !GeneUtils.isGene(g) || CollectionUtils.isNotEmpty(
            g.getItems())));
        Assert.assertTrue(genes.stream().allMatch(g -> !GeneUtils.isGene(g) || g.getItems().stream().allMatch(
            i -> MapUtils.isNotEmpty(i.getAttributes()))));
        Assert.assertTrue(genes.stream().allMatch(g -> !GeneUtils.isGene(g) || g.getItems().stream().allMatch(
            i -> CollectionUtils.isNotEmpty(i.getItems()))));
    }


    private String readFile(String filename) throws IOException {
        Resource resource = context.getResource("classpath:externaldb//data//" + filename);
        String pathStr = resource.getFile().getPath();
        return new String(Files.readAllBytes(Paths.get(pathStr)), Charset.defaultCharset());
    }


}
