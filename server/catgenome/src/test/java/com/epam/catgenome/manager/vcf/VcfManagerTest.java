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

package com.epam.catgenome.manager.vcf;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.manager.gene.GeneTrackManager;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import htsjdk.tribble.TribbleException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.codehaus.jettison.json.JSONObject;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.util.UrlTestingUtils;
import com.epam.catgenome.controller.vo.Query2TrackConverter;
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.controller.vo.ga4gh.VariantGA4GH;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.track.TrackType;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationQuery;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.entity.vcf.VcfSample;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.exception.FeatureFileReadingException;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.Ga4ghResourceUnavailableException;
import com.epam.catgenome.exception.VcfReadingException;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.manager.DownloadFileManager;
import com.epam.catgenome.manager.FeatureIndexManager;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.epam.catgenome.manager.externaldb.ParameterNameValue;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.reference.ReferenceManager;
import com.epam.catgenome.manager.vcf.reader.VcfGa4ghReader;
import com.epam.catgenome.util.Utils;

/**
 * Source:      VcfManagerTest.java
 * Created:     22/10/15, 1:46 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * A test for VcfManager class
 * </p>
 *
 * @author Mikhail Miroliubov
 */
@SuppressWarnings("PMD.UnusedPrivateField")
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:test-catgenome.properties")
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class VcfManagerTest extends AbstractManagerTest {

    private static final String CLASSPATH_TEMPLATES_FELIS_CATUS_VCF = "classpath:templates/Felis_catus.vcf";
    private static final String CLASSPATH_TEMPLATES_FELIS_CATUS_VCF_COMPRESSED = "classpath:templates/Felis_catus.vcf" +
            ".gz";
    private static final String CLASSPATH_TEMPLATES_FELIS_CATUS_VCF_GOOGLE = "classpath:templates/1000-genomes.chrMT" +
            ".vcf";
    private static final String HTTP_VCF = "http://localhost/vcf/BK0010_S12.vcf";
    public static final String PRETTY_NAME = "pretty";

    @Mock
    private HttpDataManager httpDataManager;

    @Spy
    @Autowired
    private VcfFileManager vcfFileManager;

    @Spy
    @Autowired
    private TrackHelper trackHelper;

    @Spy
    @Autowired
    private FileManager fileManager;

    @Spy
    @Autowired
    private BiologicalDataItemManager biologicalDataItemManager;

    @Spy
    @Autowired
    private FeatureIndexManager featureIndexManager;

    @Spy
    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Spy
    @Autowired
    private DownloadFileManager downloadFileManager;

    @Spy
    @Autowired
    private GeneTrackManager geneTrackManager;

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private GffManager gffManager;

    @InjectMocks
    private VcfManager vcfManager;

    @Autowired
    private ApplicationContext context;

    @Spy
    @Autowired(required = false)
    private EhCacheBasedIndexCache indexCache;

    private static final int TEST_END_INDEX = 187708306;

    private static final double TEST_SMALL_SCALE_FACTOR = 0.000007682737;

    private static final int TEST_CHROMOSOME_SIZE = 239107476;
    private static final int GENE_POSTION = 35471;
    private static final String SAMPLE_NAME = "HG00702";
    private static final int NUMBER_OF_FILTERS = 2;
    private static final int NUMBER_OF_TRIVIAL_INFO = 18;
    private static final int INDEX_BUFFER_SIZE = 32;
    @Value("${ga4gh.google.variantSetId}")
    private String varSet;
    @Value("${ga4gh.google.startPosition}")
    private Integer start;
    @Value("${ga4gh.google.endPosition}")
    private Integer end;
    @Value("${ga4gh.google.chrGA4GH}")
    private String chrGA4GH;

    @Value("${vcf.extended.info.patterns}")
    private String infoTemplate;

    private long referenceId;
    private long referenceIdGA4GH;
    private Reference testReference;
    private Reference testReferenceGA4GH;
    private Chromosome testChromosome;
    private Chromosome testChrGA4GH;

    private Logger logger = LoggerFactory.getLogger(VcfManagerTest.class);

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        Assert.assertNotNull(featureIndexManager);
        Assert.assertNotNull(downloadFileManager);
        Assert.assertNotNull(biologicalDataItemManager);
        Assert.assertNotNull(fileManager);
        Assert.assertNotNull(trackHelper);
        Assert.assertNotNull(indexCache);

        testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        testReference = EntityHelper.createNewReference(testChromosome, referenceGenomeManager.createReferenceId());

        referenceGenomeManager.create(testReference);
        referenceId = testReference.getId();

        // create new chromosome and reference for ga4gh
        testChrGA4GH = EntityHelper.createNewChromosome(chrGA4GH);
        testChrGA4GH.setSize(TEST_CHROMOSOME_SIZE);
        testReferenceGA4GH = EntityHelper.createNewReference(testChrGA4GH, referenceGenomeManager.createReferenceId());
        testReferenceGA4GH.setType(BiologicalDataItemResourceType.GA4GH);
        referenceGenomeManager.create(testReferenceGA4GH);
        referenceIdGA4GH = testReferenceGA4GH.getId();
        vcfManager.setExtendedInfoTemplates(infoTemplate);
        vcfManager.setIndexBufferSize(INDEX_BUFFER_SIZE);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveLoadVcfFile() throws IOException, InterruptedException, VcfReadingException {
        VcfFile vcfFile = testSave(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        VcfFile file = vcfFileManager.load(vcfFile.getId());
        Assert.assertNotNull(file);
        Assert.assertEquals(PRETTY_NAME, file.getPrettyName());

        testLoad(vcfFile, 1D, true);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveLoadVcfCompressedFile() throws IOException, ClassNotFoundException, InterruptedException,
                                                       ParseException, VcfReadingException {
        VcfFile vcfFile = testSave(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF_COMPRESSED);

        VcfFile file = vcfFileManager.load(vcfFile.getId());
        Assert.assertNotNull(file);

        testLoad(vcfFile, 1D, true);

        /*String featureId = "rs44098047";
        List<FeatureIndexEntry> entries2 = fileManager.searchLuceneIndex(vcfFile, featureId);
        Assert.assertFalse(entries2.isEmpty());
        entries2.forEach(e -> Assert.assertTrue(e.getFeatureId().startsWith(featureId)));*/
    }

    /**
     * Tests vcfFileManager.load() behaviour on small scale factors.
     * Should return a number of variations having type STATISTIC and variationsCount > 1
     */
    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadSmallScaleVcfFile() throws IOException, InterruptedException, VcfReadingException {
        VcfFile vcfFile = testSave(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        VcfFile file = vcfFileManager.load(vcfFile.getId());
        Assert.assertNotNull(file);

        Track<Variation> trackResult = testLoad(vcfFile, TEST_SMALL_SCALE_FACTOR, true);
        List<Variation> ambiguousVariations = trackResult.getBlocks().stream().filter((b) ->
                b.getVariationsCount() != null && b.getVariationsCount() > 1).collect(Collectors.toList());

        Assert.assertFalse(ambiguousVariations.isEmpty());

        /// test not collapsed
        trackResult = testLoad(vcfFile, TEST_SMALL_SCALE_FACTOR, true, false);
        ambiguousVariations = trackResult.getBlocks().stream()
            .filter((b) -> b.getVariationsCount() != null && b.getVariationsCount() > 1)
            .collect(Collectors.toList());

        Assert.assertTrue(ambiguousVariations.isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadSmallScaleVcfFileGa4GH() throws IOException, InterruptedException, NoSuchAlgorithmException,
                                                        ExternalDbUnavailableException, VcfReadingException {

        String fetchRes1 = readFile("GA4GH_id10473.json");
        String fetchRes2 = readFile("GA4GH_id10473_variant.json");
        Mockito.when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(JSONObject.class)))
                .thenReturn(fetchRes1)
                .thenReturn(fetchRes2);

        String fetchRes3 = readFile("GA4GH_id10473_param.json");
        Mockito.when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(ParameterNameValue[].class)))
                .thenReturn(fetchRes3);


        VcfFile vcfFileGA4GH = regesterVcfGA4GH();
        vcfFileGA4GH.setType(BiologicalDataItemResourceType.GA4GH);
        List<VcfSample> vcfSamples = vcfFileGA4GH.getSamples();
        Track<Variation> trackResult;
        Long sampleId = 0L;
        for (VcfSample sample : vcfSamples) {
            if (sample.getName().equals(SAMPLE_NAME)) {
                sampleId = sample.getId();
            }
        }
        trackResult = testLoadGA4GH(vcfFileGA4GH, TEST_SMALL_SCALE_FACTOR, true, sampleId);
        List<Variation> ambiguousVariations = trackResult.getBlocks().stream().filter((b) ->
                b.getVariationsCount() != null && b.getVariationsCount() > 1).collect(Collectors.toList());

        Assert.assertFalse(ambiguousVariations.isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadExtendedSummary()
            throws IOException, InterruptedException, FeatureIndexException,
                   NoSuchAlgorithmException, FeatureFileReadingException {
        VcfFile vcfFile = testSave("classpath:templates/samples.vcf");

        VcfFile file = vcfFileManager.load(vcfFile.getId());
        Assert.assertNotNull(file);

        Track<Variation> trackResult = testLoad(file, 1D, true);

        final VariationQuery query = new VariationQuery();
        query.setId(vcfFile.getId());
        query.setChromosomeId(testChromosome.getId());
        query.setPosition(trackResult.getBlocks().get(0).getStartIndex());
        Variation variation = vcfManager.loadVariation(query);

        Assert.assertFalse(variation.getInfo().isEmpty());
        Assert.assertFalse(variation.getGenotypeData().getInfo().isEmpty());

        VcfFilterInfo filterInfo = vcfManager.getFiltersInfo(Collections.singleton(vcfFile.getId()));
        Assert.assertFalse(filterInfo.getInfoItems().isEmpty());
        Assert.assertFalse(filterInfo.getAvailableFilters().isEmpty());

        // now add a project and try to fetch genes affected
        vcfFile = testSave(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        Resource resource = context.getResource("classpath:templates/genes_sorted.gtf");

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), geneFile.getId());

        query.setId(vcfFile.getId());
        query.setPosition(GENE_POSTION);
        variation = vcfManager.loadVariation(query);
        Assert.assertFalse(variation.getInfo().isEmpty());
        Assert.assertNotNull(variation.getGeneNames());
        Assert.assertFalse(variation.getGeneNames().isEmpty());
    }

    public VcfFile regesterVcfGA4GH()
        throws IOException, InterruptedException, NoSuchAlgorithmException, VcfReadingException {
        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceIdGA4GH);
        request.setType(BiologicalDataItemResourceType.GA4GH);
        request.setPath(varSet);
        VcfFile vcfFileGA4GH = vcfManager.registerVcfFile(request);
        vcfFileGA4GH.setType(BiologicalDataItemResourceType.GA4GH);
        return vcfFileGA4GH;
    }

    @Ignore
    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testAllTrackGa4GH()
        throws IOException, InterruptedException, NoSuchAlgorithmException, VcfReadingException {
        VcfFile vcfFileGA4GH = regesterVcfGA4GH();
        vcfFileGA4GH.setType(BiologicalDataItemResourceType.GA4GH);
        List<VcfSample> vcfSamples = vcfFileGA4GH.getSamples();
        for (VcfSample sample : vcfSamples) {
            Track<Variation> trackResultGA4GH = testLoadGA4GH(vcfFileGA4GH, 1D, true, sample.getId());
            Assert.assertNotNull(trackResultGA4GH);
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testGetVariantsGA4GH() throws IOException, InterruptedException,
                                              ExternalDbUnavailableException,
                                              Ga4ghResourceUnavailableException {

        String fetchRes1 = readFile("GA4GH_id10473_variant_2.json");

        Mockito.when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(JSONObject.class)))
                .thenReturn(fetchRes1);


        VcfGa4ghReader reader = new VcfGa4ghReader(httpDataManager, referenceGenomeManager);
        List<VariantGA4GH> ghList = reader.getVariantsGA4GH(varSet, start.toString(), end.toString(),
                testChrGA4GH.getName());
        Assert.assertFalse(ghList.isEmpty());
        Assert.assertNotNull(ghList.get(1).getNames());
        Assert.assertFalse(ghList.get(1).getCalls().isEmpty());
    }

    @Ignore
    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRegisterDownloadFile() throws IOException, ClassNotFoundException, InterruptedException,
                                                 ParseException, NoSuchAlgorithmException, VcfReadingException {
        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setType(BiologicalDataItemResourceType.DOWNLOAD);
        request.setPath(HTTP_VCF);

        VcfFile vcfFile = vcfManager.registerVcfFile(request);
        Assert.assertNotNull(vcfFile);
        Assert.assertNotNull(vcfFile.getId());

    }


    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRegisterFile() throws IOException{
        Resource resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        VcfFile vcfFile = vcfManager.registerVcfFile(request);
        Assert.assertNotNull(vcfFile);
        Assert.assertNotNull(vcfFile.getId());

        Track<Variation> trackResult = testLoad(vcfFile, 1D, true);
        Assert.assertFalse(trackResult.getBlocks().isEmpty());

        VcfFile filesByReference = vcfFileManager.load(vcfFile.getId());
        Assert.assertNotNull(filesByReference);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadStructuralVariations()
            throws IOException, InterruptedException, NoSuchAlgorithmException,
                   FeatureFileReadingException {
        Resource refResource = context.getResource("classpath:templates/A3.fa");

        ReferenceRegistrationRequest refRequest = new ReferenceRegistrationRequest();
        refRequest.setName(testReference.getName() + this.getClass().getSimpleName());
        refRequest.setPath(refResource.getFile().getPath());

        Reference reference = referenceManager.registerGenome(refRequest);

        Resource resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(reference.getId());
        request.setPath(resource.getFile().getAbsolutePath());

        VcfFile vcfFile = vcfManager.registerVcfFile(request);
        Assert.assertNotNull(vcfFile);
        Assert.assertNotNull(vcfFile.getId());

        Track<Variation> trackResult = testLoad(vcfFile, 1D, true);
        Assert.assertFalse(trackResult.getBlocks().isEmpty());
        List<VariationType> structTypes = Arrays.asList(VariationType.INS, VariationType.DEL,
                VariationType.DUP, VariationType.INV, VariationType.BND);
        List<Variation> structVars = trackResult.getBlocks().stream()
                .filter(v -> structTypes.contains(v.getType()) && v.isStructural())
                .collect(Collectors.toList());
        Assert.assertFalse(structVars.isEmpty());

        Variation bindVar = structVars.stream().filter(v -> v.getType() == VariationType.BND).findAny().get();
        Assert.assertNotNull(bindVar);
        Assert.assertNotNull(bindVar.getBindInfo().get("CIPOS"));

        VariationQuery query = new VariationQuery();
        query.setChromosomeId(testChromosome.getId());
        query.setId(vcfFile.getId());
        query.setPosition(bindVar.getStartIndex());

        Variation bindVarInfo = vcfManager.loadVariation(query);
        Assert.assertNotNull(bindVarInfo.getBindInfo().get("BIND_CHR"));
        Assert.assertNotNull(bindVarInfo.getBindInfo().get("BIND_POS"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRegisterGA4GH()
        throws IOException, ClassNotFoundException, InterruptedException, ParseException, NoSuchAlgorithmException,
               ExternalDbUnavailableException, VcfReadingException {

        String fetchRes1 = readFile("GA4GH_id10473.json");
        String fetchRes2 = readFile("GA4GH_id10473_variant.json");
        Mockito.when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(JSONObject.class)))
                .thenReturn(fetchRes1)
                .thenReturn(fetchRes2);

        String fetchRes5 = readFile("GA4GH_id10473_param.json");
        Mockito.when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(ParameterNameValue[].class)))
                .thenReturn(fetchRes5);




        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        VcfFile vcfFileGA4GH = regesterVcfGA4GH();
        vcfFileGA4GH.setType(BiologicalDataItemResourceType.GA4GH);
        List<VcfSample> vcfSamples = vcfFileGA4GH.getSamples();
        Long sampleId = 0L;
        for (VcfSample sample : vcfSamples) {
            if (sample.getName().equals(SAMPLE_NAME)) {
                sampleId = sample.getId();
            }
        }
        Track<Variation> trackResultGA4GH = testLoadGA4GH(vcfFileGA4GH, 1D, true, sampleId);

        VariationQuery query = new VariationQuery();
        query.setChromosomeId(testChrGA4GH.getId());
        query.setId(vcfFileGA4GH.getId());

        Assert.assertNotNull(vcfFileGA4GH);
        Assert.assertNotNull(vcfFileGA4GH.getId());
        Assert.assertFalse(trackResultGA4GH.getBlocks().isEmpty());

        Resource resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF_GOOGLE);
        request.setReferenceId(referenceIdGA4GH);
        request.setPath(resource.getFile().getAbsolutePath());
        request.setType(BiologicalDataItemResourceType.FILE);
        VcfFile vcfFileFile = vcfManager.registerVcfFile(request);
        Assert.assertNotNull(vcfFileFile);
        Assert.assertNotNull(vcfFileFile.getId());

        vcfSamples = vcfFileFile.getSamples();
        for (VcfSample sample : vcfSamples) {
            if (sample.getName().equals(SAMPLE_NAME)) {
                sampleId = sample.getId();
            }
        }

        Track<Variation> trackResultFile = testLoadGA4GH(vcfFileFile, 1D, true, sampleId);
        Assert.assertFalse(trackResultFile.getBlocks().isEmpty());

        Variation varGA4GH = trackResultGA4GH.getBlocks().get(0);
        Variation varFILE = trackResultFile.getBlocks().get(0);

        Assert.assertEquals(varGA4GH.getInfo().get("AC").getValue(), varFILE.getInfo().get("AC").getValue());
        Assert.assertEquals(varGA4GH.getGenotypeData().getGenotype().length, varFILE.getGenotypeData().getGenotype()
                .length);
        Assert.assertEquals(varGA4GH.getGenotypeData().getOrganismType(), varFILE.getGenotypeData().getOrganismType());
        Assert.assertEquals(varGA4GH.getGenotypeData().getGenotypeString(), varFILE.getGenotypeData()
                .getGenotypeString());
        Assert.assertEquals(varGA4GH.getType(), varFILE.getType());
        Assert.assertEquals(varGA4GH.getStartIndex(), varFILE.getStartIndex());
        Assert.assertEquals(varGA4GH.getReferenceAllele(), varFILE.getReferenceAllele());
        Assert.assertEquals(varGA4GH.getGeneNames(), varFILE.getGeneNames());

    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testUnregisterVcfFile()
        throws IOException, InterruptedException, NoSuchAlgorithmException, VcfReadingException {
        // Register vcf file.
        Resource resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        VcfFile vcfFile = vcfManager.registerVcfFile(request);
        Assert.assertNotNull(vcfFile);
        Assert.assertNotNull(vcfFile.getId());

        // Unregister vcf file.
        VcfFile deletedVcfFile = vcfManager.unregisterVcfFile(vcfFile.getId());
        Assert.assertNotNull(vcfFile);
        Assert.assertNotNull(vcfFile.getId());
        Assert.assertEquals(vcfFile.getId(), deletedVcfFile.getId());

        List<BiologicalDataItem> indexItems = biologicalDataItemDao.loadBiologicalDataItemsByIds(
                Arrays.asList(vcfFile.getIndex().getId(), vcfFile.getBioDataItemId()));
        Assert.assertTrue(indexItems.isEmpty());

        // Check. Should me IllegalArgumentException
        testLoad(vcfFile, 1D, false);

        request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceIdGA4GH);
        request.setType(BiologicalDataItemResourceType.GA4GH);
        request.setPath(varSet);
        VcfFile vcfFileGA4GH = vcfManager.registerVcfFile(request);
        vcfFileGA4GH.setType(BiologicalDataItemResourceType.GA4GH);

        Assert.assertNotNull(vcfFileGA4GH);
        Assert.assertNotNull(vcfFileGA4GH.getId());

        // Unregister vcf file.
        deletedVcfFile = vcfManager.unregisterVcfFile(vcfFileGA4GH.getId());
        Assert.assertNotNull(vcfFileGA4GH);
        Assert.assertNotNull(vcfFileGA4GH.getId());
        Assert.assertEquals(vcfFileGA4GH.getId(), deletedVcfFile.getId());

        indexItems = biologicalDataItemDao.loadBiologicalDataItemsByIds(
                Arrays.asList(vcfFileGA4GH.getIndex().getId(), vcfFileGA4GH.getBioDataItemId()));
        Assert.assertTrue(indexItems.isEmpty());

        // Check. Should me IllegalArgumentException
        testLoadGA4GH(vcfFileGA4GH, 1D, false, null);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testGetNextFeature() throws IOException, InterruptedException, NoSuchAlgorithmException,
                                            ExternalDbUnavailableException, VcfReadingException {


        String fetchRes1 = readFile("GA4GH_id10473.json");
        String fetchRes2 = readFile("GA4GH_id10473_variant.json");
        String fetchRes3 = readFile("GA4GH_id10473_variant_2.json");
        String fetchRes4 = readFile("GA4GH_id10473_variant_3.json");
        Mockito.when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(JSONObject.class)))
                .thenReturn(fetchRes1)
                .thenReturn(fetchRes2)
                .thenReturn(fetchRes3)
                .thenReturn(fetchRes4);

        String fetchRes5 = readFile("GA4GH_id10473_param.json");
        Mockito.when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(ParameterNameValue[].class)))
                .thenReturn(fetchRes5);


        getNextFeature(referenceId, BiologicalDataItemResourceType.FILE);
        logger.info("success, next feature variation for file");
        //test getNextFeature for GA4GH
        getNextFeature(referenceIdGA4GH, BiologicalDataItemResourceType.GA4GH);
        logger.info("success, next feature variation for GA4GH");
    }

    @Test
    @Ignore
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testSaveLoadUrl() throws Exception {
        final String path = "/Felis_catus.vcf";
        String vcfUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + path;
        String indexUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + "/Felis_catus.idx";

        Server server = UrlTestingUtils.getFileServer(context);
        try {
            server.start();

            FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
            request.setReferenceId(referenceId);
            request.setPath(vcfUrl);
            request.setIndexPath(indexUrl);
            request.setIndexType(BiologicalDataItemResourceType.URL);
            request.setType(BiologicalDataItemResourceType.URL);

            VcfFile vcfFile = vcfManager.registerVcfFile(request);
            Assert.assertNotNull(vcfFile);
            Assert.assertNotNull(vcfFile.getId());
            Assert.assertEquals(BiologicalDataItemResourceType.URL, vcfFile.getType());
            Assert.assertEquals(vcfUrl, vcfFile.getPath());
            Assert.assertEquals(BiologicalDataItemResourceType.URL, vcfFile.getIndex().getType());

            testLoad(vcfFile, 1D, true);

            // index as file
            Resource resource = context.getResource("classpath:templates/Felis_catus.idx");
            request.setIndexPath(resource.getFile().getAbsolutePath());
            request.setIndexType(null);

            vcfFile = vcfManager.registerVcfFile(request);
            Assert.assertEquals(BiologicalDataItemResourceType.FILE, vcfFile.getIndex().getType());

            testLoad(vcfFile, 1D, true);

            // Compressed file
            vcfUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + "/Felis_catus.vcf.gz";
            indexUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + "/Felis_catus.tbi";
            request = new FeatureIndexedFileRegistrationRequest();
            request.setReferenceId(referenceId);
            request.setPath(vcfUrl);
            request.setIndexPath(indexUrl);
            request.setIndexType(BiologicalDataItemResourceType.URL);
            request.setType(BiologicalDataItemResourceType.URL);

            vcfFile = vcfManager.registerVcfFile(request);
            testLoad(vcfFile, 1D, true);
        } finally {
            server.stop();
        }
    }

    @Test
    @Ignore
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadUrlNoRegistration() throws Exception {
        final String path = "/Felis_catus.vcf";
        String vcfUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + path;
        String indexUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + "/Felis_catus.idx";

        Server server = UrlTestingUtils.getFileServer(context);
        try {
            server.start();

            TrackQuery vcfTrackQuery = new TrackQuery();
            vcfTrackQuery.setChromosomeId(testChromosome.getId());
            vcfTrackQuery.setStartIndex(1);
            vcfTrackQuery.setEndIndex(TEST_END_INDEX);
            vcfTrackQuery.setScaleFactor(1D);

            Track<Variation> variationTrack = Query2TrackConverter.convertToTrack(vcfTrackQuery);

            Track<Variation> trackResult = vcfManager.loadVariations(variationTrack, vcfUrl, indexUrl,
                                                                     null, true, true);
            Assert.assertFalse(trackResult.getBlocks().isEmpty());

            Variation var = vcfManager.getNextOrPreviousVariation(trackResult.getBlocks().get(3).getEndIndex(), null,
                                                  null, testChromosome.getId(), true, vcfUrl, indexUrl);
            Assert.assertNotNull(var);
            Assert.assertEquals(var.getStartIndex(), trackResult.getBlocks().get(4).getStartIndex());
            Assert.assertEquals(var.getEndIndex(), trackResult.getBlocks().get(4).getEndIndex());
        } finally {
            server.stop();
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadExtendedSummaryUrl()
        throws Exception {
        final String path = "/Felis_catus.vcf";
        String vcfUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + path;
        String indexUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + "/Felis_catus.idx";

        Resource resource = context.getResource("classpath:templates/genes_sorted.gtf");

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), geneFile.getId());

        Server server = UrlTestingUtils.getFileServer(context);
        try {
            server.start();
            final VariationQuery query = new VariationQuery();
            query.setPosition(GENE_POSTION);
            query.setChromosomeId(testChromosome.getId());
            Variation variation = vcfManager.loadVariation(query, vcfUrl, indexUrl);

            Assert.assertFalse(variation.getInfo().isEmpty());
            Assert.assertFalse(variation.getInfo().isEmpty());
            Assert.assertNotNull(variation.getGeneNames());
            Assert.assertFalse(variation.getGeneNames().isEmpty());
        } finally {
            server.stop();
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadExtendedInfo() throws IOException, InterruptedException {
        VcfFile vcfFile = testSave("classpath:templates/extended_info.vcf");

        VcfFile file = vcfFileManager.load(vcfFile.getId());
        Assert.assertNotNull(file);

        VcfFilterInfo filterInfo = vcfManager.getFiltersInfo(Collections.singleton(vcfFile.getId()));
        Assert.assertEquals(NUMBER_OF_FILTERS, filterInfo.getAvailableFilters().size());
        Assert.assertEquals(NUMBER_OF_TRIVIAL_INFO, filterInfo.getInfoItems().size() - 1);
        Assert.assertEquals(NUMBER_OF_TRIVIAL_INFO, filterInfo.getInfoItemMap().size() - 1); // -1 refers to is_exon
                                                                                    // item which is added externally
    }

    private void getNextFeature(Long reference, BiologicalDataItemResourceType type) throws IOException,
                                                                                            InterruptedException,
                                                                                            NoSuchAlgorithmException,
                                                                                            VcfReadingException {
        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();

        switch (type) {
            case GA4GH: {
                request.setPath(varSet);
                request.setType(BiologicalDataItemResourceType.GA4GH);
                break;
            }
            default: {
                Resource resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);
                request.setType(BiologicalDataItemResourceType.FILE);
                request.setPath(resource.getFile().getAbsolutePath());
                break;
            }
        }
        request.setReferenceId(reference);

        VcfFile vcfFile = vcfManager.registerVcfFile(request);
        Assert.assertNotNull(vcfFile);
        Assert.assertNotNull(vcfFile.getId());
        Track<Variation> trackResult;
        Long sampleId = 0L;
        switch (type) {
            case GA4GH: {
                List<VcfSample> vcfSamples = vcfFile.getSamples();
                for (VcfSample sample : vcfSamples) {
                    if (sample.getName().equals(SAMPLE_NAME)) {
                        sampleId = sample.getId();
                    }
                }
                trackResult = testLoadGA4GH(vcfFile, 1D, true, sampleId);
                break;
            }
            default: {
                trackResult = testLoad(vcfFile, 1D, true);
                Assert.assertFalse(trackResult.getBlocks().isEmpty());
            }
        }
        int middle = trackResult.getBlocks().size() / 2;
        Variation var1 = trackResult.getBlocks().get(middle);
        Variation var2 = trackResult.getBlocks().get(middle + 1);

        double time1 = Utils.getSystemTimeMilliseconds();
        Variation loadedNextVar;
        switch (type) {
            case GA4GH: {
                loadedNextVar = vcfManager.getNextOrPreviousVariation(var1.getEndIndex(), vcfFile.getId(), sampleId,
                        testChrGA4GH.getId(), true, null, null);
                break;
            }
            default: {
                loadedNextVar = vcfManager.getNextOrPreviousVariation(var1.getEndIndex(), vcfFile.getId(), null,
                        testChromosome.getId(), true, null, null);
            }
        }
        double time2 = Utils.getSystemTimeMilliseconds();
        logger.info("next feature took {} ms", time2 - time1);
        Assert.assertNotNull(loadedNextVar);
        Assert.assertEquals(var2.getStartIndex(), loadedNextVar.getStartIndex());
        Assert.assertEquals(var2.getEndIndex(), loadedNextVar.getEndIndex());

        time1 = Utils.getSystemTimeMilliseconds();
        Variation loadedPrevVar;
        switch (type) {
            case GA4GH: {
                loadedPrevVar = vcfManager.getNextOrPreviousVariation(var2.getStartIndex(), vcfFile.getId(), sampleId,
                        testChrGA4GH.getId(), false, null, null);
                break;
            }
            default: {
                loadedPrevVar = vcfManager.getNextOrPreviousVariation(var2.getStartIndex(), vcfFile.getId(), null,
                        testChromosome.getId(), false, null, null);
                break;
            }
        }
        time2 = Utils.getSystemTimeMilliseconds();
        logger.info("prev feature took {} ms", time2 - time1);
        Assert.assertNotNull(loadedNextVar);
        Assert.assertEquals(var1.getStartIndex(), loadedPrevVar.getStartIndex());
        Assert.assertEquals(var1.getEndIndex(), loadedPrevVar.getEndIndex());
    }

    private VcfFile testSave(String filePath) throws IOException, InterruptedException {
        Resource resource = context.getResource(filePath);
        return registerVcf(resource, referenceId, vcfManager, PRETTY_NAME);
    }

    private Track<Variation> testLoad(VcfFile vcfFile, Double scaleFactor, boolean checkBlocks) throws IOException {
        return testLoad(vcfFile, scaleFactor, checkBlocks, true);
    }

    private Track<Variation> testLoad(VcfFile vcfFile, Double scaleFactor, boolean checkBlocks, boolean collapse)
        throws IOException {
        TrackQuery vcfTrackQuery = new TrackQuery();
        vcfTrackQuery.setChromosomeId(testChromosome.getId());
        vcfTrackQuery.setStartIndex(1);
        vcfTrackQuery.setEndIndex(TEST_END_INDEX);
        vcfTrackQuery.setId(vcfFile.getId());
        vcfTrackQuery.setScaleFactor(scaleFactor);

        Track<Variation> variationTrack = Query2TrackConverter.convertToTrack(vcfTrackQuery);

        double time1 = Utils.getSystemTimeMilliseconds();
        Track<Variation> trackResult = vcfManager.loadVariations(variationTrack, null, true, collapse);
        double time2 = Utils.getSystemTimeMilliseconds();
        logger.debug("Loading VCF records took {} ms", time2 - time1);

        if (checkBlocks) {
            Assert.assertFalse(trackResult.getBlocks().isEmpty());
        }

        return trackResult;
    }

    private Track<Variation> testLoadGA4GH(VcfFile vcfFile, Double scaleFactor, boolean checkBlocks, Long sampleIndex)
        throws VcfReadingException {
        TrackQuery vcfTrackQuery = new TrackQuery();
        vcfTrackQuery.setChromosomeId(testChrGA4GH.getId());
        vcfTrackQuery.setEndIndex(end);
        vcfTrackQuery.setStartIndex(start);
        vcfTrackQuery.setScaleFactor(scaleFactor);
        vcfTrackQuery.setId(vcfFile.getId());

        Track<Variation> variationTrack = Query2TrackConverter.convertToTrack(vcfTrackQuery);

        if (vcfFile.getType() == BiologicalDataItemResourceType.GA4GH) {
            variationTrack.setType(TrackType.GA4GH);
        }
        Track<Variation> trackResult = vcfManager.loadVariations(variationTrack, sampleIndex, true, true);

        if (checkBlocks) {
            Assert.assertFalse(trackResult.getBlocks().isEmpty());
        }

        return trackResult;
    }

    /*@After
    public void clear() throws IOException {
        String contentsDir = fileManager.getBaseDirPath();
        File file = new File(contentsDir);
        if (file.exists() && file.isDirectory()) {
            FileUtils.deleteDirectory(file);
        }
    }*/

    private String readFile(String filename) throws IOException {
        Resource resource = context.getResource("classpath:externaldb//data//" + filename);
        String pathStr = resource.getFile().getPath();
        return new String(Files.readAllBytes(Paths.get(pathStr)), Charset.defaultCharset());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveUnsorted() throws IOException {
        String invalidVcf = "unsorted.vcf";
        testRegisterInvalidFile("classpath:templates/invalid/" + invalidVcf,  MessageHelper
                .getMessage(MessagesConstants.ERROR_UNSORTED_FILE));

        Assert.assertTrue(biologicalDataItemDao
                .loadFilesByNameStrict(invalidVcf).isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRegisterFileExtraChr() throws IOException, InterruptedException {
        VcfFile vcfFile = testSave("classpath:templates/invalid/extra_chr.vcf");
        Assert.assertTrue(vcfFile != null);
    }


    private void testRegisterInvalidFile(String path, String expectedMessage) throws IOException {
        String errorMessage = "";
        try {
            Resource resource = context.getResource(path);
            FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
            request.setPath(resource.getFile().getAbsolutePath());
            request.setReferenceId(referenceId);
            vcfManager.registerVcfFile(request);
        } catch (TribbleException | IllegalArgumentException | AssertionError e) {
            errorMessage = e.getMessage();
        }
        //check that we received an appropriate message
        Assert.assertTrue(errorMessage.contains(expectedMessage));
    }

    public static VcfFile registerVcf(Resource vcfFile, Long referenceId, VcfManager vcfManager,
            String prettyName) throws IOException {
        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(vcfFile.getFile().getAbsolutePath());
        request.setPrettyName(prettyName);
        return vcfManager.registerVcfFile(request);
    }

}
