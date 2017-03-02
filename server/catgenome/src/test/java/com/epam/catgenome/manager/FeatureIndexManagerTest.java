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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.dao.index.field.IndexSortField;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.index.BookmarkIndexEntry;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.Group;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.project.ProjectItem;
import com.epam.catgenome.entity.reference.Bookmark;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.vcf.InfoItem;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationQuery;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.VcfReadingException;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.project.ProjectManager;
import com.epam.catgenome.manager.reference.BookmarkManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import com.epam.catgenome.util.TestUtils;
import com.epam.catgenome.util.Utils;

/**
 * Source:      FeatureIndexManagerTest
 * Created:     29.04.16, 17:11
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class FeatureIndexManagerTest extends AbstractManagerTest {
    private static final String CLASSPATH_TEMPLATES_FELIS_CATUS_VCF = "classpath:templates/Felis_catus.vcf";
    private static final String CLASSPATH_TEMPLATES_GENES_SORTED = "classpath:templates/genes_sorted.gtf";
    private static final int SVLEN_VALUE = -150;
    //public static final float QUAL_VALUE = -10.0F;
    private static final int CONST_42 = 42;
    private static final int TEST_WICKED_VCF_LENGTH = 248617560;
    private static final int PERFORMANCE_TEST_WARMING_COUNT = 20;
    private static final int PERFORMANCE_TEST_ATTEMPTS_COUNT = 20;
    private static final int PERFORMANCE_TEST_PAGE_SIZE = 20;
    private static final int INTERVAL1_START = 400_000;
    private static final int INTERVAL1_END = 500_000;
    private static final int INTERVAL2_START = 550_000;
    private static final int INTERVAL2_END = 650_000;
    private static final int INTERVAL3_START = 35470;
    private static final int INTERVAL3_END = 35490;

    private Logger logger = LoggerFactory.getLogger(FeatureIndexManagerTest.class);

    @Autowired
    private VcfManager vcfManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private GffManager gffManager;

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private FeatureIndexManager featureIndexManager;

    @Autowired
    private FeatureIndexDao featureIndexDao;

    @Autowired
    private BookmarkManager bookmarkManager;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private ApplicationContext context;

    private static final int TEST_CHROMOSOME_SIZE = 239107476;
    private static final String TEST_PROJECT_NAME = "testProject1";
    private static final String TEST_GENE_PREFIX = "ENS";
    private static final String TEST_GENE_NAME = "pglyrp4";
    private static final String TEST_GENE_AND_FILE_ID_QUERY = "geneId:ENS* AND fileId:%d";
    private static final String SVTYPE_FIELD = "SVTYPE";
    private static final String SVLEN_FIELD = "SVLEN";

    private long referenceId;
    private Reference testReference;
    private Chromosome testChromosome;

    private VcfFile testVcf;
    private GeneFile testGeneFile;
    private Project testProject;

    @Before
    public void setup() throws Exception {
        testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        testReference = EntityHelper.createNewReference(testChromosome, referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(testReference);
        referenceId = testReference.getId();

        Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        testGeneFile = gffManager.registerGeneFile(request);

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), testGeneFile.getId());

        resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        testVcf = vcfManager.registerVcfFile(request);

        testProject = new Project();
        testProject.setName(TEST_PROJECT_NAME);
        testProject.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(testVcf.getBioDataItemId())),
                                           new ProjectItem(testReference)));

        projectManager.saveProject(testProject); // Index is created when vcf file is added
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testCreateFeatureIndex() throws Exception {
        List<FeatureIndexEntry> entryList = (List<FeatureIndexEntry>) featureIndexDao.searchFileIndexes(
                Collections.singletonList(testVcf),  String.format(TEST_GENE_AND_FILE_ID_QUERY, testVcf.getId()),
                null).getEntries();
        Assert.assertFalse(entryList.isEmpty());

        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(testVcf.getId()));
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm.setVariationTypes(new VcfFilterForm.FilterSection<>(Arrays.asList(VariationType.MNP,
                VariationType.SNV), false));
        //vcfFilterForm.setQuality(Collections.singletonList(QUAL_VALUE));
        IndexSearchResult<VcfIndexEntry> entryList2 = featureIndexManager.filterVariations(vcfFilterForm,
                                                                                           testProject.getId());
        Assert.assertFalse(entryList2.getEntries().isEmpty());
        Assert.assertTrue(entryList2.getEntries().stream().anyMatch(VcfIndexEntry::getExon));

        vcfFilterForm.setChromosomeId(testChromosome.getId());
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm, testProject.getId());
        Assert.assertFalse(entryList2.getEntries().isEmpty());

        double time1 = Utils.getSystemTimeMilliseconds();
        List<Long> chromosomeIds = featureIndexDao.getChromosomeIdsWhereVariationsPresentFacet(
            Collections.singletonList(testVcf), "geneId:ENS* AND fileId:" + testVcf.getId() + " AND variationType:snv");
        double time2 = Utils.getSystemTimeMilliseconds();
        logger.info("Get chromosomes by facets time: {} ms", time2 - time1);

        Assert.assertFalse(chromosomeIds.isEmpty());

        List<Chromosome> chromosomes = featureIndexManager.filterChromosomes(vcfFilterForm, testProject.getId());
        Assert.assertFalse(chromosomes.isEmpty());

        // filter by additional fields
        Map<String, Object> additionalFilters = new HashMap<>();
        additionalFilters.put(SVTYPE_FIELD, "DEL");
        //additionalFilters.put("SVLEN", SVLEN_VALUE);
        additionalFilters.put(SVLEN_FIELD, Arrays.asList(null, SVLEN_VALUE));
        vcfFilterForm.setAdditionalFilters(additionalFilters);
        vcfFilterForm.setGenes(null);
        vcfFilterForm.setVariationTypes(null);
        vcfFilterForm.setInfoFields(Arrays.asList(SVTYPE_FIELD, SVLEN_FIELD));
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm, testProject.getId());
        Assert.assertFalse(entryList2.getEntries().isEmpty());
        Assert.assertFalse(entryList2.getEntries().stream().anyMatch(e -> e.getInfo().isEmpty()));

        Set<String> genes = featureIndexManager.searchGenesInVcfFilesInProject(testProject.getId(), TEST_GENE_PREFIX,
                Collections.singletonList(testVcf.getId()));
        Assert.assertFalse(genes.isEmpty());

        // search by gene name pglyrp4
        vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList(TEST_GENE_NAME)));
        IndexSearchResult<VcfIndexEntry> entries = featureIndexManager.filterVariations(vcfFilterForm,
                                                                                        testProject.getId());
        Assert.assertFalse(entries.getEntries().isEmpty());

        genes = featureIndexManager.searchGenesInVcfFilesInProject(testProject.getId(), TEST_GENE_NAME,
                Collections.singletonList(testVcf.getId()));
        Assert.assertFalse(genes.isEmpty());

        vcfFilterForm.setPageSize(1);
        int totalCount = featureIndexManager.getTotalPagesCount(vcfFilterForm, testProject.getId());
        Assert.assertEquals(entries.getEntries().size(), totalCount);

        // search exons
        vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setExon(true);
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm, testProject.getId());
        Assert.assertFalse(entryList2.getEntries().isEmpty());
        Assert.assertTrue(entryList2.getEntries().stream().allMatch(VcfIndexEntry::getExon));

        // check duplicates
        entryList2 = featureIndexManager.filterVariations(new VcfFilterForm(), testProject.getId());
        checkDuplicates(entryList2.getEntries());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadAllFields() throws IOException {
        VcfFilterForm filterForm = new VcfFilterForm();
        VcfFilterInfo info = vcfManager.getFiltersInfo(Collections.singletonList(testVcf.getId()));
        filterForm.setInfoFields(info.getInfoItems().stream().map(InfoItem::getName).collect(Collectors.toList()));

        IndexSearchResult<VcfIndexEntry> entries = featureIndexManager.filterVariations(filterForm,
                                                                                        testProject.getId());
        Assert.assertFalse(entries.getEntries().isEmpty());
    }

    /**
     * Testes indexing a vcf file with populated gene information. Therefore this information should be read from
     * VCF, not from gff files
     * @throws Exception
     */
    @Ignore
    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testCreateFeatureIndex2() throws Exception {
        Chromosome chr14 = EntityHelper.createNewChromosome("chr14");
        chr14.setSize(TEST_CHROMOSOME_SIZE);
        Reference testHumanReference = EntityHelper.createNewReference(chr14,
                referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(testHumanReference);
        Long humanReferenceId = testHumanReference.getId();

        Resource resource = context.getResource("classpath:templates/sample_2-lumpy.vcf");
        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(humanReferenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        VcfFile vcfFile = vcfManager.registerVcfFile(request);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
        project.setItems(Collections.singletonList(new ProjectItem(new BiologicalDataItem(
                vcfFile.getBioDataItemId()))));

        projectManager.saveProject(project); // Index is created when vcf file is added
        VcfFilterInfo info = featureIndexManager.loadVcfFilterInfoForProject(project.getId());

        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(vcfFile.getId()));
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList("ENSG00000185070"), false));
        vcfFilterForm.setVariationTypes(new VcfFilterForm.FilterSection<>(Arrays.asList(VariationType.DEL,
                VariationType.SNV), false));

        String cipos95 = "CIPOS95";
        vcfFilterForm.setInfoFields(info.getInfoItems().stream().map(InfoItem::getName).collect(Collectors.toList()));
        vcfFilterForm.setAdditionalFilters(Collections.singletonMap(cipos95, Arrays.asList(CONST_42, CONST_42)));

        IndexSearchResult<VcfIndexEntry> entryList2 = featureIndexManager.filterVariations(vcfFilterForm,
                                                                                           project.getId());
        Assert.assertFalse(entryList2.getEntries().isEmpty());
        Assert.assertTrue(entryList2.getEntries().stream().anyMatch(e -> e.getInfo().containsKey(cipos95)));
        Assert.assertTrue(entryList2.getEntries().stream().filter(e -> e.getInfo().containsKey(cipos95)).allMatch(e -> {
            String cipos = (String) e.getInfo().get(cipos95);
            return cipos.startsWith("[") && cipos.endsWith("]");
        }));

        // check info properly loaded
        for (VcfIndexEntry e : entryList2.getEntries()) {
            VariationQuery query = new VariationQuery();
            query.setId(e.getFeatureFileId());
            query.setProjectId(project.getId());
            query.setChromosomeId(e.getChromosome().getId());
            query.setPosition(e.getStartIndex());
            Variation variation = vcfManager.loadVariation(query);
            Assert.assertNotNull(variation);

            for (Map.Entry<String, Variation.InfoField> i : variation.getInfo().entrySet()) {
                if (i.getValue().getValue() != null) {
                    Assert.assertTrue(String.format("%s expected, %s found", i.getValue().getValue(),
                            e.getInfo().get(i.getKey())),
                            i.getValue().getValue().toString().equalsIgnoreCase(
                                    e.getInfo().get(i.getKey()).toString()));
                } else {
                    Assert.assertEquals(i.getValue().getValue(), e.getInfo().get(i.getKey()));
                }
            }
        }

        // flrt2

        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList("FLRT2"), false));
        IndexSearchResult<VcfIndexEntry> entryList21 = featureIndexManager.filterVariations(vcfFilterForm,
                                                                                            project.getId());
        Assert.assertFalse(entryList21.getEntries().isEmpty());
        Assert.assertEquals(entryList21.getEntries().size(), entryList2.getEntries().size());
        Assert.assertEquals(entryList21.getEntries().get(0).getGene(), entryList2.getEntries().get(0).getGene());

        // empty filter test
        entryList2 = featureIndexManager.filterVariations(new VcfFilterForm(), project.getId());
        Assert.assertFalse(entryList2.getEntries().isEmpty());
        checkDuplicates(entryList2.getEntries());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testIndexUpdateOnProjectOperations() throws Exception {
        Resource gffResource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(gffResource.getFile().getAbsolutePath());
        request.setName("testGeneFile");

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), geneFile.getId());

        Resource vcfResource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(vcfResource.getFile().getAbsolutePath());
        request.setName("testVcf");

        VcfFile vcfFile = vcfManager.registerVcfFile(request);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME + 1);
        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(vcfFile.getBioDataItemId())),
                new ProjectItem(new BiologicalDataItem(geneFile.getBioDataItemId()))));

        projectManager.saveProject(project); // Index is created when vcf file is added

        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(vcfFile.getId()));
        vcfFilterForm.setChromosomeId(testChromosome.getId());
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm.setVariationTypes(new VcfFilterForm.FilterSection<>(Arrays.asList(VariationType.MNP,
                VariationType.SNV), false));
        IndexSearchResult<VcfIndexEntry> entryList = featureIndexManager.filterVariations(vcfFilterForm,
                                                                                          project.getId());
        Assert.assertFalse(entryList.getEntries().isEmpty());

        // try to add an vcf item
        request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(vcfResource.getFile().getAbsolutePath());
        request.setName(vcfResource.getFilename() + "2");
        VcfFile vcfFile2 = vcfManager.registerVcfFile(request);

        project = projectManager.addProjectItem(project.getId(), vcfFile2.getBioDataItemId());

        entryList = featureIndexManager.filterVariations(vcfFilterForm, project.getId());
        Assert.assertFalse(entryList.getEntries().isEmpty());
        Assert.assertTrue(entryList.getEntries().stream().allMatch(e -> e.getFeatureFileId().equals(vcfFile.getId())));


        VcfFilterForm vcfFilterForm2 = new VcfFilterForm();
        vcfFilterForm2.setVcfFileIds(Collections.singletonList(vcfFile2.getId()));
        vcfFilterForm2.setChromosomeId(testChromosome.getId());
        vcfFilterForm2.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm2.setVariationTypes(new VcfFilterForm.FilterSection<>(Arrays.asList(VariationType.MNP,
                VariationType.SNV), false));
        IndexSearchResult<VcfIndexEntry> entryList2 = featureIndexManager.filterVariations(vcfFilterForm2,
                                                                                           project.getId());
        Assert.assertFalse(entryList2.getEntries().isEmpty());
        Assert.assertEquals(entryList.getEntries().size(), entryList2.getEntries().size());

        Assert.assertTrue(entryList2.getEntries().stream().allMatch(e -> e.getFeatureFileId().equals(
            vcfFile2.getId())));

        // test no vcfFileIds
        vcfFilterForm2.setVcfFileIds(null);
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm2, project.getId());
        Assert.assertFalse(entryList2.getEntries().isEmpty());
        Assert.assertEquals(entryList2.getEntries().size(), entryList.getEntries().size() * 2);

        // test with multiple vcfFileIds
        vcfFilterForm2.setVcfFileIds(Arrays.asList(vcfFile.getId(), vcfFile2.getId()));
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm2, project.getId());
        Assert.assertFalse(entryList2.getEntries().isEmpty());
        Assert.assertEquals(entryList2.getEntries().size(), entryList.getEntries().size() * 2);

        // try to remove a vcf item by save - should be not indexed
        project.setItems(project.getItems().stream()
                .filter(i -> !(i.getBioDataItem() instanceof VcfFile) || !((VcfFile) i.getBioDataItem()).getId().equals(
                        vcfFile2.getId()))
                .collect(Collectors.toList()));

        project = projectManager.saveProject(project);

        vcfFilterForm2.setVcfFileIds(Collections.singletonList(vcfFile2.getId()));
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm2, project.getId());
        Assert.assertTrue(entryList2.getEntries().isEmpty());

        // try to remove gene file
        project.setItems(project.getItems().stream()
                .filter(i -> !(i.getBioDataItem() instanceof GeneFile))
                .collect(Collectors.toList()));
        project = projectManager.saveProject(project);

        vcfFilterForm.setGenes(null);
        entryList = featureIndexManager.filterVariations(vcfFilterForm, project.getId());
        Assert.assertFalse(entryList.getEntries().isEmpty());

        // add multiple files
        project.getItems().clear();
        projectManager.saveProject(project);
        Project loadedProject = projectManager.loadProject(project.getId());
        Assert.assertTrue(loadedProject.getItems().isEmpty());
        entryList2 = featureIndexManager.filterVariations(new VcfFilterForm(), project.getId());
        Assert.assertTrue(entryList2.getEntries().isEmpty());

        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(vcfFile.getBioDataItemId())),
                new ProjectItem(new BiologicalDataItem(vcfFile2.getBioDataItemId()))));
        projectManager.saveProject(project);
        entryList2 = featureIndexManager.filterVariations(new VcfFilterForm(), project.getId());
        Assert.assertTrue(entryList2.getEntries().stream().anyMatch(e -> e.getFeatureFileId().equals(vcfFile.getId())));
        Assert.assertTrue(entryList2.getEntries().stream().anyMatch(e -> e.getFeatureFileId().equals(
            vcfFile2.getId())));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadVcfFilterInfoForProject()
        throws IOException, InterruptedException, NoSuchAlgorithmException, VcfReadingException, FeatureIndexException {
        VcfFilterInfo filterInfo = featureIndexManager.loadVcfFilterInfoForProject(testProject.getId());
        Assert.assertFalse(filterInfo.getAvailableFilters().isEmpty());
        Assert.assertFalse(filterInfo.getInfoItems().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testCreateGeneIndex() throws IOException, InterruptedException, FeatureIndexException,
                                             NoSuchAlgorithmException, VcfReadingException {
        IndexSearchResult searchResult = featureIndexManager.searchFeaturesInProject("", testProject.getId());
        Assert.assertTrue(searchResult.getEntries().isEmpty());

        searchResult = featureIndexManager.searchFeaturesInProject("ens", testProject.getId());
        Assert.assertFalse(searchResult.getEntries().isEmpty());
        Assert.assertTrue(searchResult.getEntries().size() <= 10);
        Assert.assertTrue(searchResult.isExceedsLimit());

        // ensfcag00000031547 and ccdc115
        searchResult = featureIndexManager.searchFeaturesInProject("ensfcag00000031547", testProject.getId());
        Assert.assertEquals(searchResult.getEntries().size(), 1);
        searchResult = featureIndexManager.searchFeaturesInProject("ccdc115", testProject.getId());
        Assert.assertEquals(searchResult.getEntries().size(), 2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testIntervalQuery() throws IOException, InterruptedException, FeatureIndexException,
            NoSuchAlgorithmException, VcfReadingException {
        Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setName("GENES_SORTED_INT");
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), geneFile.getId());

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME + "_INT");

        project.setItems(Arrays.asList(new ProjectItem(testReference), new ProjectItem(new BiologicalDataItem(
                geneFile.getBioDataItemId()))));
        projectManager.saveProject(project);

        IndexSearchResult result1 = featureIndexDao
                .searchFeaturesInInterval(Collections.singletonList(geneFile), INTERVAL1_START, INTERVAL1_END,
                        testChromosome);

        Assert.assertEquals(3, result1.getEntries().size());

        IndexSearchResult result2 = featureIndexDao
                .searchFeaturesInInterval(Collections.singletonList(geneFile), INTERVAL2_START, INTERVAL2_END,
                        testChromosome);
        Assert.assertEquals(0, result2.getEntries().size());

        IndexSearchResult result3 = featureIndexDao
                .searchFeaturesInInterval(Collections.singletonList(geneFile), INTERVAL3_START, INTERVAL3_END,
                        testChromosome);
        Assert.assertEquals(3, result3.getEntries().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testCreateUnmappedGeneIndex() throws IOException, InterruptedException, FeatureIndexException,
            NoSuchAlgorithmException {
        Chromosome chr1 = EntityHelper.createNewChromosome("chr1");
        chr1.setSize(TEST_CHROMOSOME_SIZE);
        Reference testHumanReference = EntityHelper.createNewReference(chr1,
                referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(testHumanReference);
        Long humanReferenceId = testHumanReference.getId();

        Resource resource = context.getResource("classpath:templates/mrna.sorted.chunk.gtf");

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(humanReferenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME + 1);

        project.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(geneFile.getBioDataItemId()))));
        projectManager.saveProject(project);

        List<FeatureIndexEntry> entryList = (List<FeatureIndexEntry>)
                featureIndexManager.searchFeaturesInProject("", project.getId()).getEntries();
        Assert.assertTrue(entryList.isEmpty());

        entryList = (List<FeatureIndexEntry>) featureIndexManager.searchFeaturesInProject("AM992871",
                                                                                          project.getId()).getEntries();
        Assert.assertTrue(entryList.isEmpty()); // we don't search for exons
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testBookmarkSearch() throws IOException, InterruptedException, FeatureIndexException {
        Bookmark bookmark = new Bookmark();
        bookmark.setChromosome(testChromosome);
        bookmark.setStartIndex(1);
        bookmark.setEndIndex(testChromosome.getSize());
        bookmark.setName("testBookmark");

        bookmarkManager.saveBookmark(bookmark);
        Bookmark loadedBookmark = bookmarkManager.loadBookmark(bookmark.getId());
        Assert.assertNotNull(loadedBookmark);

        IndexSearchResult<FeatureIndexEntry> result = featureIndexManager.searchFeaturesInProject(bookmark.getName(),
                                                                                                  testProject.getId());
        Assert.assertFalse(result.getEntries().isEmpty());
        Assert.assertEquals(result.getEntries().get(0).getFeatureType(), FeatureType.BOOKMARK);
        Assert.assertNotNull(((BookmarkIndexEntry) result.getEntries().get(0)).getBookmark());
        Assert.assertEquals(result.getEntries().size(), result.getTotalResultsCount());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testWickedVcfIndex() throws IOException, InterruptedException, FeatureIndexException,
                                            NoSuchAlgorithmException, ParseException, VcfReadingException {
        Chromosome chr1 = EntityHelper.createNewChromosome("chr21");
        chr1.setSize(TEST_WICKED_VCF_LENGTH);
        Chromosome chr2 = EntityHelper.createNewChromosome("chr22");
        chr2.setSize(TEST_WICKED_VCF_LENGTH);
        Reference testHumanReference = EntityHelper.createNewReference(Arrays.asList(chr1, chr2),
                referenceGenomeManager.createReferenceId());
        referenceGenomeManager.register(testHumanReference);
        Long humanReferenceId = testHumanReference.getId();

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME + 1);
        project.setItems(new ArrayList<>());

        projectManager.saveProject(project);

        Resource resource = context.getResource("classpath:templates/Homo_sapiens.GRCh38.83.sorted.chr21-22.gtf");
        FeatureIndexedFileRegistrationRequest geneRequest = new FeatureIndexedFileRegistrationRequest();
        geneRequest.setPath(resource.getFile().getAbsolutePath());
        geneRequest.setReferenceId(humanReferenceId);

        GeneFile geneFile = gffManager.registerGeneFile(geneRequest);
        referenceGenomeManager.updateReferenceGeneFileId(humanReferenceId, geneFile.getId());

        resource = context.getResource("classpath:templates/Dream.set3.VarDict.SV.vcf");

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(humanReferenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        VcfFile vcfFile = vcfManager.registerVcfFile(request);
        Assert.assertNotNull(vcfFile);
        Assert.assertNotNull(vcfFile.getId());

        project.setItems(Arrays.asList(new ProjectItem(geneFile), new ProjectItem(vcfFile)));

        projectManager.saveProject(project);

        IndexSearchResult<VcfIndexEntry> entries = featureIndexManager.filterVariations(new VcfFilterForm(),
                                                                                        project.getId());
        Assert.assertFalse(entries.getEntries().isEmpty());

        long varGenesCount = entries.getEntries().stream().filter(e -> StringUtils.isNotBlank(e.getGene())).count();
        Assert.assertTrue(varGenesCount > 0);
        /*entries.stream().filter(e -> StringUtils.isNotBlank(e.getGene())).forEach(e -> logger.info("{} - {}, {}", e
                .getStartIndex(), e.getEndIndex(), e.getGeneIds()));*/
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testSearchIndexForFile() throws IOException, FeatureIndexException {
        List<FeatureIndexEntry> entryList = (List<FeatureIndexEntry>) featureIndexDao.searchFileIndexes(
            Collections.singletonList(testVcf), String.format(TEST_GENE_AND_FILE_ID_QUERY, testVcf.getId()),
            null).getEntries();
        Assert.assertFalse(entryList.isEmpty());

        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(testVcf.getId()));
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm.setVariationTypes(new VcfFilterForm.FilterSection<>(Arrays.asList(VariationType.MNP,
                                                                                        VariationType.SNV), false));
        //vcfFilterForm.setQuality(Collections.singletonList(QUAL_VALUE));
        IndexSearchResult<VcfIndexEntry> entryList2 = featureIndexManager.filterVariations(vcfFilterForm);
        Assert.assertFalse(entryList2.getEntries().isEmpty());
        Assert.assertTrue(entryList2.getEntries().stream().anyMatch(VcfIndexEntry::getExon));

        vcfFilterForm.setChromosomeId(testChromosome.getId());
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm);
        Assert.assertFalse(entryList2.getEntries().isEmpty());

        double time1 = Utils.getSystemTimeMilliseconds();
        List<Long> chromosomeIds = featureIndexDao.getChromosomeIdsWhereVariationsPresentFacet(
            Collections.singletonList(testVcf), "geneId:ENS* AND fileId:" + testVcf.getId() +
                                                " AND variationType:snv");
        double time2 = Utils.getSystemTimeMilliseconds();
        logger.info("Get chromosomes by facets time: {} ms", time2 - time1);

        Assert.assertFalse(chromosomeIds.isEmpty());

        List<Chromosome> chromosomes = featureIndexManager.filterChromosomes(vcfFilterForm);
        Assert.assertFalse(chromosomes.isEmpty());

        // filter by additional fields
        Map<String, Object> additionalFilters = new HashMap<>();
        additionalFilters.put(SVTYPE_FIELD, "DEL");
        //additionalFilters.put("SVLEN", SVLEN_VALUE);
        additionalFilters.put(SVLEN_FIELD, Arrays.asList(null, SVLEN_VALUE));
        vcfFilterForm.setAdditionalFilters(additionalFilters);
        vcfFilterForm.setGenes(null);
        vcfFilterForm.setVariationTypes(null);
        vcfFilterForm.setInfoFields(Arrays.asList(SVTYPE_FIELD, SVLEN_FIELD));
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm);
        Assert.assertFalse(entryList2.getEntries().isEmpty());
        Assert.assertFalse(entryList2.getEntries().stream().anyMatch(e -> e.getInfo().isEmpty()));

        Set<String> genes = featureIndexManager.searchGenesInVcfFiles(TEST_GENE_PREFIX, Collections.singletonList(
                                                                                                    testVcf.getId()));
        Assert.assertFalse(genes.isEmpty());

        // search by gene name pglyrp4
        vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(testVcf.getId()));
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList(TEST_GENE_NAME)));
        IndexSearchResult<VcfIndexEntry> entries = featureIndexManager.filterVariations(vcfFilterForm);
        Assert.assertFalse(entries.getEntries().isEmpty());

        genes = featureIndexManager.searchGenesInVcfFiles(TEST_GENE_NAME, Collections.singletonList(testVcf.getId()));
        Assert.assertFalse(genes.isEmpty());

        vcfFilterForm.setPageSize(1);
        int totalCount = featureIndexManager.getTotalPagesCount(vcfFilterForm);
        Assert.assertEquals(entries.getEntries().size(), totalCount);

        // search exons
        vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(testVcf.getId()));
        vcfFilterForm.setExon(true);
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm);
        Assert.assertFalse(entryList2.getEntries().isEmpty());
        Assert.assertTrue(entryList2.getEntries().stream().allMatch(VcfIndexEntry::getExon));

        // check duplicates
        vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(testVcf.getId()));
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm);
        checkDuplicates(entryList2.getEntries());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testGeneIndexForFile() throws IOException {
        IndexSearchResult searchResult = featureIndexDao.searchFeatures("", testGeneFile, null);
        Assert.assertTrue(searchResult.getEntries().isEmpty());

        searchResult = featureIndexDao.searchFeatures(TEST_GENE_PREFIX.toLowerCase(), testGeneFile, 10);
        Assert.assertFalse(searchResult.getEntries().isEmpty());
        Assert.assertTrue(searchResult.getEntries().size() <= 10);
        Assert.assertTrue(searchResult.isExceedsLimit());

        // ensfcag00000031547 and ccdc115
        searchResult = featureIndexDao.searchFeatures("ensfcag00000031547", testGeneFile, null);
        Assert.assertEquals(searchResult.getEntries().size(), 1);
        searchResult = featureIndexDao.searchFeatures("ccdc115", testGeneFile, null);
        Assert.assertEquals(searchResult.getEntries().size(), 2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testReindexVcf() throws FeatureIndexException, IOException {
        Resource resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());
        request.setName(UUID.randomUUID().toString());

        VcfFile vcfFile = vcfManager.registerVcfFile(request);

        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(vcfFile.getId()));
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm.setVariationTypes(new VcfFilterForm.FilterSection<>(Arrays.asList(VariationType.MNP,
                                                                                        VariationType.SNV), false));
        IndexSearchResult<VcfIndexEntry> entryList = featureIndexManager.filterVariations(vcfFilterForm);
        Assert.assertFalse(entryList.getEntries().isEmpty());

        fileManager.deleteFileFeatureIndex(vcfFile);

        TestUtils.assertFail(() -> featureIndexManager.filterVariations(vcfFilterForm),
                             Collections.singletonList(IllegalArgumentException.class));

        vcfManager.reindexVcfFile(vcfFile.getId());
        entryList = featureIndexManager.filterVariations(vcfFilterForm);
        Assert.assertFalse(entryList.getEntries().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testReindexGene() throws IOException {
        FeatureIndexedFileRegistrationRequest geneRequest = new FeatureIndexedFileRegistrationRequest();
        Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);
        geneRequest.setReferenceId(referenceId);
        geneRequest.setPath(resource.getFile().getAbsolutePath());
        geneRequest.setName(UUID.randomUUID().toString());

        GeneFile geneFile = gffManager.registerGeneFile(geneRequest);

        IndexSearchResult searchResult = featureIndexDao.searchFeatures(TEST_GENE_PREFIX.toLowerCase(), geneFile, 10);
        Assert.assertFalse(searchResult.getEntries().isEmpty());
        Assert.assertTrue(searchResult.getEntries().size() <= 10);
        Assert.assertTrue(searchResult.isExceedsLimit());

        fileManager.deleteFileFeatureIndex(geneFile);
        TestUtils.assertFail(() -> featureIndexDao.searchFeatures(TEST_GENE_PREFIX.toLowerCase(), geneFile, 10),
                             Collections.singletonList(IllegalArgumentException.class));

        gffManager.reindexGeneFile(geneFile.getId(), false);
        searchResult = featureIndexDao.searchFeatures("ens", geneFile, 10);
        Assert.assertFalse(searchResult.getEntries().isEmpty());
        Assert.assertTrue(searchResult.getEntries().size() <= 10);
        Assert.assertTrue(searchResult.isExceedsLimit());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testNoIndexVcf() throws IOException, FeatureIndexException {
        Resource resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());
        request.setDoIndex(false);
        request.setName(UUID.randomUUID().toString());

        VcfFile vcfFile = vcfManager.registerVcfFile(request);

        Assert.assertNotNull(vcfFile);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME + UUID.randomUUID().toString());
        project.setItems(Collections.singletonList(new ProjectItem(new BiologicalDataItem(
            vcfFile.getBioDataItemId()))));

        projectManager.saveProject(project); // Index is created when vcf file is added

        TestUtils.assertFail(() -> featureIndexManager.filterVariations(new VcfFilterForm(), project.getId()),
                             Collections.singletonList(IllegalArgumentException.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testNoIndexGene() throws IOException {
        FeatureIndexedFileRegistrationRequest geneRequest = new FeatureIndexedFileRegistrationRequest();
        Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);
        geneRequest.setReferenceId(referenceId);
        geneRequest.setPath(resource.getFile().getAbsolutePath());
        geneRequest.setDoIndex(false);
        geneRequest.setName(UUID.randomUUID().toString());

        GeneFile geneFile = gffManager.registerGeneFile(geneRequest);
        Assert.assertNotNull(geneFile);

        TestUtils.assertFail(() -> featureIndexDao.searchFeatures(TEST_GENE_PREFIX.toLowerCase(), geneFile, 10),
                             Collections.singletonList(IllegalArgumentException.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pagingTest() throws IOException, FeatureIndexException {
        VcfFilterForm vcfFilterForm = new VcfFilterForm();

        IndexSearchResult<VcfIndexEntry> entryList = featureIndexManager.filterVariations(vcfFilterForm,
                                                                                          testProject.getId());
        Assert.assertFalse(entryList.getEntries().isEmpty());

        vcfFilterForm.setPageSize(10);
        int total = featureIndexManager.getTotalPagesCount(vcfFilterForm, testProject.getId());

        Set<VcfIndexEntry> pagedEntries = new HashSet<>();
        for (int i = 1; i < total + 1; i++) {
            vcfFilterForm.setPage(i);
            IndexSearchResult<VcfIndexEntry> page = featureIndexManager.filterVariations(vcfFilterForm,
                                                                                         testProject.getId());
            Assert.assertFalse(page.getEntries().isEmpty());
            Assert.assertEquals(total, page.getTotalPagesCount().intValue());

            if (i < (entryList.getEntries().size() / 10) + 1) { // check if only it is not the last page
                                                    // (there should be 4 variations)
                Assert.assertEquals(page.getEntries().size(), 10);
            } else {
                Assert.assertEquals(page.getEntries().size(), 4);
            }

            List<VcfIndexEntry> duplicates = page.getEntries().stream().filter(e -> pagedEntries.contains(e))
                .collect(Collectors.toList());
            Assert.assertTrue(duplicates.isEmpty());
            pagedEntries.addAll(page.getEntries());
        }

        Assert.assertEquals(entryList.getEntries().size(), pagedEntries.size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    public void sortingTest() throws IOException {
        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setPage(1);
        vcfFilterForm.setPageSize(10);

        for (IndexSortField sortField : IndexSortField.values()) {
            vcfFilterForm.setOrderBy(Collections.singletonList(new VcfFilterForm.OrderBy(sortField.name(), false)));

            IndexSearchResult<VcfIndexEntry> entryList = featureIndexManager.filterVariations(vcfFilterForm,
                                                                                              testProject.getId());
            Assert.assertFalse(entryList.getEntries().isEmpty());
            Assert.assertEquals(vcfFilterForm.getPageSize().intValue(), entryList.getEntries().size());
        }

        // check sorting by various fields

        checkSorted(IndexSortField.START_INDEX.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> e.getStartIndex() > p.getStartIndex())),
                    testProject.getId());

        checkSorted(IndexSortField.END_INDEX.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> e.getEndIndex() > p.getEndIndex())),
                    testProject.getId());

        checkSorted(IndexSortField.CHROMOSOME_NAME.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(p -> seenEntries.stream().anyMatch(
                e -> e.getChromosome().getName().compareTo(p.getChromosome().getName()) > 0)),
                    testProject.getId());

        checkSorted(IndexSortField.GENE_NAME.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> StringUtils.isNotBlank(e.getGeneName()) &&
                                                        StringUtils.isNotBlank(p.getGeneName()) &&
                                                        e.getGeneName().compareTo(p.getGeneName()) > 0)),
                    testProject.getId());

        checkSorted(IndexSortField.GENE_NAME.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> StringUtils.isNotBlank(e.getGeneNames()) &&
                                                        StringUtils.isNotBlank(p.getGeneNames()) &&
                                                        e.getGeneNames().compareTo(p.getGeneNames()) > 0)),
                    testProject.getId());

        checkSorted(IndexSortField.GENE_ID.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> StringUtils.isNotBlank(e.getGene()) &&
                                                        StringUtils.isNotBlank(p.getGene()) &&
                                                        e.getGene().compareTo(p.getGene()) > 0)),
                    testProject.getId());

        checkSorted(IndexSortField.GENE_ID.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> StringUtils.isNotBlank(e.getGeneIds()) &&
                                                        StringUtils.isNotBlank(p.getGeneIds()) &&
                                                        e.getGeneIds().compareTo(p.getGeneIds()) > 0)),
                    testProject.getId());

        checkSorted(IndexSortField.VARIATION_TYPE.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> e.getVariationType().name().compareTo(
                                p.getVariationType().name()) > 0)),
                    testProject.getId());

        checkSorted(IndexSortField.FILTER.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> (e.getFailedFilter() != null ? e.getFailedFilter() : "")
                                            .compareTo(p.getFailedFilter() != null ? p.getFailedFilter() : "") > 0)),
                    testProject.getId());

        // check order by additional fields
        VcfFilterInfo info = vcfManager.getFiltersInfo(Collections.singletonList(testVcf.getId()));
        for (InfoItem item : info.getInfoItems()) {
            switch (item.getType()) {
                case Integer:
                    checkSorted(item.getName(), false,
                        (page, seenEntries) -> page.stream().anyMatch(p -> seenEntries.stream().anyMatch(
                            e -> e.getInfo().containsKey(item.getName()) && e.getInfo().get(item.getName()) != null
                                 && e.getInfo().containsKey(item.getName()) && p.getInfo().get(item.getName()) != null
                                 && (e.getInfo().get(item.getName()).toString()).compareTo(
                                     p.getInfo().get(item.getName()).toString()) > 0
                        )),
                        testProject.getId(), Collections.singletonList(item.getName()));
                    break;
                case Float:
                    checkSorted(item.getName(), false,
                        (page, seenEntries) -> page.stream().anyMatch(p -> seenEntries.stream().anyMatch(
                            e -> e.getInfo().containsKey(item.getName()) && e.getInfo().get(item.getName()) != null
                                 && e.getInfo().containsKey(item.getName()) && p.getInfo().get(item.getName()) != null
                                 && (e.getInfo().get(item.getName()).toString()).compareTo(
                                     p.getInfo().get(item.getName()).toString()) > 0
                        )),
                        testProject.getId(), Collections.singletonList(item.getName()));
                    break;
                default:
                    checkSorted(item.getName(), false,
                        (page, seenEntries) -> page.stream().anyMatch(p -> seenEntries.stream().anyMatch(
                            e -> e.getInfo().get(item.getName()) != null &&
                                 p.getInfo().get(item.getName()) != null &&
                                 e.getInfo().get(item.getName()).toString().compareTo(
                                     p.getInfo().get(item.getName()).toString()) > 0)),
                                testProject.getId(), Collections.singletonList(item.getName()));
            }
        }

        // Test sort desc
        checkSorted(IndexSortField.START_INDEX.name(), true,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> e.getStartIndex() < p.getStartIndex())),
            testProject.getId());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testSortByMultipleFields() throws IOException {
        // check sorted by multiple fields

        IndexSearchResult<VcfIndexEntry> referentList = featureIndexManager.filterVariations(new VcfFilterForm(),
                                                                                testProject.getId());
        List<VcfIndexEntry> pagedEntries = new ArrayList<>();

        VcfFilterForm filterForm = new VcfFilterForm();
        filterForm.setPageSize(10);
        filterForm.setOrderBy(Arrays.asList(new VcfFilterForm.OrderBy(IndexSortField.START_INDEX.name(), false),
                                        new VcfFilterForm.OrderBy(IndexSortField.VARIATION_TYPE.name(), false)));

        for (int i = 1; i < (referentList.getEntries().size() / 10) + 2; i++) {
            filterForm.setPage(i);
            IndexSearchResult<VcfIndexEntry> pageRes = featureIndexManager.filterVariations(filterForm,
                                                                                            testProject.getId());
            List<VcfIndexEntry> page = pageRes.getEntries();
            Assert.assertFalse(page.isEmpty());

            if (i < (referentList.getEntries().size() / 10) + 1) { // check if only it is not the last page
                // (there should be 4 variations)
                Assert.assertEquals(page.size(), 10);
            } else {
                Assert.assertEquals(page.size(), 4);
            }

            List<VcfIndexEntry> duplicates = page.stream().filter(pagedEntries::contains).collect(Collectors.toList());
            Assert.assertTrue(duplicates.isEmpty());
            Assert.assertFalse(page.stream().anyMatch(p -> pagedEntries.stream().anyMatch(
                e -> e.getStartIndex() > p.getStartIndex())));
            Assert.assertFalse(page.stream().anyMatch(
                p -> pagedEntries.stream().anyMatch(e -> e.getVariationType().name().compareTo(
                    p.getVariationType().name()) > 0)));
            pagedEntries.addAll(page);
        }
    }

    private void checkSorted(String orderBy, boolean desc, SortTestingPredicate testingPredicate, Long projectId)
        throws IOException {
        checkSorted(orderBy, desc, testingPredicate, projectId, null);
    }

    private void checkSorted(String orderBy, boolean desc, SortTestingPredicate testingPredicate, Long projectId,
                             List<String> additionalFields) throws IOException {
        IndexSearchResult<VcfIndexEntry> referentList = featureIndexManager.filterVariations(new VcfFilterForm(),
                                                                                             projectId);
        List<VcfIndexEntry> pagedEntries = new ArrayList<>();

        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setPageSize(10);
        vcfFilterForm.setOrderBy(Collections.singletonList(new VcfFilterForm.OrderBy(orderBy, desc)));
        vcfFilterForm.setInfoFields(additionalFields);

        for (int i = 1; i < (referentList.getEntries().size() / 10) + 2; i++) {
            vcfFilterForm.setPage(i);
            IndexSearchResult<VcfIndexEntry> pageRes = featureIndexManager.filterVariations(vcfFilterForm, projectId);
            List<VcfIndexEntry> page = pageRes.getEntries();

            Assert.assertFalse(page.isEmpty());

            if (i < (referentList.getEntries().size() / 10) + 1) { // check if only it is not the last page
                // (there should be 4 variations)
                Assert.assertEquals(page.size(), 10);
            } else {
                Assert.assertEquals(page.size(), 4);
            }

            List<VcfIndexEntry> duplicates = page.stream().filter(pagedEntries::contains).collect(Collectors.toList());
            Assert.assertTrue(duplicates.isEmpty());
            Assert.assertFalse(testingPredicate.doTest(page, pagedEntries));
            pagedEntries.addAll(page);
        }
    }

    @FunctionalInterface
    private interface SortTestingPredicate {
        boolean doTest(List<VcfIndexEntry> page, List<VcfIndexEntry> seenEntries);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void groupingTest() throws IOException {
        VcfFilterForm vcfFilterForm = new VcfFilterForm();

        IndexSearchResult<VcfIndexEntry> entryList = featureIndexManager.filterVariations(vcfFilterForm,
                                                                                          testProject.getId());
        Assert.assertFalse(entryList.getEntries().isEmpty());

        List<Group> counts = featureIndexManager.groupVariations(new VcfFilterForm(), testProject.getId(),
                                                                 IndexSortField.CHROMOSOME_NAME.name());
        Assert.assertFalse(counts.isEmpty());

        // test load additional info and group by it
        VcfFilterInfo info = vcfManager.getFiltersInfo(Collections.singletonList(testVcf.getId()));

        vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setInfoFields(info.getInfoItems().stream().map(i -> i.getName()).collect(Collectors.toList()));

        entryList = featureIndexManager.filterVariations(vcfFilterForm, testProject.getId());
        Assert.assertFalse(entryList.getEntries().isEmpty());

        for (InfoItem infoItem : info.getInfoItems()) {
            String groupByField = infoItem.getName();
            List<Group> c = featureIndexManager.groupVariations(new VcfFilterForm(), testProject.getId(),
                                                                                groupByField);
            List<VcfIndexEntry> entriesWithField = entryList.getEntries().stream()
                .filter(e -> e.getInfo().get(groupByField) != null).collect(Collectors.toList());
            if (!entriesWithField.isEmpty()) {
                Assert.assertFalse("Empty grouping for field: " + groupByField, c.isEmpty());
            }
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testGroupingForPlots() throws IOException {
        // chromosomes histogram
        List<Group> counts = featureIndexManager.groupVariations(new VcfFilterForm(), testProject.getId(),
                                                     IndexSortField.CHROMOSOME_NAME.name());
        Assert.assertFalse(counts.isEmpty());

        // variation types histogram

        counts = featureIndexManager.groupVariations(new VcfFilterForm(), testProject.getId(),
                                                     IndexSortField.VARIATION_TYPE.name());
        Assert.assertFalse(counts.isEmpty());

        counts = featureIndexManager.groupVariations(new VcfFilterForm(), testProject.getId(),
                                                     IndexSortField.QUALITY.name());
        Assert.assertFalse(counts.isEmpty());
    }

    @Test
    @Ignore // TODO: remove this test before merging to master
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void performanceTest() throws Exception {
        Reference hg38 = EntityHelper.createG38Reference(referenceGenomeManager.createReferenceId());
        referenceGenomeManager.register(hg38);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(hg38.getId());
        request.setPath("/home/kite/Documents/sampleData/Dream.set3.VarDict.SV.vcf");

        VcfFile vcfFile1 = vcfManager.registerVcfFile(request);

        request.setPath("/home/kite/Documents/sampleData/synthetic.challenge.set3.tumor.20pctmasked.truth.vcf");
        VcfFile vcfFile2 = vcfManager.registerVcfFile(request);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME + 1);
        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(vcfFile1.getBioDataItemId())),
                                       new ProjectItem(new BiologicalDataItem(vcfFile2.getBioDataItemId()))));

        projectManager.saveProject(project);

        IndexSearchResult<VcfIndexEntry> entriesRes = featureIndexManager.filterVariations(new VcfFilterForm(),
                                                                                           project.getId());
        List<VcfIndexEntry> entries = entriesRes.getEntries();
        Assert.assertFalse(entries.isEmpty());
        logger.info("!! Variations count: {}", entries.size());

        TestUtils.warmUp(() -> featureIndexManager.filterVariations(new VcfFilterForm(), project.getId()),
                         PERFORMANCE_TEST_WARMING_COUNT);

        double averageTime = TestUtils.measurePerformance(
            () -> featureIndexManager.filterVariations(new VcfFilterForm(), project.getId()),
            PERFORMANCE_TEST_ATTEMPTS_COUNT);

        logger.info("!! Performing index search took: {} ms", averageTime);

        TestUtils.warmUp(() -> featureIndexManager.filterVariations(new VcfFilterForm(), project.getId()),
                         PERFORMANCE_TEST_WARMING_COUNT);

        averageTime = TestUtils.measurePerformance(
            () -> featureIndexManager.filterVariations(new VcfFilterForm(), project.getId()),
            PERFORMANCE_TEST_ATTEMPTS_COUNT);

        logger.info("!! Performing index search paging took: {} ms", averageTime);

        final VcfFilterForm filterForm = new VcfFilterForm();
        filterForm.setPage(1);
        filterForm.setPageSize(PERFORMANCE_TEST_PAGE_SIZE);
        TestUtils.warmUp(() -> featureIndexManager.filterVariations(filterForm, project.getId()),
                         PERFORMANCE_TEST_WARMING_COUNT);

        averageTime = TestUtils.measurePerformance(
            () -> featureIndexManager.filterVariations(filterForm, project.getId()),
            PERFORMANCE_TEST_ATTEMPTS_COUNT);

        logger.info("!! Performing index search single page took: {} ms", averageTime);

        TestUtils.warmUp(
            () -> ThreadLocalRandom.current().nextInt(1, entries.size() / PERFORMANCE_TEST_PAGE_SIZE + 1),
            (page) -> {
                filterForm.setPage(page);
                featureIndexManager.filterVariations(filterForm, project.getId());
            }, PERFORMANCE_TEST_WARMING_COUNT);

        List<Double> timings = TestUtils.measurePerformanceTimings(
            () -> ThreadLocalRandom.current().nextInt(1, entries.size() / PERFORMANCE_TEST_PAGE_SIZE + 1),
            (page) -> {
                filterForm.setPage(page);
                featureIndexManager.filterVariations(filterForm, project.getId());
            }, PERFORMANCE_TEST_ATTEMPTS_COUNT);

        timings.forEach(t -> logger.info("!! Performed index search random page took: {}", t));
        averageTime = TestUtils.calculateAverage(timings);

        logger.info("!! Average Performing index search random page took: {} ms", averageTime);

        /*TestUtils.warmUp(() -> featureIndexManager.getTotalPagesCount(new VcfFilterForm(), project.getId()),
                         PERFORMANCE_TEST_WARMING_COUNT);
        averageTime = TestUtils.measurePerformance(
            () -> featureIndexManager.getTotalPagesCount(new VcfFilterForm(), project.getId()),
            PERFORMANCE_TEST_ATTEMPTS_COUNT);
        logger.info("!! Performing total facet page count lookup took: {} ms", averageTime);*/
    }

    private void checkDuplicates(List<VcfIndexEntry> entryList) {
        Map<Pair<Integer, Integer>, FeatureIndexEntry> duplicateMap = new HashMap<>();
        entryList.stream().forEach(e -> {
            Pair<Integer, Integer> indexPair = new ImmutablePair<>(e.getStartIndex(), e.getEndIndex());
            Assert.assertFalse(String.format("Found duplicate: %d, %d", e.getStartIndex(), e.getEndIndex()),
                    duplicateMap.containsKey(indexPair));
            duplicateMap.put(indexPair, e);
        });
    }
}
