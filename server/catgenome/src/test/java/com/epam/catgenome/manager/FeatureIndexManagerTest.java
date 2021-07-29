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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.epam.catgenome.controller.vo.ItemsByProject;
import com.epam.catgenome.entity.bed.BedFile;
import com.epam.catgenome.entity.gene.GeneFilterInfo;
import com.epam.catgenome.entity.index.GeneIndexEntry;
import com.epam.catgenome.manager.bed.BedManager;
import com.epam.catgenome.manager.export.ExportManager;
import com.epam.catgenome.manager.export.ExportFormat;
import com.epam.catgenome.manager.export.GeneExportFilterForm;
import com.epam.catgenome.manager.export.VcfExportFilterForm;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
import com.epam.catgenome.dao.index.field.VcfIndexSortField;
import com.epam.catgenome.entity.AbstractFilterForm.OrderBy;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneFilterForm;
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
import com.epam.catgenome.entity.vcf.Pointer;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationQuery;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.entity.vcf.VcfFilterForm.FilterSection;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.project.ProjectManager;
import com.epam.catgenome.manager.reference.BookmarkManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import com.epam.catgenome.util.TestUtils;
import com.epam.catgenome.util.Utils;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    private static final String CLASSPATH_TEMPLATES_GENES_2 = "classpath:templates/genes_sorted_2.gtf";
    private static final String CLASSPATH_TEMPLATES_BED = "classpath:templates/example.bed";

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
    private static final int BED_FEATURE_START = 127471197;
    private static final int BED_FEATURE_END = 127472363;
    private static final int TEST_CHROMOSOME_SIZE = 239107476;
    private static final String TEST_PROJECT_NAME = "testProject1";
    private static final String TEST_GENE_PREFIX = "ENS";
    private static final String TEST_GENE_NAME = "pglyrp4";
    private static final String TEST_GENE_AND_FILE_ID_QUERY = "geneId:ENS* AND fileId:%d";
    private static final String SVTYPE_FIELD = "SVTYPE";
    private static final String SVLEN_FIELD = "SVLEN";
    private static final List<Float> TEST_QUALITY_BOUNDS = Arrays.asList(0.5F, 1.0F);
    private static final long TEST_AMOUNT = 78L;
    private static final long TEST_PAGE_SIZE = 5L;
    private static final long TEST_AMOUNT_OF_MRNA = 10L;
    private static final long TEST_AMOUNT_OF_GENE = 9L;
    private static final long TEST_AMOUNT_POSITION = 25L;
    private static final int TEST_START_INDEX = 65000;
    private static final int TEST_END_INDEX = 200_000;
    private static final int DEFAULT_PAGE_SIZE = 1000;
    public static final int SMALL_PAGE_SIZE = 8;
    public static final int FULL_GENE_FILE_SIZE = 144;
    public static final float GOOD_SCORE_FROM = -2.f;
    public static final int GOOD_FRAME = -1;
    public static final int ONE = 1;
    public static final int ZERO = 0;
    public static final float WRONG_SCORE_FROM = 2.f;

    private Logger logger = LoggerFactory.getLogger(FeatureIndexManagerTest.class);

    @Autowired
    private VcfManager vcfManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private GffManager gffManager;

    @Autowired
    private BedManager bedManager;

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

    @Autowired
    private ExportManager exportManager;

    private long referenceId;
    private Reference testReference;
    private Chromosome testChromosome;
    private VcfFile testVcf;
    private GeneFile testGeneFile;
    private GeneFile testGeneFile2;
    private BedFile testBedFile;
    private Project testProject;

    @Before
    public void setup() throws Exception {
        testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        testReference = EntityHelper.createNewReference(testChromosome, referenceGenomeManager.createReferenceId());

        referenceGenomeManager.create(testReference);
        referenceId = testReference.getId();

        Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());
        testGeneFile = gffManager.registerGeneFile(request);
        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), testGeneFile.getId());

        resource = context.getResource(CLASSPATH_TEMPLATES_GENES_2);
        request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());
        testGeneFile2 = gffManager.registerGeneFile(request);

        Resource bedResource = context.getResource(CLASSPATH_TEMPLATES_BED);
        FeatureIndexedFileRegistrationRequest bedFileRequest = new FeatureIndexedFileRegistrationRequest();
        bedFileRequest.setReferenceId(referenceId);
        bedFileRequest.setPath(bedResource.getFile().getAbsolutePath());

        testBedFile = bedManager.registerBed(bedFileRequest);
        // TODO Ask about indexing bed with registration
        bedManager.reindexBedFile(testBedFile.getId());

        resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        testVcf = vcfManager.registerVcfFile(request);

        testProject = new Project();
        testProject.setName(TEST_PROJECT_NAME);
        testProject.setItems(
                Arrays.asList(new ProjectItem(new BiologicalDataItem(testVcf.getBioDataItemId())),
                        new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));

        projectManager.create(testProject); // Index is created when vcf file is added
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testCreateFeatureIndex() throws Exception {
        assertFalse(featureIndexDao.searchFileIndexes(
                Collections.singletonList(testVcf), String.format(TEST_GENE_AND_FILE_ID_QUERY, testVcf.getId()), null)
                .getEntries().isEmpty());

        final VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIdsByProject(Collections.singletonMap(testProject.getId(),
                Collections.singletonList(testVcf.getId())));
        vcfFilterForm.setGenes(new FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm.setVariationTypes(new FilterSection<>(Arrays.asList(VariationType.MNP, VariationType.SNV),
                false));
        vcfFilterForm.setQuality(TEST_QUALITY_BOUNDS);

        final IndexSearchResult<VcfIndexEntry> entryList = featureIndexManager.filterVariations(vcfFilterForm,
                testProject.getId());
        assertFalse(entryList.getEntries().isEmpty());
        assertTrue(entryList.getEntries().stream().anyMatch(e -> e.getInfo() != null && (Boolean) e.getInfo()
                .get(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName())));

        vcfFilterForm.setChromosomeIds(Collections.singletonList(testChromosome.getId()));

        assertFalse(featureIndexManager.filterVariations(vcfFilterForm, testProject.getId()).getEntries().isEmpty());

        final double time1 = Utils.getSystemTimeMilliseconds();
        final List<Long> chromosomeIds = featureIndexDao.getChromosomeIdsWhereVariationsPresentFacet(
                Collections.singletonList(testVcf),
                String.format("geneId:ENS* AND fileId:%d AND variationType:snv", testVcf.getId()));
        final double time2 = Utils.getSystemTimeMilliseconds();
        logger.info("Get chromosomes by facets time: {} ms", time2 - time1);

        assertFalse(chromosomeIds.isEmpty());

        final List<Chromosome> chromosomes = featureIndexManager.filterChromosomes(vcfFilterForm, testProject.getId());
        assertFalse(chromosomes.isEmpty());

        // filter by additional fields
        final Map<String, Object> additionalFilters = new HashMap<>();
        additionalFilters.put(SVTYPE_FIELD, "DEL");
        //additionalFilters.put("SVLEN", SVLEN_VALUE);
        additionalFilters.put(SVLEN_FIELD, String.valueOf(SVLEN_VALUE));
        vcfFilterForm.setAdditionalFilters(additionalFilters);
        vcfFilterForm.setGenes(null);
        vcfFilterForm.setVariationTypes(null);
        vcfFilterForm.setInfoFields(Arrays.asList(SVTYPE_FIELD, SVLEN_FIELD));

        final IndexSearchResult<VcfIndexEntry> entryList2 =
                featureIndexManager.filterVariations(vcfFilterForm, testProject.getId());
        assertFalse(entryList2.getEntries().isEmpty());
        assertFalse(entryList2.getEntries().stream().anyMatch(e -> e.getInfo().isEmpty()));

        assertFalse(featureIndexManager.searchGenesInVcfFilesInProject(
                testProject.getId(), TEST_GENE_PREFIX, Collections.singletonList(testVcf.getId())).isEmpty());

        // search by gene name pglyrp4
        VcfFilterForm vcfFilterForm2 = new VcfFilterForm();
        vcfFilterForm2.setGenes(new FilterSection<>(Collections.singletonList(TEST_GENE_NAME)));
        final IndexSearchResult<VcfIndexEntry> entries = featureIndexManager.filterVariations(vcfFilterForm2,
                testProject.getId());
        assertFalse(entries.getEntries().isEmpty());

        final Set<String> genes = featureIndexManager.searchGenesInVcfFilesInProject(
                testProject.getId(), TEST_GENE_NAME, Collections.singletonList(testVcf.getId()));
        assertFalse(genes.isEmpty());

        vcfFilterForm2.setPageSize(ONE);
        int totalCount = featureIndexManager.getTotalPagesCount(vcfFilterForm2, testProject.getId());
        assertEquals(entries.getEntries().size(), totalCount);

        // search exons
        final VcfFilterForm vcfFilterForm3 = new VcfFilterForm();
        vcfFilterForm3.setExon(true);
        final IndexSearchResult<VcfIndexEntry> entryList3 =
                featureIndexManager.filterVariations(vcfFilterForm3, testProject.getId());
        assertFalse(entryList3.getEntries().isEmpty());
        assertTrue(entryList3.getEntries().stream().allMatch(e -> e.getInfo() != null && (Boolean) e.getInfo()
                .get(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName())));

        // check duplicates
        final IndexSearchResult<VcfIndexEntry> entryList4 =
                featureIndexManager.filterVariations(new VcfFilterForm(), testProject.getId());
        checkDuplicates(entryList4.getEntries());

        // test filter by position
        final VcfIndexEntry e = entryList4.getEntries().get(ZERO);

        final VcfFilterForm filterForm = new VcfFilterForm();
        filterForm.setStartIndex(e.getStartIndex());
        filterForm.setEndIndex(e.getEndIndex());
        final IndexSearchResult<VcfIndexEntry> entryList5 =
                featureIndexManager.filterVariations(filterForm, testProject.getId());

        assertFalse(entryList5.getEntries().isEmpty());
        assertTrue(entryList5.getEntries().stream().allMatch(v -> v.getStartIndex() >= filterForm
                .getStartIndex() && v.getEndIndex() <= filterForm.getEndIndex()));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadAllFields() throws IOException {
        final VcfFilterForm filterForm = new VcfFilterForm();
        final VcfFilterInfo info = vcfManager.getFiltersInfo(Collections.singletonList(testVcf.getId()));
        filterForm.setInfoFields(info.getInfoItems().stream()
                .map(InfoItem::getName)
                .collect(toList()));

        final IndexSearchResult<VcfIndexEntry> entries = featureIndexManager.filterVariations(filterForm,
                                                                                        testProject.getId());
        assertFalse(entries.getEntries().isEmpty());
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

        referenceGenomeManager.create(testHumanReference);
        Long humanReferenceId = testHumanReference.getId();
        referenceGenomeManager.updateReferenceGeneFileId(testHumanReference.getId(), testGeneFile.getId());

        Resource resource = context.getResource("classpath:templates/sample_2-lumpy.vcf");
        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(humanReferenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        VcfFile vcfFile = vcfManager.registerVcfFile(request);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME + "1");
        project.setItems(Arrays.asList(new ProjectItem(
                new BiologicalDataItem(testHumanReference.getBioDataItemId())),
                new ProjectItem(new BiologicalDataItem(vcfFile.getBioDataItemId()))));

        projectManager.create(project); // Index is created when vcf file is added
        VcfFilterInfo info = featureIndexManager.loadVcfFilterInfoForProject(project.getId());

        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIdsByProject(Collections.singletonMap(project.getId(),
                Collections.singletonList(vcfFile.getId())));
        vcfFilterForm.setVariationTypes(new FilterSection<>(Arrays.asList(VariationType.DEL,
                VariationType.SNV), false));

        String cipos95 = "CIPOS95";
        vcfFilterForm.setInfoFields(info.getInfoItems().stream().map(InfoItem::getName).collect(toList()));
        vcfFilterForm.setAdditionalFilters(Collections.singletonMap(cipos95, Arrays.asList(CONST_42, CONST_42)));
        vcfFilterForm.setVcfFileIdsByProject(Collections.singletonMap(project.getId(),
                Collections.singletonList(vcfFile.getId())));

        IndexSearchResult<VcfIndexEntry> entryList2 = featureIndexManager.filterVariations(vcfFilterForm);
        assertFalse(entryList2.getEntries().isEmpty());
        assertTrue(entryList2.getEntries().stream().anyMatch(e -> e.getInfo().containsKey(cipos95)));
        assertTrue(entryList2.getEntries().stream().filter(e -> e.getInfo().containsKey(cipos95)).allMatch(e -> {
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
            assertNotNull(variation);

//            for (Map.Entry<String, Variation.InfoField> i : variation.getInfo().entrySet()) {
//                if (i.getValue().getValue() != null) {
//                    Assert.assertTrue(String.format("%s expected, %s found", i.getValue().getValue(),
//                            e.getInfo().get(i.getKey())),
//                            i.getValue().getValue().toString().equalsIgnoreCase(
//                                    e.getInfo().get(i.getKey()).toString()));
//                } else {
//                    Assert.assertEquals(i.getValue().getValue(), e.getInfo().get(i.getKey()));
//                }
//            }
        }

        // flrt2

        vcfFilterForm.setVcfFileIdsByProject(Collections.singletonMap(project.getId(),
                Collections.singletonList(vcfFile.getId())));
        IndexSearchResult<VcfIndexEntry> entryList21 = featureIndexManager.filterVariations(vcfFilterForm);

        assertFalse(entryList21.getEntries().isEmpty());
        assertEquals(entryList21.getEntries().size(), entryList2.getEntries().size());
        assertEquals(entryList21.getEntries().get(ZERO).getGene(), entryList2.getEntries().get(ZERO).getGene());

        // empty filter test
        VcfFilterForm emptyForm = new VcfFilterForm();
        emptyForm.setVcfFileIdsByProject(Collections.singletonMap(project.getId(),
                Collections.singletonList(vcfFile.getId())));
        entryList2 = featureIndexManager.filterVariations(emptyForm);
        assertFalse(entryList2.getEntries().isEmpty());
        checkDuplicates(entryList2.getEntries());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testIndexUpdateOnProjectOperations() throws Exception {
        final Resource gffResource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);

        final FeatureIndexedFileRegistrationRequest request1 = new FeatureIndexedFileRegistrationRequest();
        request1.setReferenceId(referenceId);
        request1.setPath(gffResource.getFile().getAbsolutePath());
        request1.setName("testGeneFile");

        final GeneFile geneFile = gffManager.registerGeneFile(request1);
        assertNotNull(geneFile);
        assertNotNull(geneFile.getId());

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), geneFile.getId());

        final Resource vcfResource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        final FeatureIndexedFileRegistrationRequest request2 = new FeatureIndexedFileRegistrationRequest();
        request2.setReferenceId(referenceId);
        request2.setPath(vcfResource.getFile().getAbsolutePath());
        request2.setName("testVcf");

        final VcfFile vcfFile = vcfManager.registerVcfFile(request2);

        final Project project1 = new Project();
        project1.setName(TEST_PROJECT_NAME + ONE);
        project1.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(vcfFile.getBioDataItemId())),
                new ProjectItem(new BiologicalDataItem(geneFile.getBioDataItemId())),
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));

        projectManager.create(project1); // Index is created when vcf file is added

        final VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIdsByProject(
                Collections.singletonMap(project1.getId(), Collections.singletonList(vcfFile.getId())));
        vcfFilterForm.setChromosomeIds(Collections.singletonList(testChromosome.getId()));
        vcfFilterForm.setGenes(new FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm.setVariationTypes(new FilterSection<>(Arrays.asList(VariationType.MNP, VariationType.SNV),
                false));

        final IndexSearchResult<VcfIndexEntry> entryList1 = featureIndexManager.filterVariations(vcfFilterForm,
                                                                                          project1.getId());
        assertFalse(entryList1.getEntries().isEmpty());

        // try to add an vcf item
        final FeatureIndexedFileRegistrationRequest request3 = new FeatureIndexedFileRegistrationRequest();
        request3.setReferenceId(referenceId);
        request3.setPath(vcfResource.getFile().getAbsolutePath());
        request3.setName(vcfResource.getFilename() + "2");
        final VcfFile vcfFile2 = vcfManager.registerVcfFile(request3);

        final Project project2 = projectManager.addProjectItem(project1.getId(), vcfFile2.getBioDataItemId());

        final IndexSearchResult<VcfIndexEntry> entryList2 =
                featureIndexManager.filterVariations(vcfFilterForm, project2.getId());
        assertFalse(entryList2.getEntries().isEmpty());
        assertTrue(entryList2.getEntries().stream().allMatch(e -> e.getFeatureFileId().equals(vcfFile.getId())));


        final VcfFilterForm vcfFilterForm2 = new VcfFilterForm();
        vcfFilterForm2.setVcfFileIdsByProject(
                Collections.singletonMap(project2.getId(), Collections.singletonList(vcfFile2.getId())));
        vcfFilterForm2.setChromosomeIds(Collections.singletonList(testChromosome.getId()));
        vcfFilterForm2.setGenes(new FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm2.setVariationTypes(new FilterSection<>(Arrays.asList(VariationType.MNP, VariationType.SNV),
                false));

        final IndexSearchResult<VcfIndexEntry> entryList3 = featureIndexManager.filterVariations(vcfFilterForm2,
                                                                                           project2.getId());
        assertFalse(entryList3.getEntries().isEmpty());
        assertEquals(entryList2.getEntries().size(), entryList3.getEntries().size());
        assertTrue(entryList3.getEntries().stream()
                .allMatch(e -> e.getFeatureFileId().equals(vcfFile2.getId())));

        // test no vcfFileIds
        vcfFilterForm2.setVcfFileIdsByProject(null);
        final IndexSearchResult<VcfIndexEntry> entryList4 =
                featureIndexManager.filterVariations(vcfFilterForm2, project2.getId());
        assertFalse(entryList4.getEntries().isEmpty());
        assertEquals(entryList4.getEntries().size(), entryList2.getEntries().size() * 2);

        // test with multiple vcfFileIds
        vcfFilterForm2.setVcfFileIdsByProject(
                Collections.singletonMap(project2.getId(), Arrays.asList(vcfFile.getId(), vcfFile2.getId())));
        final IndexSearchResult<VcfIndexEntry> entryList5 =
                featureIndexManager.filterVariations(vcfFilterForm2, project2.getId());
        assertFalse(entryList5.getEntries().isEmpty());
        assertEquals(entryList5.getEntries().size(), entryList2.getEntries().size() * 2);

        // try to remove a vcf item by save - should be not indexed
        project2.setItems(project2.getItems().stream()
                .filter(i -> !(i.getBioDataItem() instanceof VcfFile) || !((VcfFile) i.getBioDataItem()).getId().equals(
                        vcfFile2.getId()))
                .collect(toList()));

        final Project project3 = projectManager.create(project2);

        vcfFilterForm2.setVcfFileIdsByProject(Collections.singletonMap(project3.getId(),
                Collections.singletonList(vcfFile2.getId())));
        final IndexSearchResult<VcfIndexEntry> entryList6 =
                featureIndexManager.filterVariations(vcfFilterForm2, project3.getId());
        assertTrue(entryList6.getEntries().isEmpty());

        // try to remove gene file
        project3.setItems(project3.getItems().stream()
                .filter(i -> !(i.getBioDataItem() instanceof GeneFile))
                .collect(toList()));

        final Project project4 = projectManager.create(project3);

        vcfFilterForm.setGenes(null);
        final IndexSearchResult<VcfIndexEntry> entryList7 =
                featureIndexManager.filterVariations(vcfFilterForm, project4.getId());
        assertFalse(entryList7.getEntries().isEmpty());

        // add multiple files
        project4.getItems().clear();
        projectManager.create(project4);
        final Project loadedProject = projectManager.load(project4.getId());
        assertTrue(loadedProject.getItems().isEmpty());
        final IndexSearchResult<VcfIndexEntry> entryList8 =
                featureIndexManager.filterVariations(new VcfFilterForm(), project4.getId());
        assertTrue(entryList8.getEntries().isEmpty());

        project4.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(vcfFile.getBioDataItemId())),
                new ProjectItem(new BiologicalDataItem(vcfFile2.getBioDataItemId())),
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        projectManager.create(project4);

        final IndexSearchResult<VcfIndexEntry> entryList9 =
                featureIndexManager.filterVariations(new VcfFilterForm(), project4.getId());
        assertTrue(entryList9.getEntries().stream()
                .anyMatch(e -> e.getFeatureFileId().equals(vcfFile.getId())));
        assertTrue(entryList9.getEntries().stream()
                .anyMatch(e -> e.getFeatureFileId().equals(vcfFile2.getId())));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadVcfFilterInfoForProject() throws IOException {
        final VcfFilterInfo filterInfo = featureIndexManager.loadVcfFilterInfoForProject(testProject.getId());
        assertFalse(filterInfo.getAvailableFilters().isEmpty());
        assertFalse(filterInfo.getInfoItems().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testCreateGeneIndex() throws IOException {
        final IndexSearchResult<FeatureIndexEntry> searchResult1 =
                featureIndexManager.searchFeaturesInProject("", testProject.getId());
        assertTrue(searchResult1.getEntries().isEmpty());

        final IndexSearchResult<FeatureIndexEntry> searchResult2 =
                featureIndexManager.searchFeaturesInProject("ens", testProject.getId());
        assertFalse(searchResult2.getEntries().isEmpty());
        assertTrue(searchResult2.getEntries().size() <= 10);
        assertTrue(searchResult2.isExceedsLimit());

        // ensfcag00000031547 and ccdc115
        final IndexSearchResult<FeatureIndexEntry> searchResult3 =
                featureIndexManager.searchFeaturesInProject("ensfcag00000031547", testProject.getId());
        assertEquals(searchResult3.getEntries().size(), ONE);

        final IndexSearchResult<FeatureIndexEntry> searchResult4 =
                featureIndexManager.searchFeaturesInProject("ccdc115", testProject.getId());
        assertEquals(searchResult4.getEntries().size(), 2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testIntervalQuery() throws IOException {
        final Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);

        final FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setName("GENES_SORTED_INT");
        request.setPath(resource.getFile().getAbsolutePath());

        final GeneFile geneFile = gffManager.registerGeneFile(request);
        assertNotNull(geneFile);
        assertNotNull(geneFile.getId());

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), geneFile.getId());

        final Project project = new Project();
        project.setName(TEST_PROJECT_NAME + "_INT");

        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId())),
                new ProjectItem(new BiologicalDataItem(geneFile.getBioDataItemId()))));
        projectManager.create(project);

        final IndexSearchResult<FeatureIndexEntry> result1 = featureIndexDao.searchFeaturesInInterval(
                Collections.singletonList(geneFile), INTERVAL1_START, INTERVAL1_END, testChromosome);

        assertEquals(3, result1.getEntries().size());

        final IndexSearchResult<FeatureIndexEntry> result2 = featureIndexDao.searchFeaturesInInterval(
                Collections.singletonList(geneFile), INTERVAL2_START, INTERVAL2_END, testChromosome);
        assertEquals(ZERO, result2.getEntries().size());

        final IndexSearchResult<FeatureIndexEntry> result3 = featureIndexDao.searchFeaturesInInterval(
                Collections.singletonList(geneFile), INTERVAL3_START, INTERVAL3_END, testChromosome);
        assertEquals(3, result3.getEntries().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testCreateUnmappedGeneIndex() throws IOException {
        final Chromosome chr1 = EntityHelper.createNewChromosome("chr1");
        chr1.setSize(TEST_CHROMOSOME_SIZE);
        final Reference testHumanReference = EntityHelper.createNewReference(chr1,
                referenceGenomeManager.createReferenceId());

        referenceGenomeManager.create(testHumanReference);
        final Long humanReferenceId = testHumanReference.getId();

        final Resource resource = context.getResource("classpath:templates/mrna.sorted.chunk.gtf");

        final FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(humanReferenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        final GeneFile geneFile = gffManager.registerGeneFile(request);
        assertNotNull(geneFile);
        assertNotNull(geneFile.getId());

        final Project project = new Project();
        project.setName(TEST_PROJECT_NAME + ONE);

        project.setItems(Arrays.asList(
                new ProjectItem(new BiologicalDataItem(geneFile.getBioDataItemId())),
                new ProjectItem(new BiologicalDataItem(testHumanReference.getBioDataItemId()))));
        projectManager.create(project);

        final List<FeatureIndexEntry> entryList1 =
                featureIndexManager.searchFeaturesInProject("", project.getId()).getEntries();
        assertTrue(entryList1.isEmpty());

        final List<FeatureIndexEntry> entryList2 =
                featureIndexManager.searchFeaturesInProject("AM992871", project.getId()).getEntries();
        assertTrue(entryList2.isEmpty()); // we don't search for exons
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testBookmarkSearch() throws IOException {
        final Bookmark bookmark = new Bookmark();
        bookmark.setChromosome(testChromosome);
        bookmark.setStartIndex(ONE);
        bookmark.setEndIndex(testChromosome.getSize());
        bookmark.setName("testBookmark");

        bookmarkManager.create(bookmark);
        final Bookmark loadedBookmark = bookmarkManager.load(bookmark.getId());
        assertNotNull(loadedBookmark);

        final IndexSearchResult<FeatureIndexEntry> result = featureIndexManager.searchFeaturesInProject(
                bookmark.getName(), testProject.getId());
        assertFalse(result.getEntries().isEmpty());
        assertEquals(result.getEntries().get(ZERO).getFeatureType(), FeatureType.BOOKMARK);
        assertNotNull(((BookmarkIndexEntry) result.getEntries().get(ZERO)).getBookmark());
        assertEquals(result.getEntries().size(), result.getTotalResultsCount());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testWickedVcfIndex() throws IOException {
        final Chromosome chr1 = EntityHelper.createNewChromosome("chr21");
        chr1.setSize(TEST_WICKED_VCF_LENGTH);
        final Chromosome chr2 = EntityHelper.createNewChromosome("chr22");
        chr2.setSize(TEST_WICKED_VCF_LENGTH);
        final Reference testHumanReference = EntityHelper.createNewReference(Arrays.asList(chr1, chr2),
                referenceGenomeManager.createReferenceId());
        referenceGenomeManager.create(testHumanReference);
        final Long humanReferenceId = testHumanReference.getId();

        final Project project = new Project();
        project.setName(TEST_PROJECT_NAME + ONE);
        project.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testHumanReference.getBioDataItemId()))));

        projectManager.create(project);

        final Resource resource1 =
                context.getResource("classpath:templates/Homo_sapiens.GRCh38.83.sorted.chr21-22.gtf");
        final FeatureIndexedFileRegistrationRequest geneRequest = new FeatureIndexedFileRegistrationRequest();
        geneRequest.setPath(resource1.getFile().getAbsolutePath());
        geneRequest.setReferenceId(humanReferenceId);

        final GeneFile geneFile = gffManager.registerGeneFile(geneRequest);
        referenceGenomeManager.updateReferenceGeneFileId(humanReferenceId, geneFile.getId());

        final Resource resource2 = context.getResource("classpath:templates/Dream.set3.VarDict.SV.vcf");

        final FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(humanReferenceId);
        request.setPath(resource2.getFile().getAbsolutePath());

        final VcfFile vcfFile = vcfManager.registerVcfFile(request);
        assertNotNull(vcfFile);
        assertNotNull(vcfFile.getId());

        project.setItems(Arrays.asList(new ProjectItem(geneFile), new ProjectItem(vcfFile)));

        projectManager.create(project);

        final IndexSearchResult<VcfIndexEntry> entries = featureIndexManager.filterVariations(new VcfFilterForm(),
                                                                                        project.getId());
        assertFalse(entries.getEntries().isEmpty());

        final long varGenesCount = entries.getEntries().stream()
                .filter(e -> StringUtils.isNotBlank(e.getGene()))
                .count();
        assertTrue(varGenesCount > ZERO);
        /*entries.stream().filter(e -> StringUtils.isNotBlank(e.getGene())).forEach(e -> logger.info("{} - {}, {}", e
                .getStartIndex(), e.getEndIndex(), e.getGeneIds()));*/

        // check chromosome filter
        int chr21EntrySize = checkChromosomeFilterAndGetEntrySize(project, chr1.getId());
        int chr22EntrySize = checkChromosomeFilterAndGetEntrySize(project, chr2.getId());
        int chr2122EntrySize = checkChromosomeFilterAndGetEntrySize(project, chr1.getId(), chr2.getId());

        assertEquals(chr21EntrySize + chr22EntrySize, chr2122EntrySize);
    }

    private int checkChromosomeFilterAndGetEntrySize(final Project project, final Long... chrIds) throws IOException {
        final VcfFilterForm chrForm = new VcfFilterForm();
        chrForm.setChromosomeIds(Arrays.asList(chrIds));
        final IndexSearchResult<VcfIndexEntry> chrEntries =
                featureIndexManager.filterVariations(chrForm, project.getId());

        assertFalse(chrEntries.getEntries().isEmpty());

        for (final Long chrId : chrIds) {
            if (chrIds.length > ONE) {
                assertTrue(chrEntries.getEntries().stream().anyMatch(e -> e.getChromosome().getId().equals(chrId)));
            } else {
                assertTrue(chrEntries.getEntries().stream().allMatch(e -> e.getChromosome().getId().equals(chrId)));
            }
        }

        return chrEntries.getEntries().size();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testSearchIndexForFile() throws IOException, FeatureIndexException {
        final List<FeatureIndexEntry> entryList = featureIndexDao.searchFileIndexes(Collections.singletonList(testVcf),
                String.format(TEST_GENE_AND_FILE_ID_QUERY, testVcf.getId()), null).getEntries();
        assertFalse(entryList.isEmpty());

        final VcfFilterForm vcfFilterForm1 = getSimpleVcfFilter();
        vcfFilterForm1.setGenes(new FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm1.setVariationTypes(new FilterSection<>(Arrays.asList(VariationType.MNP, VariationType.SNV),
                false));

        //vcfFilterForm.setQuality(Collections.singletonList(QUAL_VALUE));
        final IndexSearchResult<VcfIndexEntry> result1 = featureIndexManager.filterVariations(vcfFilterForm1);
        assertFalse(result1.getEntries().isEmpty());
        assertTrue(result1.getEntries().stream().anyMatch(e -> e.getInfo() != null && (Boolean) e.getInfo()
                .get(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName())));

        vcfFilterForm1.setChromosomeIds(Collections.singletonList(testChromosome.getId()));
        final IndexSearchResult<VcfIndexEntry> result2 = featureIndexManager.filterVariations(vcfFilterForm1);
        assertFalse(result2.getEntries().isEmpty());

        final double time1 = Utils.getSystemTimeMilliseconds();
        final List<Long> chromosomeIds = featureIndexDao.getChromosomeIdsWhereVariationsPresentFacet(
                Collections.singletonList(testVcf),
                String.format("geneId:ENS* AND fileId:%d AND variationType:snv", testVcf.getId()));
        final double time2 = Utils.getSystemTimeMilliseconds();
        logger.info("Get chromosomes by facets time: {} ms", time2 - time1);

        assertFalse(chromosomeIds.isEmpty());

        final List<Chromosome> chromosomes = featureIndexManager.filterChromosomes(vcfFilterForm1);
        assertFalse(chromosomes.isEmpty());

        // filter by additional fields
        final Map<String, Object> additionalFilters = new HashMap<>();
        additionalFilters.put(SVTYPE_FIELD, "del");
        additionalFilters.put(SVLEN_FIELD, String.valueOf(SVLEN_VALUE));
        vcfFilterForm1.setAdditionalFilters(additionalFilters);
        vcfFilterForm1.setGenes(null);
        vcfFilterForm1.setVariationTypes(null);
        vcfFilterForm1.setInfoFields(Arrays.asList(SVTYPE_FIELD, SVLEN_FIELD));

        final IndexSearchResult<VcfIndexEntry> result3 = featureIndexManager.filterVariations(vcfFilterForm1);
        assertFalse(result3.getEntries().isEmpty());
        assertFalse(result3.getEntries().stream().anyMatch(e -> e.getInfo().isEmpty()));

        final Set<String> genes1 = featureIndexManager.searchGenesInVcfFiles(
                TEST_GENE_PREFIX, Collections.singletonList(testVcf.getId()));
        assertFalse(genes1.isEmpty());

        // search by gene name pglyrp4
        final VcfFilterForm vcfFilterForm2 = getSimpleVcfFilter();
        vcfFilterForm2.setGenes(new FilterSection<>(Collections.singletonList(TEST_GENE_NAME)));
        final IndexSearchResult<VcfIndexEntry> entries = featureIndexManager.filterVariations(vcfFilterForm2);
        assertFalse(entries.getEntries().isEmpty());

        final Set<String> genes2 = featureIndexManager.searchGenesInVcfFiles(
                TEST_GENE_NAME, Collections.singletonList(testVcf.getId()));
        assertFalse(genes2.isEmpty());

        vcfFilterForm2.setPageSize(ONE);
        final int totalCount = featureIndexManager.getTotalPagesCount(vcfFilterForm2);
        assertEquals(entries.getEntries().size(), totalCount);

        // search exons
        final VcfFilterForm vcfFilterForm3 = getSimpleVcfFilter();
        vcfFilterForm3.setExon(true);

        final IndexSearchResult<VcfIndexEntry> result4 = featureIndexManager.filterVariations(vcfFilterForm3);
        assertFalse(result4.getEntries().isEmpty());
        assertTrue(result4.getEntries().stream().allMatch(e -> e.getInfo() != null && (Boolean) e.getInfo()
                .get(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName())));

        // check duplicates
        final VcfFilterForm vcfFilterForm4 = getSimpleVcfFilter();

        final IndexSearchResult<VcfIndexEntry> result5 = featureIndexManager.filterVariations(vcfFilterForm4);
        checkDuplicates(result5.getEntries());
    }

    private VcfFilterForm getSimpleVcfFilter() {
        final VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIdsByProject(Collections.singletonMap(testProject.getId(),
                Collections.singletonList(testVcf.getId())));
        return vcfFilterForm;
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testGeneIndexForFile() throws IOException {
        final IndexSearchResult<FeatureIndexEntry> searchResult1 =
                featureIndexDao.searchFeatures("", testGeneFile, null);
        assertTrue(searchResult1.getEntries().isEmpty());

        final IndexSearchResult<FeatureIndexEntry> searchResult2 =
                featureIndexDao.searchFeatures(TEST_GENE_PREFIX.toLowerCase(), testGeneFile, 10);
        assertFalse(searchResult2.getEntries().isEmpty());
        assertTrue(searchResult2.getEntries().size() <= 10);
        assertTrue(searchResult2.isExceedsLimit());

        // ensfcag00000031547 and ccdc115
        final IndexSearchResult<FeatureIndexEntry> searchResult3 =
                featureIndexDao.searchFeatures("ensfcag00000031547", testGeneFile, null);
        assertEquals(searchResult3.getEntries().size(), ONE);

        final IndexSearchResult<FeatureIndexEntry> searchResult4 =
                featureIndexDao.searchFeatures("ccdc115", testGeneFile, null);
        assertEquals(searchResult4.getEntries().size(), 2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testBedIndexForFile() throws IOException {
        final IndexSearchResult<FeatureIndexEntry> searchResult1 =
                featureIndexDao.searchFeatures("", testBedFile, null);
        assertTrue(searchResult1.getEntries().isEmpty());

        final IndexSearchResult<FeatureIndexEntry> searchResult2 =
                featureIndexDao.searchFeatures("Pos1", testBedFile, null);
        final List<FeatureIndexEntry> entries = searchResult2.getEntries();
        assertEquals(ONE, entries.size());
        assertEquals("pos1", entries.get(ZERO).getFeatureName());
        assertEquals("A1", entries.get(ZERO).getChromosome().getName());
        // The BED format uses a first-base-is-zero convention,  Tribble features use 1 => add 1.
        assertEquals(BED_FEATURE_START, (int) entries.get(ZERO).getStartIndex());
        assertEquals(BED_FEATURE_END, (int) entries.get(ZERO).getEndIndex());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testReindexVcf() throws FeatureIndexException, IOException {
        final Resource resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        final FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());
        request.setName(UUID.randomUUID().toString());

        final VcfFile vcfFile = vcfManager.registerVcfFile(request);

        final VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIdsByProject(Collections.singletonMap(testProject.getId(),
                Collections.singletonList(vcfFile.getId())));
        vcfFilterForm.setGenes(new FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm.setVariationTypes(new FilterSection<>(Arrays.asList(VariationType.MNP, VariationType.SNV),
                false));
        final IndexSearchResult<VcfIndexEntry> entryList1 = featureIndexManager.filterVariations(vcfFilterForm);
        assertFalse(entryList1.getEntries().isEmpty());

        fileManager.deleteFileFeatureIndex(vcfFile);

        assertTrue(featureIndexManager.filterVariations(vcfFilterForm).getEntries().isEmpty());

        vcfManager.reindexVcfFile(vcfFile.getId(), false);
        final IndexSearchResult<VcfIndexEntry> entryList2 = featureIndexManager.filterVariations(vcfFilterForm);
        assertFalse(entryList2.getEntries().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testReindexGene() throws IOException {
        final FeatureIndexedFileRegistrationRequest geneRequest = new FeatureIndexedFileRegistrationRequest();
        final Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);
        geneRequest.setReferenceId(referenceId);
        geneRequest.setPath(resource.getFile().getAbsolutePath());
        geneRequest.setName(UUID.randomUUID().toString());

        final GeneFile geneFile = gffManager.registerGeneFile(geneRequest);

        final IndexSearchResult<FeatureIndexEntry> searchResult1 =
                featureIndexDao.searchFeatures(TEST_GENE_PREFIX.toLowerCase(), geneFile, 10);
        assertFalse(searchResult1.getEntries().isEmpty());
        assertTrue(searchResult1.getEntries().size() <= 10);
        assertTrue(searchResult1.isExceedsLimit());

        fileManager.deleteFileFeatureIndex(geneFile);
        assertTrue(featureIndexDao.searchFeatures(TEST_GENE_PREFIX.toLowerCase(), geneFile, 10)
                .getEntries().isEmpty());

        gffManager.reindexGeneFile(geneFile.getId(), false, false);
        final IndexSearchResult<FeatureIndexEntry> searchResult2 =
                featureIndexDao.searchFeatures("ens", geneFile, 10);
        assertFalse(searchResult2.getEntries().isEmpty());
        assertTrue(searchResult2.getEntries().size() <= 10);
        assertTrue(searchResult2.isExceedsLimit());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testNoIndexVcf() throws IOException {
        final Resource resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        final FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());
        request.setDoIndex(false);
        request.setName(UUID.randomUUID().toString());

        final VcfFile vcfFile = vcfManager.registerVcfFile(request);

        assertNotNull(vcfFile);

        final Project project = new Project();
        project.setName(TEST_PROJECT_NAME + UUID.randomUUID().toString());
        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(vcfFile.getBioDataItemId())),
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));

        projectManager.create(project); // Index is created when vcf file is added

        assertTrue(featureIndexManager.filterVariations(new VcfFilterForm(), project.getId()).getEntries().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testNoIndexGene() throws IOException {
        final FeatureIndexedFileRegistrationRequest geneRequest = new FeatureIndexedFileRegistrationRequest();
        final Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);
        geneRequest.setReferenceId(referenceId);
        geneRequest.setPath(resource.getFile().getAbsolutePath());
        geneRequest.setDoIndex(false);
        geneRequest.setName(UUID.randomUUID().toString());

        final GeneFile geneFile = gffManager.registerGeneFile(geneRequest);
        assertNotNull(geneFile);

        assertTrue(featureIndexDao.searchFeatures(TEST_GENE_PREFIX.toLowerCase(), geneFile, 10)
                .getEntries().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pagingTest() throws IOException {
        final VcfFilterForm vcfFilterForm = new VcfFilterForm();

        final IndexSearchResult<VcfIndexEntry> entryList = featureIndexManager.filterVariations(vcfFilterForm,
                                                                                          testProject.getId());
        assertFalse(entryList.getEntries().isEmpty());

        vcfFilterForm.setPageSize(10);
        int total = featureIndexManager.getTotalPagesCount(vcfFilterForm, testProject.getId());

        final Set<VcfIndexEntry> pagedEntries = new HashSet<>();
        for (int i = ONE; i < total + ONE; i++) {
            vcfFilterForm.setPage(i);
            final IndexSearchResult<VcfIndexEntry> page = featureIndexManager.filterVariations(vcfFilterForm,
                                                                                         testProject.getId());
            assertFalse(page.getEntries().isEmpty());
            assertEquals(total, page.getTotalPagesCount().intValue());

            if (i < (entryList.getEntries().size() / 10) + ONE) { // check if only it is not the last page
                                                    // (there should be 4 variations)
                assertEquals(10, page.getEntries().size());
            } else {
                assertEquals(4, page.getEntries().size());
            }

            final List<VcfIndexEntry> duplicates = page.getEntries().stream()
                    .filter(pagedEntries::contains)
                .collect(toList());
            assertTrue(duplicates.isEmpty());
            pagedEntries.addAll(page.getEntries());
        }

        assertEquals(entryList.getEntries().size(), pagedEntries.size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    public void sortingTest() throws IOException {
        final VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setPage(ONE);
        vcfFilterForm.setPageSize(10);

        for (final VcfIndexSortField sortField : VcfIndexSortField.values()) {
            vcfFilterForm.setOrderBy(Collections.singletonList(new OrderBy(sortField.name(), false)));

            final IndexSearchResult<VcfIndexEntry> entryList = featureIndexManager.filterVariations(vcfFilterForm,
                                                                                              testProject.getId());
            assertFalse(entryList.getEntries().isEmpty());
            assertEquals(vcfFilterForm.getPageSize().intValue(), entryList.getEntries().size());
        }

        // check sorting by various fields
        checkSorted(VcfIndexSortField.START_INDEX.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> e.getStartIndex() > p.getStartIndex())),
                    testProject.getId());

        checkSorted(VcfIndexSortField.END_INDEX.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> e.getEndIndex() > p.getEndIndex())),
                    testProject.getId());

        checkSorted(VcfIndexSortField.CHROMOSOME_NAME.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(p -> seenEntries.stream().anyMatch(
                e -> e.getChromosome().getName().compareTo(p.getChromosome().getName()) > ZERO)),
                    testProject.getId());

        checkSorted(VcfIndexSortField.GENE_NAME.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> StringUtils.isNotBlank(e.getGeneName()) &&
                                                        StringUtils.isNotBlank(p.getGeneName()) &&
                                                        e.getGeneName().compareTo(p.getGeneName()) > ZERO)),
                    testProject.getId());

        checkSorted(VcfIndexSortField.GENE_NAME.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> StringUtils.isNotBlank(e.getGeneNames()) &&
                                                        StringUtils.isNotBlank(p.getGeneNames()) &&
                                                        e.getGeneNames().compareTo(p.getGeneNames()) > ZERO)),
                    testProject.getId());

        checkSorted(VcfIndexSortField.GENE_ID.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> StringUtils.isNotBlank(e.getGene()) &&
                                                        StringUtils.isNotBlank(p.getGene()) &&
                                                        e.getGene().compareTo(p.getGene()) > ZERO)),
                    testProject.getId());

        checkSorted(VcfIndexSortField.GENE_ID.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> StringUtils.isNotBlank(e.getGeneIds()) &&
                                                        StringUtils.isNotBlank(p.getGeneIds()) &&
                                                        e.getGeneIds().compareTo(p.getGeneIds()) > ZERO)),
                    testProject.getId());

        checkSorted(VcfIndexSortField.VARIATION_TYPE.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> e.getVariationType().name().compareTo(
                                p.getVariationType().name()) > ZERO)),
                    testProject.getId());

        checkSorted(VcfIndexSortField.FILTER.name(), false,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream().anyMatch(e -> (e.getFailedFilter() != null ? e.getFailedFilter() : "")
                                            .compareTo(p.getFailedFilter() != null ? p.getFailedFilter() : "") > ZERO)),
                    testProject.getId());

        // check order by additional fields
        final VcfFilterInfo info = vcfManager.getFiltersInfo(Collections.singletonList(testVcf.getId()));
        for (final InfoItem item : info.getInfoItems()) {
            switch (item.getType()) {
                case Integer:
                case Float:
                    checkSorted(item.getName(), false,
                        (page, seenEntries) -> page.stream().anyMatch(p -> seenEntries.stream().anyMatch(
                            e -> e.getInfo().containsKey(item.getName()) && e.getInfo().get(item.getName()) != null
                                 && e.getInfo().containsKey(item.getName()) && p.getInfo().get(item.getName()) != null
                                 && (e.getInfo().get(item.getName()).toString()).compareTo(
                                     p.getInfo().get(item.getName()).toString()) > ZERO
                        )),
                        testProject.getId(), Collections.singletonList(item.getName()));
                    break;
                default:
                    checkSorted(item.getName(), false,
                        (page, seenEntries) -> page.stream().anyMatch(p -> seenEntries.stream().anyMatch(
                            e -> e.getInfo().get(item.getName()) != null &&
                                 p.getInfo().get(item.getName()) != null &&
                                 e.getInfo().get(item.getName()).toString().compareTo(
                                     p.getInfo().get(item.getName()).toString()) > ZERO)),
                                testProject.getId(), Collections.singletonList(item.getName()));
            }
        }

        // Test sort desc
        checkSorted(VcfIndexSortField.START_INDEX.name(), true,
            (page, seenEntries) -> page.stream().anyMatch(
                p -> seenEntries.stream()
                        .anyMatch(e -> e.getStartIndex() < p.getStartIndex())),
            testProject.getId());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testSortByMultipleFields() throws IOException {
        // check sorted by multiple fields
        final IndexSearchResult<VcfIndexEntry> referentList = featureIndexManager.filterVariations(new VcfFilterForm(),
                                                                                testProject.getId());
        final List<VcfIndexEntry> pagedEntries = new ArrayList<>();

        final VcfFilterForm filterForm = new VcfFilterForm();
        filterForm.setPageSize(10);
        filterForm.setOrderBy(Arrays.asList(new OrderBy(VcfIndexSortField.START_INDEX.name(), false),
                                        new OrderBy(VcfIndexSortField.VARIATION_TYPE.name(), false)));

        for (int i = ONE; i < (referentList.getEntries().size() / 10) + 2; i++) {
            filterForm.setPage(i);
            final IndexSearchResult<VcfIndexEntry> pageRes = featureIndexManager.filterVariations(filterForm,
                                                                                            testProject.getId());
            final List<VcfIndexEntry> page = pageRes.getEntries();
            assertFalse(page.isEmpty());

            if (i < (referentList.getEntries().size() / 10) + ONE) { // check if only it is not the last page
                // (there should be 4 variations)
                assertEquals(page.size(), 10);
            } else {
                assertEquals(page.size(), 4);
            }

            final List<VcfIndexEntry> duplicates = page.stream()
                    .filter(pagedEntries::contains)
                    .collect(toList());
            assertTrue(duplicates.isEmpty());
            assertFalse(page.stream().anyMatch(p -> pagedEntries.stream().anyMatch(
                e -> e.getStartIndex() > p.getStartIndex())));
            assertFalse(page.stream().anyMatch(
                p -> pagedEntries.stream().anyMatch(e -> e.getVariationType().name().compareTo(
                    p.getVariationType().name()) > ZERO)));
            pagedEntries.addAll(page);
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void searchGenesByFilterWithFeatureName() throws IOException {
        final GeneFilterForm geneFilterForm = getSimpleGeneFilter();

        assertEquals(TEST_AMOUNT, featureIndexManager.searchGenesByReference(geneFilterForm, referenceId)
                .getEntries().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void searchGenesByFilterWithSpecificFiles() throws IOException {
        final GeneFilterForm geneFilterForm = getEmptyGeneFilter();
        geneFilterForm.setGeneFileIdsByProject(Collections.singletonMap(0L,
                Arrays.asList(testGeneFile.getId(), testGeneFile2.getId())));

        // check that we will found features from another file
        assertEquals(FULL_GENE_FILE_SIZE + ONE,
                featureIndexManager.searchGenesByReference(geneFilterForm, referenceId)
                .getEntries().size());

        geneFilterForm.setGeneFileIdsByProject(Collections.singletonMap(0L,
                Collections.singletonList(testGeneFile2.getId())));

        // check that we will found features from another file
        assertEquals(ONE, featureIndexManager.searchGenesByReference(geneFilterForm, referenceId)
                .getEntries().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void searchGenesWithCoreFilter() throws IOException {
        final GeneFilterForm geneFilterForm = getEmptyGeneFilter();
        geneFilterForm.setGeneFileIdsByProject(Collections.singletonMap(0L,
                Collections.singletonList(testGeneFile2.getId())));
        // check that we will found feature without filters
        assertEquals(ONE, featureIndexManager.searchGenesByReference(geneFilterForm, referenceId)
                .getEntries().size());

        // check that we will found feature with source filters
        geneFilterForm.setSources(Collections.singletonList("ensembl"));
        assertEquals(ONE, featureIndexManager.searchGenesByReference(geneFilterForm, referenceId)
                .getEntries().size());

        // check that we will found feature with frame filters
        geneFilterForm.setFrames(Collections.singletonList(GOOD_FRAME));
        assertEquals(ONE, featureIndexManager.searchGenesByReference(geneFilterForm, referenceId)
                .getEntries().size());

        // check that we will found feature with Score filters
        geneFilterForm.setScore(new GeneFilterForm.BoundsFilter<>(GOOD_SCORE_FROM, null));
        assertEquals(ONE, featureIndexManager.searchGenesByReference(geneFilterForm, referenceId)
                .getEntries().size());

        // check that we will found feature with Score filters
        geneFilterForm.setScore(new GeneFilterForm.BoundsFilter<>(GOOD_SCORE_FROM, WRONG_SCORE_FROM));
        assertEquals(ONE, featureIndexManager.searchGenesByReference(geneFilterForm, referenceId)
                .getEntries().size());

        // check that we will  dont found feature with Score wrong filters
        geneFilterForm.setScore(new GeneFilterForm.BoundsFilter<>(WRONG_SCORE_FROM, null));
        assertEquals(ZERO, featureIndexManager.searchGenesByReference(geneFilterForm, referenceId)
                .getEntries().size());

        geneFilterForm.setScore(null);

        // check that we will  found feature with additional filters
        geneFilterForm.setAdditionalFilters(Collections.singletonMap("gene_biotype", "protein_coding"));
        assertEquals(ONE, featureIndexManager.searchGenesByReference(geneFilterForm, referenceId)
                .getEntries().size());

        // check that we will  found feature with additional filters
        geneFilterForm.setAdditionalFilters(Collections.singletonMap("gene_biotype", "abracadabra"));
        assertEquals(ZERO, featureIndexManager.searchGenesByReference(geneFilterForm, referenceId)
                .getEntries().size());

    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void searchGenesWithCustomFeatureType() throws IOException {
        final GeneFilterForm geneFilterForm = getEmptyGeneFilter();
        geneFilterForm.setGeneFileIdsByProject(Collections.singletonMap(0L,
                Collections.singletonList(testGeneFile2.getId())));

        // check that we will found features from another file
        List<GeneIndexEntry> entries = featureIndexManager.searchGenesByReference(geneFilterForm, referenceId)
                .getEntries();
        assertEquals(ONE, entries.size());
        assertEquals(FeatureType.GENERIC_GENE_FEATURE, entries.get(ZERO).getFeatureType());
        assertEquals("REGION_FEATURE", entries.get(ZERO).getFeature());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void searchGenesByFilterWithChromosome() throws IOException {
        final GeneFilterForm geneFilterForm = getSimpleGeneFilter();
        geneFilterForm.setChromosomeIds(Arrays.asList(
                testChromosome.getId(), testChromosome.getId() + ONE, testChromosome.getId() + ONE));

        //We have only 1 chromosome A1 at the test data, other names are artificial:)
        assertEquals(TEST_AMOUNT, featureIndexManager.searchGenesByReference(geneFilterForm, referenceId)
                .getEntries().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void searchGenesByFilterWithFeatureTypes() throws IOException {
        final GeneFilterForm geneFilterForm = getSimpleGeneFilter();
        geneFilterForm.setPageSize((int)(TEST_AMOUNT_OF_GENE + TEST_AMOUNT_OF_MRNA));
        geneFilterForm.setFeatureTypes(Arrays.asList(FeatureType.MRNA.getFileValue(), FeatureType.GENE.getFileValue()));

        assertEquals(TEST_AMOUNT_OF_GENE + TEST_AMOUNT_OF_MRNA,
                featureIndexManager.searchGenesByReference(geneFilterForm, referenceId).getEntries().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void searchGenesByFilterWithPositions() throws IOException {
        final GeneFilterForm geneFilterForm = getSimpleGeneFilter();
        geneFilterForm.setStartIndex(TEST_START_INDEX);
        geneFilterForm.setEndIndex(TEST_END_INDEX);

        assertEquals(TEST_AMOUNT_POSITION,
                featureIndexManager.searchGenesByReference(geneFilterForm, referenceId).getEntries().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void searchGenesByFilterWithSorting() throws IOException {
        final GeneFilterForm geneFilterForm = getSimpleGeneFilter();
        geneFilterForm.setFeatureTypes(Collections.singletonList(FeatureType.GENE.getFileValue()));
        geneFilterForm.setOrderBy(Collections.singletonList(new OrderBy("START_INDEX", true)));

        final IndexSearchResult<GeneIndexEntry> result = featureIndexManager.searchGenesByReference(
                geneFilterForm, referenceId);

        assertEquals(TEST_AMOUNT_OF_GENE, result.getEntries().size());

        final Integer maxInt = result.getEntries().stream()
                .map(FeatureIndexEntry::getStartIndex)
                .max(Comparator.naturalOrder())
                .orElse(ZERO);

        assertEquals(maxInt, result.getEntries().get(ZERO).getStartIndex());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void searchGenesByFilterWithSortingAndPaging() throws IOException {
        final GeneFilterForm geneFilterForm = getSmallGeneFilter();
        geneFilterForm.setFeatureTypes(Collections.singletonList(FeatureType.GENE.getFileValue()));
        geneFilterForm.setOrderBy(Collections.singletonList(new OrderBy("START_INDEX", true)));

        IndexSearchResult<GeneIndexEntry> result = featureIndexManager.searchGenesByReference(
                geneFilterForm, referenceId);

        assertEquals(SMALL_PAGE_SIZE, result.getEntries().size());
        assertNotNull(result.getPointer());

        final Integer maxInt = result.getEntries().stream()
                .map(FeatureIndexEntry::getStartIndex)
                .max(Comparator.naturalOrder())
                .orElse(ZERO);

        assertEquals(maxInt, result.getEntries().get(ZERO).getStartIndex());

        geneFilterForm.setPointer(result.getPointer());
        result = featureIndexManager.searchGenesByReference(geneFilterForm, referenceId);

        assertEquals(ONE, result.getEntries().size());

    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void searchCDS() throws IOException {
        final GeneFilterForm geneFilterForm = getSimpleGeneFilter();
        geneFilterForm.setFeatureId(null);
        geneFilterForm.setFeatureTypes(Collections.singletonList(FeatureType.CDS.getFileValue()));

        final IndexSearchResult<GeneIndexEntry> result = featureIndexManager.searchGenesByReference(
                geneFilterForm, referenceId);

        assertFalse(result.getEntries().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void getAvailableGeneAttributes() {
        GeneFilterInfo availableFields = featureIndexManager.getAvailableGeneFieldsToSearch(referenceId,
                new ItemsByProject());
        assertNotNull(availableFields);
        assertTrue(availableFields.getAvailableFilters().contains("gene_name"));
        assertTrue(availableFields.getAvailableFilters().contains("gene_source"));
        assertTrue(availableFields.getAvailableFilters().contains("gene_biotype"));
        assertTrue(availableFields.getAvailableFilters().contains("mrna_name"));
        assertTrue(availableFields.getAvailableFilters().contains("mrna_biotype"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void searchGenesByFilterWithIncorrectSortingName() throws IOException {
        final GeneFilterForm geneFilterForm = getSimpleGeneFilter();
        geneFilterForm.setOrderBy(Collections.singletonList(new OrderBy("TEST_TEST", true)));

        final IndexSearchResult<GeneIndexEntry> result = featureIndexManager.searchGenesByReference(
                geneFilterForm, referenceId);

        assertEquals(TEST_AMOUNT, result.getEntries().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void searchGenesByFilterWithPageSizeAndIncorrectSortingName() throws IOException {
        final GeneFilterForm geneFilterForm = getSimpleGeneFilter();
        geneFilterForm.setOrderBy(Collections.singletonList(new OrderBy("TEST_TEST", true)));
        geneFilterForm.setPageSize((int) TEST_AMOUNT);

        final IndexSearchResult<GeneIndexEntry> result = featureIndexManager.searchGenesByReference(
                geneFilterForm, referenceId);

        assertEquals(TEST_AMOUNT, result.getEntries().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void searchGenesByFilterWithPageSize() throws IOException {
        final GeneFilterForm geneFilterForm = getSimpleGeneFilter();
        geneFilterForm.setPageSize((int) TEST_PAGE_SIZE);

        assertEquals(TEST_PAGE_SIZE, featureIndexManager.searchGenesByReference(geneFilterForm, referenceId)
                .getEntries().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testExportGeneTable() throws IOException {
        final GeneExportFilterForm geneFilterForm = new GeneExportFilterForm();
        geneFilterForm.setFeatureId("ENSFCAG000000");
        geneFilterForm.setExportFields(Arrays.asList("source", "featureId", "gene_version", "gene_source", "empty"));

        assertNotNull(exportManager.exportGenesByReference(geneFilterForm, referenceId, ExportFormat.TSV, true));
    }

    @Test
    @Ignore
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void searchGenesByFilterWithPointer() throws IOException {
        final GeneFilterForm geneFilterForm = getSimpleGeneFilter();
        geneFilterForm.setPageSize((int) TEST_PAGE_SIZE);

        final Pointer pointer = new Pointer();
        pointer.setScore(1.0F);
        pointer.setDoc((int) TEST_PAGE_SIZE - ONE);
        pointer.setShardIndex(ZERO);
        geneFilterForm.setPointer(pointer);

        assertEquals(TEST_AMOUNT - TEST_PAGE_SIZE,
                featureIndexManager.searchGenesByReference(geneFilterForm, referenceId).getEntries().size());
    }

    private GeneFilterForm getSimpleGeneFilter() {
        final GeneFilterForm geneFilterForm = new GeneFilterForm();
        geneFilterForm.setFeatureId("ENSFCA");
        geneFilterForm.setPageSize(DEFAULT_PAGE_SIZE);
        return geneFilterForm;
    }

    private GeneFilterForm getEmptyGeneFilter() {
        final GeneFilterForm geneFilterForm = new GeneFilterForm();
        geneFilterForm.setPageSize(DEFAULT_PAGE_SIZE);
        return geneFilterForm;
    }

    private GeneFilterForm getSmallGeneFilter() {
        final GeneFilterForm geneFilterForm = new GeneFilterForm();
        geneFilterForm.setFeatureId("ENSFCA");
        geneFilterForm.setPageSize(SMALL_PAGE_SIZE);
        return geneFilterForm;
    }

    private void checkSorted(final String orderBy, final boolean desc, final SortTestingPredicate testingPredicate,
                             final Long projectId) throws IOException {
        checkSorted(orderBy, desc, testingPredicate, projectId, null);
    }

    private void checkSorted(final String orderBy, final boolean desc, final SortTestingPredicate testingPredicate,
                             final Long projectId, final List<String> additionalFields) throws IOException {
        final IndexSearchResult<VcfIndexEntry> referentList = featureIndexManager.filterVariations(new VcfFilterForm(),
                                                                                             projectId);
        final List<VcfIndexEntry> pagedEntries = new ArrayList<>();

        final VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setPageSize(10);
        vcfFilterForm.setOrderBy(Collections.singletonList(new OrderBy(orderBy, desc)));
        vcfFilterForm.setInfoFields(additionalFields);

        for (int i = ONE; i < (referentList.getEntries().size() / 10) + 2; i++) {
            vcfFilterForm.setPage(i);
            final IndexSearchResult<VcfIndexEntry> pageRes = featureIndexManager
                    .filterVariations(vcfFilterForm, projectId);
            final List<VcfIndexEntry> page = pageRes.getEntries();

            assertFalse(page.isEmpty());

            if (i < (referentList.getEntries().size() / 10) + ONE) { // check if only it is not the last page
                // (there should be 4 variations)
                assertEquals(page.size(), 10);
            } else {
                assertEquals(page.size(), 4);
            }

            final List<VcfIndexEntry> duplicates = page.stream()
                    .filter(pagedEntries::contains)
                    .collect(toList());
            assertTrue(duplicates.isEmpty());
            assertFalse(testingPredicate.doTest(page, pagedEntries));
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
        final IndexSearchResult<VcfIndexEntry> entryList1 = featureIndexManager.filterVariations(new VcfFilterForm(),
                testProject.getId());
        assertFalse(entryList1.getEntries().isEmpty());

        testGroupingBy(VcfIndexSortField.CHROMOSOME_NAME);
        testGroupingBy(VcfIndexSortField.QUALITY);

        // test load additional info and group by it
        final VcfFilterInfo info = vcfManager.getFiltersInfo(Collections.singletonList(testVcf.getId()));
        final VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setInfoFields(info.getInfoItems().stream()
                .map(InfoItem::getName)
                .collect(toList()));

        final IndexSearchResult<VcfIndexEntry> entryList2 = featureIndexManager.filterVariations(vcfFilterForm,
                testProject.getId());
        assertFalse(entryList2.getEntries().isEmpty());

        for (final InfoItem infoItem : info.getInfoItems()) {
            final String groupByField = infoItem.getName();
            final List<Group> c = featureIndexManager.groupVariations(new VcfFilterForm(), testProject.getId(),
                    groupByField);
            final List<VcfIndexEntry> entriesWithField = entryList2.getEntries().stream()
                    .filter(e -> e.getInfo().get(groupByField) != null)
                    .collect(toList());
            if (!entriesWithField.isEmpty()) {
                assertFalse("Empty grouping for field: " + groupByField, c.isEmpty());
            }
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testGroupingForPlots() throws IOException {
        testGroupingBy(VcfIndexSortField.CHROMOSOME_NAME);
        testGroupingBy(VcfIndexSortField.VARIATION_TYPE);
        testGroupingBy(VcfIndexSortField.QUALITY);
    }

    private void testGroupingBy(final VcfIndexSortField field) throws IOException {
        final List<Group> counts = featureIndexManager.groupVariations(new VcfFilterForm(), testProject.getId(),
                field.name());
        assertFalse(counts.isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testMultipleVariationTypes() throws IOException {
        final FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        final Resource resource = context.getResource("classpath:templates/samples.vcf");
        request.setPath(resource.getFile().getAbsolutePath());

        final VcfFile samplesVcf = vcfManager.registerVcfFile(request);

        final VcfFilterForm form = new VcfFilterForm();
        form.setVcfFileIdsByProject(Collections.singletonMap(testProject.getId(),
                Collections.singletonList(samplesVcf.getId())));

        final IndexSearchResult<VcfIndexEntry> res1 = featureIndexManager.filterVariations(form);
        final long mnpCount = res1.getEntries().stream()
                .filter(e -> e.getVariationTypes().contains(VariationType.MNP))
                .count();
        final long insCount = res1.getEntries().stream()
                .filter(e -> e.getVariationTypes().contains(VariationType.INS))
                .count();

        final List<Group> variationTypes =
                featureIndexManager.groupVariations(form, FeatureIndexDao.FeatureIndexFields.VARIATION_TYPE.name());
        final Group mnp = variationTypes.stream()
                .filter(g -> g.getGroupName().equals("MNP"))
                .findFirst()
                .get();
        final Group ins = variationTypes.stream()
                .filter(g -> g.getGroupName().equals("INS"))
                .findFirst()
                .get();

        assertEquals((int) mnpCount, mnp.getEntriesCount().intValue());
        assertEquals((int) insCount, ins.getEntriesCount().intValue());

        form.setVariationTypes(new FilterSection<>(Collections.singletonList(VariationType.INS)));
        final IndexSearchResult<VcfIndexEntry> res2 = featureIndexManager.filterVariations(form);

        assertTrue(res2.getEntries().stream()
                .anyMatch(e -> e.getFeatureId().equals("rs11804171")));

        form.setVariationTypes(new FilterSection<>(Collections.singletonList(VariationType.MNP)));

        final IndexSearchResult<VcfIndexEntry> res3 = featureIndexManager.filterVariations(form);
        assertTrue(res3.getEntries().stream()
                .anyMatch(e -> e.getFeatureId().equals("rs11804171")));

        form.setVariationTypes(null);
        form.setPage(ONE);
        form.setPageSize(5);
        form.setOrderBy(Collections.singletonList(
                new OrderBy(FeatureIndexDao.FeatureIndexFields.VARIATION_TYPE.name(), false)));

        final IndexSearchResult<VcfIndexEntry> res4 = featureIndexManager.filterVariations(form);
        assertFalse(res4.getEntries().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testExportVariations() throws IOException {
        final FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        final Resource resource = context.getResource("classpath:templates/samples.vcf");
        request.setPath(resource.getFile().getAbsolutePath());

        final VcfFile samplesVcf = vcfManager.registerVcfFile(request);

        final VcfExportFilterForm form = new VcfExportFilterForm();
        form.setVcfFileIdsByProject(Collections.singletonMap(testProject.getId(),
                Collections.singletonList(samplesVcf.getId())));
        form.setExportFields(Arrays.asList("variationType", "featureType", "chromosome", "empty"));

        final byte[] exportResult = exportManager.exportVariations(form, ExportFormat.CSV, true);
        assertNotNull(exportResult);
    }

    @Test
    @Ignore // TODO: remove this test before merging to master
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void performanceTest() throws Exception {
        Reference hg38 = EntityHelper.createG38Reference(referenceGenomeManager.createReferenceId());
        referenceGenomeManager.create(hg38);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(hg38.getId());
        request.setPath("/home/kite/Documents/sampleData/Dream.set3.VarDict.SV.vcf");

        VcfFile vcfFile1 = vcfManager.registerVcfFile(request);

        request.setPath("/home/kite/Documents/sampleData/synthetic.challenge.set3.tumor.20pctmasked.truth.vcf");
        VcfFile vcfFile2 = vcfManager.registerVcfFile(request);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME + ONE);
        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(vcfFile1.getBioDataItemId())),
                                       new ProjectItem(new BiologicalDataItem(vcfFile2.getBioDataItemId()))));

        projectManager.create(project);

        IndexSearchResult<VcfIndexEntry> entriesRes = featureIndexManager.filterVariations(new VcfFilterForm(),
                                                                                           project.getId());
        List<VcfIndexEntry> entries = entriesRes.getEntries();
        assertFalse(entries.isEmpty());
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
        filterForm.setPage(ONE);
        filterForm.setPageSize(PERFORMANCE_TEST_PAGE_SIZE);
        TestUtils.warmUp(() -> featureIndexManager.filterVariations(filterForm, project.getId()),
                         PERFORMANCE_TEST_WARMING_COUNT);

        averageTime = TestUtils.measurePerformance(
            () -> featureIndexManager.filterVariations(filterForm, project.getId()),
            PERFORMANCE_TEST_ATTEMPTS_COUNT);

        logger.info("!! Performing index search single page took: {} ms", averageTime);

        TestUtils.warmUp(
            () -> ThreadLocalRandom.current().nextInt(ONE, entries.size() / PERFORMANCE_TEST_PAGE_SIZE + ONE),
            (page) -> {
                filterForm.setPage(page);
                featureIndexManager.filterVariations(filterForm, project.getId());
            }, PERFORMANCE_TEST_WARMING_COUNT);

        List<Double> timings = TestUtils.measurePerformanceTimings(
            () -> ThreadLocalRandom.current().nextInt(ONE, entries.size() / PERFORMANCE_TEST_PAGE_SIZE + ONE),
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

    private void checkDuplicates(final List<VcfIndexEntry> entryList) {
        final Map<Pair<Integer, Integer>, FeatureIndexEntry> duplicateMap = new HashMap<>();
        entryList.forEach(e -> {
            final Pair<Integer, Integer> indexPair = new ImmutablePair<>(e.getStartIndex(), e.getEndIndex());
            assertFalse(String.format("Found duplicate: %d, %d", e.getStartIndex(), e.getEndIndex()),
                    duplicateMap.containsKey(indexPair));
            duplicateMap.put(indexPair, e);
        });
    }
}
