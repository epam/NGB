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
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.index.BookmarkIndexEntry;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.FeatureType;
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
public class FeatureIndexManagerTest {
    private static final String CLASSPATH_TEMPLATES_FELIS_CATUS_VCF = "classpath:templates/Felis_catus.vcf";
    private static final String CLASSPATH_TEMPLATES_GENES_SORTED = "classpath:templates/genes_sorted.gtf";
    private static final int SVLEN_VALUE = -150;
    //public static final float QUAL_VALUE = -10.0F;
    private static final int CONST_42 = 42;
    private static final int TEST_WICKED_VCF_LENGTH = 248617560;

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
    private static final String TEST_GENE_AND_FILE_ID_QUERY = "gene:ENS* AND fileId:%d";
    private static final String SVTYPE_FIELD = "SVTYPE";
    private static final String SVLEN_FIELD = "SVLEN";

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
    public void testCreateFeatureIndex() throws Exception {
        Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), geneFile.getId());

        resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        VcfFile vcfFile = vcfManager.registerVcfFile(request);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(vcfFile.getBioDataItemId())),
                new ProjectItem(new BiologicalDataItem(geneFile.getBioDataItemId()))));

        projectManager.saveProject(project); // Index is created when vcf file is added

        List<FeatureIndexEntry> entryList = (List<FeatureIndexEntry>) featureIndexDao.searchFileIndexes(
                Collections.singletonList(vcfFile),  String.format(TEST_GENE_AND_FILE_ID_QUERY, vcfFile.getId()),
                null).getEntries();
        Assert.assertFalse(entryList.isEmpty());

        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(vcfFile.getId()));
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm.setVariationTypes(new VcfFilterForm.FilterSection<>(Arrays.asList(VariationType.MNP,
                VariationType.SNV), false));
        //vcfFilterForm.setQuality(Collections.singletonList(QUAL_VALUE));
        List<VcfIndexEntry> entryList2 = featureIndexManager.filterVariations(vcfFilterForm, project.getId());
        Assert.assertFalse(entryList2.isEmpty());
        Assert.assertTrue(entryList2.stream().anyMatch(VcfIndexEntry::getExon));

        vcfFilterForm.setChromosomeId(testChromosome.getId());
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm, project.getId());
        Assert.assertFalse(entryList2.isEmpty());

        double time1 = Utils.getSystemTimeMilliseconds();
        List<Long> chromosomeIds = featureIndexDao.getChromosomeIdsWhereVariationsPresentFacet(
            Collections.singletonList(vcfFile), "gene:ENS* AND fileId:" + vcfFile.getId() + " AND variationType:snv");
        double time2 = Utils.getSystemTimeMilliseconds();
        logger.info("Get chromosomes by facets time: {} ms", time2 - time1);

        Assert.assertFalse(chromosomeIds.isEmpty());

        List<Chromosome> chromosomes = featureIndexManager.filterChromosomes(vcfFilterForm, project.getId());
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
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm, project.getId());
        Assert.assertFalse(entryList2.isEmpty());
        Assert.assertFalse(entryList2.stream().anyMatch(e -> e.getInfo().isEmpty()));

        Set<String> genes = featureIndexManager.searchGenesInVcfFilesInProject(project.getId(), TEST_GENE_PREFIX,
                Collections.singletonList(vcfFile.getId()));
        Assert.assertFalse(genes.isEmpty());

        // search by gene name pglyrp4
        vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList(TEST_GENE_NAME)));
        List<VcfIndexEntry> entries = featureIndexManager.filterVariations(vcfFilterForm, project.getId());
        Assert.assertFalse(entries.isEmpty());

        genes = featureIndexManager.searchGenesInVcfFilesInProject(project.getId(), TEST_GENE_NAME,
                Collections.singletonList(vcfFile.getId()));
        Assert.assertFalse(genes.isEmpty());

        // search exons
        vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setExon(true);
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm, project.getId());
        Assert.assertFalse(entryList2.isEmpty());
        Assert.assertTrue(entryList2.stream().allMatch(VcfIndexEntry::getExon));

        // check duplicates
        entryList2 = featureIndexManager.filterVariations(new VcfFilterForm(), project.getId());
        checkDuplicates(entryList2);
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

        List<VcfIndexEntry> entryList2 = featureIndexManager.filterVariations(vcfFilterForm, project.getId());
        Assert.assertFalse(entryList2.isEmpty());
        Assert.assertTrue(entryList2.stream().anyMatch(e -> e.getInfo().containsKey(cipos95)));
        Assert.assertTrue(entryList2.stream().filter(e -> e.getInfo().containsKey(cipos95)).allMatch(e -> {
            String cipos = (String) e.getInfo().get(cipos95);
            return cipos.startsWith("[") && cipos.endsWith("]");
        }));

        // check info properly loaded
        for (VcfIndexEntry e : entryList2) {
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
        List<VcfIndexEntry> entryList21 = featureIndexManager.filterVariations(vcfFilterForm, project.getId());
        Assert.assertFalse(entryList21.isEmpty());
        Assert.assertEquals(entryList21.size(), entryList2.size());
        Assert.assertEquals(entryList21.get(0).getGene(), entryList2.get(0).getGene());

        // empty filter test
        entryList2 = featureIndexManager.filterVariations(new VcfFilterForm(), project.getId());
        Assert.assertFalse(entryList2.isEmpty());
        checkDuplicates(entryList2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testIndexUpdateOnProjectOperations() throws Exception {
        Resource gffResource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(gffResource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), geneFile.getId());

        Resource vcfResource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(vcfResource.getFile().getAbsolutePath());

        VcfFile vcfFile = vcfManager.registerVcfFile(request);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(vcfFile.getBioDataItemId())),
                new ProjectItem(new BiologicalDataItem(geneFile.getBioDataItemId()))));

        projectManager.saveProject(project); // Index is created when vcf file is added

        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(vcfFile.getId()));
        vcfFilterForm.setChromosomeId(testChromosome.getId());
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm.setVariationTypes(new VcfFilterForm.FilterSection<>(Arrays.asList(VariationType.MNP,
                VariationType.SNV), false));
        List<VcfIndexEntry> entryList = featureIndexManager.filterVariations(vcfFilterForm, project.getId());
        Assert.assertFalse(entryList.isEmpty());

        // try to add an vcf item
        request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(vcfResource.getFile().getAbsolutePath());
        VcfFile vcfFile2 = vcfManager.registerVcfFile(request);

        project = projectManager.addProjectItem(project.getId(), vcfFile2.getBioDataItemId());

        entryList = featureIndexManager.filterVariations(vcfFilterForm, project.getId());
        Assert.assertFalse(entryList.isEmpty());
        Assert.assertTrue(entryList.stream().allMatch(e -> e.getFeatureFileId().equals(vcfFile.getId())));


        VcfFilterForm vcfFilterForm2 = new VcfFilterForm();
        vcfFilterForm2.setVcfFileIds(Collections.singletonList(vcfFile2.getId()));
        vcfFilterForm2.setChromosomeId(testChromosome.getId());
        vcfFilterForm2.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm2.setVariationTypes(new VcfFilterForm.FilterSection<>(Arrays.asList(VariationType.MNP,
                VariationType.SNV), false));
        List<VcfIndexEntry> entryList2 = featureIndexManager.filterVariations(vcfFilterForm2, project.getId());
        Assert.assertFalse(entryList2.isEmpty());
        Assert.assertEquals(entryList.size(), entryList2.size());

        Assert.assertTrue(entryList2.stream().allMatch(e -> e.getFeatureFileId().equals(vcfFile2.getId())));

        // test no vcfFileIds
        vcfFilterForm2.setVcfFileIds(null);
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm2, project.getId());
        Assert.assertFalse(entryList2.isEmpty());
        Assert.assertEquals(entryList2.size(), entryList.size() * 2);

        // test with multiple vcfFileIds
        vcfFilterForm2.setVcfFileIds(Arrays.asList(vcfFile.getId(), vcfFile2.getId()));
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm2, project.getId());
        Assert.assertFalse(entryList2.isEmpty());
        Assert.assertEquals(entryList2.size(), entryList.size() * 2);

        // try to remove a vcf item by save - should be not indexed
        project.setItems(project.getItems().stream()
                .filter(i -> !(i.getBioDataItem() instanceof VcfFile) || !((VcfFile) i.getBioDataItem()).getId().equals(
                        vcfFile2.getId()))
                .collect(Collectors.toList()));

        project = projectManager.saveProject(project);

        vcfFilterForm2.setVcfFileIds(Collections.singletonList(vcfFile2.getId()));
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm2, project.getId());
        Assert.assertTrue(entryList2.isEmpty());

        // try to remove gene file
        project.setItems(project.getItems().stream()
                .filter(i -> !(i.getBioDataItem() instanceof GeneFile))
                .collect(Collectors.toList()));
        project = projectManager.saveProject(project);

        /*entryList = featureIndexManager.filterVariations(vcfFilterForm, project.getId());
        Assert.assertTrue(entryList.isEmpty());*/

        /*IndexSearchResult searchResult = featureIndexManager.searchFeatures("ensf", project.getId());
        Assert.assertTrue(searchResult.getEntries().isEmpty());*/

        vcfFilterForm.setGenes(null);
        entryList = featureIndexManager.filterVariations(vcfFilterForm, project.getId());
        Assert.assertFalse(entryList.isEmpty());
        /*Assert.assertTrue(entryList.stream().allMatch(e -> ((VcfIndexEntry) e).getGene() == null
                && ((VcfIndexEntry) e).getGeneIds() == null));*/

        // add multiple files
        project.getItems().clear();
        projectManager.saveProject(project);
        Project loadedProject = projectManager.loadProject(project.getId());
        Assert.assertTrue(loadedProject.getItems().isEmpty());
        entryList2 = featureIndexManager.filterVariations(new VcfFilterForm(), project.getId());
        Assert.assertTrue(entryList2.isEmpty());

        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(vcfFile.getBioDataItemId())),
                new ProjectItem(new BiologicalDataItem(vcfFile2.getBioDataItemId()))));
        projectManager.saveProject(project);
        entryList2 = featureIndexManager.filterVariations(new VcfFilterForm(), project.getId());
        Assert.assertTrue(entryList2.stream().filter(e -> e.getFeatureFileId().equals(vcfFile.getId()))
                .findAny().isPresent());
        Assert.assertTrue(entryList2.stream().filter(e -> e.getFeatureFileId().equals(vcfFile2.getId()))
                .findAny().isPresent());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadVcfFilterInfoForProject()
        throws IOException, InterruptedException, NoSuchAlgorithmException, VcfReadingException, FeatureIndexException {
        Resource resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        VcfFile vcfFile = vcfManager.registerVcfFile(request);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
        project.setItems(Collections.singletonList(new ProjectItem(
                new BiologicalDataItem(vcfFile.getBioDataItemId()))));

        projectManager.saveProject(project); // Index is created when vcf file is added

        VcfFilterInfo filterInfo = featureIndexManager.loadVcfFilterInfoForProject(project.getId());
        Assert.assertFalse(filterInfo.getAvailableFilters().isEmpty());
        Assert.assertFalse(filterInfo.getInfoItems().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testCreateGeneIndex() throws IOException, InterruptedException, FeatureIndexException,
                                             NoSuchAlgorithmException, VcfReadingException {
        Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), geneFile.getId());

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);

        project.setItems(Arrays.asList(new ProjectItem(testReference), new ProjectItem(new BiologicalDataItem(
            geneFile.getBioDataItemId()))));
        projectManager.saveProject(project);

        IndexSearchResult searchResult = featureIndexManager.searchFeatures("", project.getId());
        Assert.assertTrue(searchResult.getEntries().isEmpty());

        searchResult = featureIndexManager.searchFeatures("ens", project.getId());
        Assert.assertFalse(searchResult.getEntries().isEmpty());
        Assert.assertTrue(searchResult.getEntries().size() <= 10);
        Assert.assertTrue(searchResult.isExceedsLimit());

        // ensfcag00000031547 and ccdc115
        searchResult = featureIndexManager.searchFeatures("ensfcag00000031547", project.getId());
        Assert.assertEquals(searchResult.getEntries().size(), 1);
        searchResult = featureIndexManager.searchFeatures("ccdc115", project.getId());
        Assert.assertEquals(searchResult.getEntries().size(), 2);
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
        project.setName(TEST_PROJECT_NAME);

        project.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(geneFile.getBioDataItemId()))));
        projectManager.saveProject(project);

        List<FeatureIndexEntry> entryList = (List<FeatureIndexEntry>)
                featureIndexManager.searchFeatures("", project.getId()).getEntries();
        Assert.assertTrue(entryList.isEmpty());

        entryList = (List<FeatureIndexEntry>) featureIndexManager.searchFeatures("AM992871", project.getId())
                .getEntries();
        Assert.assertTrue(entryList.isEmpty()); // we don't search for exons
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testBookmarkSearch() throws IOException, InterruptedException, FeatureIndexException {
        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);

        projectManager.saveProject(project);

        Bookmark bookmark = new Bookmark();
        bookmark.setChromosome(testChromosome);
        bookmark.setStartIndex(1);
        bookmark.setEndIndex(testChromosome.getSize());
        bookmark.setName("testBookmark");

        bookmarkManager.saveBookmark(bookmark);
        Bookmark loadedBookmark = bookmarkManager.loadBookmark(bookmark.getId());
        Assert.assertNotNull(loadedBookmark);

        IndexSearchResult result = featureIndexManager.searchFeatures(bookmark.getName(), project.getId());
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
        Reference testHumanReference = EntityHelper.createNewReference(chr1,
                referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(testHumanReference);
        Long humanReferenceId = testHumanReference.getId();

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
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

        List<VcfIndexEntry> entries = featureIndexManager.filterVariations(new VcfFilterForm(), project.getId());
        Assert.assertFalse(entries.isEmpty());

        long varGenesCount = entries.stream().filter(e -> StringUtils.isNotBlank(e.getGene())).count();
        Assert.assertTrue(varGenesCount > 0);
        /*entries.stream().filter(e -> StringUtils.isNotBlank(e.getGene())).forEach(e -> logger.info("{} - {}, {}", e
                .getStartIndex(), e.getEndIndex(), e.getGeneIds()));*/
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testSearchIndexForFile() throws IOException, FeatureIndexException {
        FeatureIndexedFileRegistrationRequest geneRequest = new FeatureIndexedFileRegistrationRequest();
        Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);
        geneRequest.setReferenceId(referenceId);
        geneRequest.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(geneRequest);

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), geneFile.getId());

        resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        VcfFile vcfFile = vcfManager.registerVcfFile(request);

        List<FeatureIndexEntry> entryList = (List<FeatureIndexEntry>) featureIndexDao.searchFileIndexes(
            Collections.singletonList(vcfFile), String.format(TEST_GENE_AND_FILE_ID_QUERY, vcfFile.getId()),
            null).getEntries();
        Assert.assertFalse(entryList.isEmpty());

        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(vcfFile.getId()));
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm.setVariationTypes(new VcfFilterForm.FilterSection<>(Arrays.asList(VariationType.MNP,
                                                                                        VariationType.SNV), false));
        //vcfFilterForm.setQuality(Collections.singletonList(QUAL_VALUE));
        List<VcfIndexEntry> entryList2 = featureIndexManager.filterVariations(vcfFilterForm);
        Assert.assertFalse(entryList2.isEmpty());
        Assert.assertTrue(entryList2.stream().anyMatch(VcfIndexEntry::getExon));

        vcfFilterForm.setChromosomeId(testChromosome.getId());
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm);
        Assert.assertFalse(entryList2.isEmpty());

        double time1 = Utils.getSystemTimeMilliseconds();
        List<Long> chromosomeIds = featureIndexDao.getChromosomeIdsWhereVariationsPresentFacet(
            Collections.singletonList(vcfFile), "gene:ENS* AND fileId:" + vcfFile.getId() +
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
        Assert.assertFalse(entryList2.isEmpty());
        Assert.assertFalse(entryList2.stream().anyMatch(e -> e.getInfo().isEmpty()));

        Set<String> genes = featureIndexManager.searchGenesInVcfFiles(TEST_GENE_PREFIX, Collections.singletonList(
                                                                                                    vcfFile.getId()));
        Assert.assertFalse(genes.isEmpty());

        // search by gene name pglyrp4
        vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(vcfFile.getId()));
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList(TEST_GENE_NAME)));
        List<VcfIndexEntry> entries = featureIndexManager.filterVariations(vcfFilterForm);
        Assert.assertFalse(entries.isEmpty());

        genes = featureIndexManager.searchGenesInVcfFiles(TEST_GENE_NAME, Collections.singletonList(vcfFile.getId()));
        Assert.assertFalse(genes.isEmpty());

        // search exons
        vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(vcfFile.getId()));
        vcfFilterForm.setExon(true);
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm);
        Assert.assertFalse(entryList2.isEmpty());
        Assert.assertTrue(entryList2.stream().allMatch(VcfIndexEntry::getExon));

        // check duplicates
        vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(vcfFile.getId()));
        entryList2 = featureIndexManager.filterVariations(vcfFilterForm);
        checkDuplicates(entryList2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testGeneIndexForFile() throws IOException {
        FeatureIndexedFileRegistrationRequest geneRequest = new FeatureIndexedFileRegistrationRequest();
        Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);
        geneRequest.setReferenceId(referenceId);
        geneRequest.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(geneRequest);

        IndexSearchResult searchResult = featureIndexDao.searchFeatures("", geneFile, null);
        Assert.assertTrue(searchResult.getEntries().isEmpty());

        searchResult = featureIndexDao.searchFeatures(TEST_GENE_PREFIX.toLowerCase(), geneFile, 10);
        Assert.assertFalse(searchResult.getEntries().isEmpty());
        Assert.assertTrue(searchResult.getEntries().size() <= 10);
        Assert.assertTrue(searchResult.isExceedsLimit());

        // ensfcag00000031547 and ccdc115
        searchResult = featureIndexDao.searchFeatures("ensfcag00000031547", geneFile, null);
        Assert.assertEquals(searchResult.getEntries().size(), 1);
        searchResult = featureIndexDao.searchFeatures("ccdc115", geneFile, null);
        Assert.assertEquals(searchResult.getEntries().size(), 2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testReindexVcf() throws FeatureIndexException, IOException {
        FeatureIndexedFileRegistrationRequest geneRequest = new FeatureIndexedFileRegistrationRequest();
        Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);
        geneRequest.setReferenceId(referenceId);
        geneRequest.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(geneRequest);

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), geneFile.getId());

        resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        VcfFile vcfFile = vcfManager.registerVcfFile(request);

        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(vcfFile.getId()));
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList(TEST_GENE_PREFIX), false));
        vcfFilterForm.setVariationTypes(new VcfFilterForm.FilterSection<>(Arrays.asList(VariationType.MNP,
                                                                                        VariationType.SNV), false));
        List<VcfIndexEntry> entryList = featureIndexManager.filterVariations(vcfFilterForm);
        Assert.assertFalse(entryList.isEmpty());

        fileManager.deleteFileFeatureIndex(vcfFile);

        TestUtils.assertFail(() -> featureIndexManager.filterVariations(vcfFilterForm),
                             Collections.singletonList(IllegalArgumentException.class));

        vcfManager.reindexVcfFile(vcfFile.getId());
        entryList = featureIndexManager.filterVariations(vcfFilterForm);
        Assert.assertFalse(entryList.isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testReindexGene() throws IOException {
        FeatureIndexedFileRegistrationRequest geneRequest = new FeatureIndexedFileRegistrationRequest();
        Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);
        geneRequest.setReferenceId(referenceId);
        geneRequest.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(geneRequest);

        IndexSearchResult searchResult = featureIndexDao.searchFeatures(TEST_GENE_PREFIX.toLowerCase(), geneFile, 10);
        Assert.assertFalse(searchResult.getEntries().isEmpty());
        Assert.assertTrue(searchResult.getEntries().size() <= 10);
        Assert.assertTrue(searchResult.isExceedsLimit());

        fileManager.deleteFileFeatureIndex(geneFile);
        TestUtils.assertFail(() -> featureIndexDao.searchFeatures(TEST_GENE_PREFIX.toLowerCase(), geneFile, 10),
                             Collections.singletonList(IllegalArgumentException.class));

        gffManager.reindexGeneFile(geneFile.getId());
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

        VcfFile vcfFile = vcfManager.registerVcfFile(request);

        Assert.assertNotNull(vcfFile);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
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

        GeneFile geneFile = gffManager.registerGeneFile(geneRequest);
        Assert.assertNotNull(geneFile);

        TestUtils.assertFail(() -> featureIndexDao.searchFeatures(TEST_GENE_PREFIX.toLowerCase(), geneFile, 10),
                             Collections.singletonList(IllegalArgumentException.class));
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
