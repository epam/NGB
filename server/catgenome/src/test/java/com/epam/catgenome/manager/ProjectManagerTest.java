/*
 * MIT License
 *
 * Copyright (c) 2016-2022 EPAM Systems
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
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.bed.BedFile;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.maf.MafFile;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.project.ProjectItem;
import com.epam.catgenome.entity.reference.Bookmark;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.seg.SegFile;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.bed.BedManager;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.maf.MafManager;
import com.epam.catgenome.manager.project.ProjectManager;
import com.epam.catgenome.manager.reference.BookmarkManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.seg.SegManager;
import com.epam.catgenome.manager.vcf.VcfManager;

/**
 * Source:      ProjectManagerTest
 * Created:     11.01.16, 14:21
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class ProjectManagerTest extends AbstractManagerTest {
    public static final String TEST_PARENT = "testParent";
    public static final String TEST_CHILD = "testChild1";
    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private VcfManager vcfManager;

    @Autowired
    private GffManager gffManager;

    @Autowired
    private BedManager bedManager;

    @Autowired
    private SegManager segManager;

    @Autowired
    private BookmarkManager bookmarkManager;

    @Autowired
    private MafManager mafManager;

    private final Logger logger = LoggerFactory.getLogger(ProjectManagerTest.class);

    @Autowired
    private ApplicationContext context;

    private long referenceId;
    private Reference testReference;
    private Chromosome testChromosome;
    private static final String TEST_VCF_FILE_PATH = "classpath:templates/samples.vcf";
    private static final String TEST_PROJECT_NAME = "testProject";
    private static final String TEST_VCF_FILE_NAME1 = "file1";
    private static final String TEST_VCF_FILE_NAME2 = "file2";
    private static final int BOOKMARK_END_INDEX = 10000;

    @Value("#{catgenome['files.base.directory.path']}")
    private String baseDirPath;

    @Before
    public void setup() throws Exception {
        testChromosome = EntityHelper.createNewChromosome();
        testReference = EntityHelper.createNewReference(testChromosome, referenceGenomeManager.createReferenceId());

        referenceGenomeManager.create(testReference);
        referenceId = testReference.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveLoadDeleteProject()
        throws IOException, InterruptedException, NoSuchAlgorithmException {
        VcfFile file = addVcfFile(TEST_VCF_FILE_NAME1, TEST_VCF_FILE_PATH);
        BiologicalDataItem item = new BiologicalDataItem();
        item.setId(file.getBioDataItemId());

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId())),
                new ProjectItem(item)));

        projectManager.create(project);

        Project loadedProject = projectManager.load(project.getId());
        Assert.assertNotNull(loadedProject);
        Assert.assertFalse(loadedProject.getItems().isEmpty());

        // load my projects
        List<Project> myProjects = projectManager.loadTopLevelProjects();
        Assert.assertTrue(myProjects.stream().allMatch(p -> !p.getItems().isEmpty()));

        VcfFile file2 = addVcfFile(TEST_VCF_FILE_NAME2, TEST_VCF_FILE_PATH);
        BiologicalDataItem item2 = new BiologicalDataItem();
        item2.setId(file2.getBioDataItemId());
        loadedProject.getItems().add(new ProjectItem(item2));

        projectManager.create(loadedProject);

        loadedProject = projectManager.load(project.getId());
        Assert.assertEquals(loadedProject.getItems().size(), 3);

        loadedProject.getItems().remove(2);
        projectManager.create(loadedProject);

        loadedProject = projectManager.load(project.getId());
        Assert.assertEquals(loadedProject.getItems().size(), 2);
        Assert.assertEquals(TEST_VCF_FILE_NAME1, loadedProject.getItems().get(1).getBioDataItem().getName());

        loadedProject.getItems().get(0).setHidden(true);
        projectManager.create(loadedProject);

        loadedProject = projectManager.load(project.getId());
        Assert.assertEquals(loadedProject.getItems().size(), 2);
        Assert.assertTrue(loadedProject.getItems().get(0).getHidden());

        // add a bookmark
        Bookmark bookmark = new Bookmark();
        bookmark.setOpenedItems(Collections.singletonList(item));
        bookmark.setStartIndex(1);
        bookmark.setEndIndex(BOOKMARK_END_INDEX);
        bookmark.setChromosome(testChromosome);
        bookmark.setName("testBookmark");

        bookmarkManager.create(bookmark);

        List<Bookmark> loadedBookmarks = bookmarkManager.loadAllBookmarks();

        Assert.assertNotNull(loadedBookmarks);
        Assert.assertFalse(loadedBookmarks.isEmpty());

        Bookmark loadedBookmark = loadedBookmarks.get(0);

        loadedBookmark = bookmarkManager.load(loadedBookmark.getId());
        Assert.assertFalse(loadedBookmark.getOpenedItems().isEmpty());
        Assert.assertEquals(BiologicalDataItem.getBioDataItemId(loadedBookmark.getOpenedItems()
                .get(0)), item.getId());

        // Now delete project
        projectManager.delete(project.getId(), false);
        try {
            projectManager.load(project.getId());
            Assert.fail("No exception happened, but should happen");
        } catch (IllegalArgumentException e) {
            // success, nothing to do here
            logger.debug("Deleted successfully, nothing to do here");
        }

        File dir = new File(baseDirPath + "/projects/" + project.getId());
        Assert.assertFalse(dir.exists());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveLoadProjectWithPrettyName() throws IOException, InterruptedException,
            NoSuchAlgorithmException {
        VcfFile file = addVcfFile(TEST_VCF_FILE_NAME1, TEST_VCF_FILE_PATH);
        BiologicalDataItem item = new BiologicalDataItem();
        item.setId(file.getBioDataItemId());

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
        project.setPrettyName("pretty");
        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId())),
                new ProjectItem(item)));

        projectManager.create(project);

        Project loadedProject = projectManager.load(project.getId());
        Assert.assertNotNull(loadedProject);
        Assert.assertEquals("pretty", loadedProject.getPrettyName());
        Assert.assertFalse(loadedProject.getItems().isEmpty());
    }


    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testDeleteProjectWithNested() throws IOException {
        Project parent = new Project();
        parent.setName(TEST_PARENT);
        parent.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        parent = projectManager.create(parent);

        Project child1 = new Project();
        child1.setName(TEST_CHILD);
        child1.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        child1 = projectManager.create(child1, parent.getId());

        Project child2 = new Project();
        child2.setName("testChild2");
        child2.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        child2 = projectManager.create(child2, parent.getId());

        Project child11 = new Project();
        child11.setName("tesChild11");
        child11.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        projectManager.create(child11, child1.getId());

        List<Project> topProjects = projectManager.loadProjectTree(null, null);
        Assert.assertEquals(1, topProjects.size());

        Project root = topProjects.get(0);
        Assert.assertEquals(2, root.getNestedProjects().size());

        // check that we cannot delete parent dataset without force option
        boolean catchException = false;
        try {
            projectManager.delete(root.getId(), false);
        } catch (IllegalArgumentException e) {
            catchException = true;
        }
        if (!catchException) {
            Assert.fail("Parent project cannot be deleted without force option");
        }

        // check that we can delete parent dataset with force option
        projectManager.delete(root.getId(), true);
        assertNullLoadProjectWithNested(root);
    }

    private void assertNullLoadProjectWithNested(Project root) {
        try {
            // check that we handle exception as expected
            Assert.assertNull(projectManager.load(root.getId()));
        } catch (IllegalArgumentException e) {
            // continue checking with child projects
            Optional.ofNullable(root.getNestedProjects())
                    .orElse(Collections.emptyList())
                    .forEach(this::assertNullLoadProjectWithNested);
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testAddRemoveHideItems()
        throws IOException, InterruptedException, NoSuchAlgorithmException, FeatureIndexException {
        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
        project.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        projectManager.create(project);

        Project loadedProject = projectManager.load(project.getId());
        Assert.assertNotNull(loadedProject);
        Assert.assertEquals(1, loadedProject.getItems().size());

        addVcfFileToProject(project.getId(), TEST_VCF_FILE_NAME1, TEST_VCF_FILE_PATH);
        addVcfFileToProject(project.getId(), TEST_VCF_FILE_NAME2, TEST_VCF_FILE_PATH);

        loadedProject = projectManager.load(project.getId());
        Assert.assertEquals(3, loadedProject.getItems().size());

        projectManager.removeProjectItem(project.getId(),
                ((VcfFile) loadedProject.getItems().get(1).getBioDataItem()).getBioDataItemId());
        loadedProject = projectManager.load(project.getId());
        Assert.assertEquals(2, loadedProject.getItems().size());
        Assert.assertEquals(TEST_VCF_FILE_NAME2, loadedProject.getItems().get(1).getBioDataItem().getName());

        projectManager.hideProjectItem(project.getId(),
                ((VcfFile) loadedProject.getItems().get(1).getBioDataItem()).getBioDataItemId());
        loadedProject = projectManager.load(project.getId());
        Assert.assertFalse(loadedProject.getItems().isEmpty());
        Assert.assertTrue(loadedProject.getItems().get(1).getHidden());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testMoveProjectToParent()
        throws InterruptedException,
            NoSuchAlgorithmException, IOException {
        Project parent = new Project();
        parent.setName(TEST_PARENT);
        parent.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        parent = projectManager.create(parent);

        Project child1 = new Project();
        child1.setName(TEST_CHILD);
        child1.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        child1 = projectManager.create(child1, parent.getId());

        parent = projectManager.load(parent.getId());
        Assert.assertFalse(parent.getNestedProjects().isEmpty());
        Assert.assertEquals(child1.getId(), parent.getNestedProjects().get(0).getId());

        Project child2 = new Project();
        child2.setName("testChild2");
        child2.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        child2 = projectManager.create(child2);
        projectManager.moveProjectToParent(child2.getId(), parent.getId());
        parent = projectManager.load(parent.getId());

        Assert.assertEquals(parent.getNestedProjects().size(), 2);
        Assert.assertEquals(child1.getId(), parent.getNestedProjects().get(0).getId());
        Assert.assertEquals(child2.getId(), parent.getNestedProjects().get(1).getId());

        parent = projectManager.load(parent.getId());
        Assert.assertEquals(parent.getNestedProjects().size(), 2);
        Assert.assertEquals(child1.getId(), parent.getNestedProjects().get(0).getId());
        Assert.assertEquals(child2.getId(), parent.getNestedProjects().get(1).getId());

        List<Project> topLevel = projectManager.loadTopLevelProjects();
        Assert.assertEquals(1, topLevel.size());

        // test loading tree
        Project child11 = new Project();
        child11.setName("tesChild11");
        child11.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        projectManager.create(child11, child1.getId());

        addVcfFileToProject(parent.getId(), "testVcf", TEST_VCF_FILE_PATH);

        Resource resource = context.getResource("classpath:templates/genes_sorted.gtf");

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setName("genes");
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(request);
        projectManager.addProjectItem(child1.getId(), geneFile.getBioDataItemId());

        referenceGenomeManager.updateReferenceGeneFileId(referenceId, geneFile.getId());
        projectManager.addProjectItem(parent.getId(), geneFile.getBioDataItemId());

        topLevel = projectManager.loadProjectTree(null, null);
        Assert.assertFalse(topLevel.isEmpty());
        Assert.assertFalse(topLevel.stream().anyMatch(p -> p.getNestedProjects().isEmpty()));
        Assert.assertTrue(topLevel.stream()
                              .anyMatch(p -> p.getItems()
                                  .stream()
                                  .anyMatch(i -> i.getBioDataItem().getFormat() == BiologicalDataItemFormat.GENE)));
        Assert.assertFalse(topLevel.stream().anyMatch(p -> p.getItems().isEmpty()));
        Assert.assertFalse(topLevel.get(0).getNestedProjects().stream()
                               .allMatch(p -> CollectionUtils.isEmpty(p.getNestedProjects())));
        Assert.assertFalse(topLevel.get(0).getNestedProjects().stream()
                               .allMatch(p -> CollectionUtils.isEmpty(p.getItems())));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadTopLevelProjects() throws IOException {
        Project project1 = new Project();
        project1.setName(TEST_PARENT);
        project1.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        project1 = projectManager.create(project1);
        Long project1Id = project1.getId();

        Project project2 = new Project();
        project2.setName(TEST_CHILD);
        project2.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        project2 = projectManager.create(project2);
        Long project2Id = project2.getId();
        addVcfFileToProject(project2Id, "testVcf", TEST_VCF_FILE_PATH);
        Resource resource = context.getResource("classpath:templates/genes_sorted.gtf");
        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setName("genes");
        request.setPath(resource.getFile().getAbsolutePath());
        GeneFile geneFile = gffManager.registerGeneFile(request);
        projectManager.addProjectItem(project2Id, geneFile.getBioDataItemId());

        Project project3 = new Project();
        project3.setName("testChild2");
        project3.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        project3 = projectManager.create(project3);
        Long project3Id = project3.getId();
        addVcfFileToProject(project3.getId(), "testVcf2", TEST_VCF_FILE_PATH);
        addVcfFileToProject(project3.getId(), "testVcf3", TEST_VCF_FILE_PATH);


        List<Project> topLevel = projectManager.loadTopLevelProjects();
        Assert.assertEquals(3, topLevel.size());

        final Project loadedProject1 = topLevel.stream()
                .filter(project -> Objects.equals(project.getId(), project1Id)).findFirst().get();
        Assert.assertEquals(0, (int) loadedProject1.getItemsCount());
        Assert.assertTrue(loadedProject1.getItemsCountPerFormat().isEmpty());
        Assert.assertEquals(1, loadedProject1.getItems().size());
        Assert.assertEquals(testReference.getId(), loadedProject1.getItems().get(0).getBioDataItem().getId());

        final Project loadedProject2 = topLevel.stream()
                .filter(project -> Objects.equals(project.getId(), project2Id)).findFirst().get();
        Assert.assertEquals(2, (int) loadedProject2.getItemsCount());
        Assert.assertNotNull(loadedProject2.getItemsCountPerFormat());
        Assert.assertEquals(1, (int) loadedProject2.getItemsCountPerFormat().get(BiologicalDataItemFormat.VCF));
        Assert.assertEquals(1, (int) loadedProject2.getItemsCountPerFormat().get(BiologicalDataItemFormat.GENE));
        Assert.assertEquals(1, loadedProject2.getItems().size());
        Assert.assertEquals(testReference.getId(), loadedProject2.getItems().get(0).getBioDataItem().getId());

        final Project loadedProject3 = topLevel.stream()
                .filter(project -> Objects.equals(project.getId(), project3Id)).findFirst().get();
        Assert.assertEquals(2, (int) loadedProject3.getItemsCount());
        Assert.assertNotNull(loadedProject3.getItemsCountPerFormat());
        Assert.assertEquals(2, (int) loadedProject3.getItemsCountPerFormat().get(BiologicalDataItemFormat.VCF));
        Assert.assertEquals(1, loadedProject3.getItems().size());
        Assert.assertEquals(testReference.getId(), loadedProject3.getItems().get(0).getBioDataItem().getId());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadTreeWithParent() {
        Project parent = new Project();
        parent.setName(TEST_PARENT);
        parent.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        parent = projectManager.create(parent);

        Project child1 = new Project();
        child1.setName(TEST_CHILD);
        child1.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        child1 = projectManager.create(child1, parent.getId());

        Project child2 = new Project();
        child2.setName("testChild2");
        child2.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        child2 = projectManager.create(child2, parent.getId());

        Project child11 = new Project();
        child11.setName("tesChild11");
        child11.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        projectManager.create(child11, child1.getId());

        List<Project> topProjects = projectManager.loadProjectTree(null, null);
        Assert.assertEquals(1, topProjects.size());
        Assert.assertEquals(2, topProjects.get(0).getNestedProjects().size());

        List<Project> childProjects = projectManager.loadProjectTree(child1.getId(), null);
        Assert.assertEquals(1, childProjects.size());
        Assert.assertEquals(1, childProjects.get(0).getNestedProjects().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testAllFileTypesLoading()
        throws IOException, InterruptedException, NoSuchAlgorithmException {
        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
        project.setItems(Collections.singletonList(
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));
        projectManager.create(project);

        Project loadedProject = projectManager.load(project.getId());
        Assert.assertNotNull(loadedProject);
        Assert.assertEquals(1, loadedProject.getItems().size());

        // Add Vcf
        addVcfFileToProject(project.getId(), TEST_VCF_FILE_NAME1, TEST_VCF_FILE_PATH);

        // Add genes
        Resource resource = context.getResource("classpath:templates/genes_sorted.gtf");

        FeatureIndexedFileRegistrationRequest geneRequest = new FeatureIndexedFileRegistrationRequest();
        geneRequest.setReferenceId(referenceId);
        geneRequest.setName("genes");
        geneRequest.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(geneRequest);
        projectManager.addProjectItem(project.getId(), geneFile.getBioDataItemId());

        // Add BED file
        resource = context.getResource("classpath:templates/genes_sorted.bed");

        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setName("bed");
        request.setPath(resource.getFile().getAbsolutePath());

        BedFile bedFile = bedManager.registerBed(request);
        projectManager.addProjectItem(project.getId(), bedFile.getBioDataItemId());


        // Add SEG file
        resource = context.getResource("classpath:templates/test_seg.seg");

        request = new IndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setName("seg");
        request.setPath(resource.getFile().getAbsolutePath());

        SegFile segFile = segManager.registerSegFile(request);
        projectManager.addProjectItem(project.getId(), segFile.getBioDataItemId());

        // Add MAF file
        resource = context.getResource("classpath:templates/maf/" +
                "TCGA.ACC.mutect.abbe72a5-cb39-48e4-8df5-5fd2349f2bb2.somatic.sorted.maf.gz");

        request = new IndexedFileRegistrationRequest();
        request.setPath(resource.getFile().getAbsolutePath());
        request.setReferenceId(referenceId);

        MafFile mafFile = mafManager.registerMafFile(request);
        Assert.assertNotNull(mafFile);
        projectManager.addProjectItem(project.getId(), mafFile.getBioDataItemId());

        loadedProject = projectManager.load(project.getId());

        // Test VCF item
        ProjectItem vcfItem = loadedProject.getItems().stream().filter(i -> i.getBioDataItem().getFormat() ==
                BiologicalDataItemFormat.VCF).findFirst().get();
        Assert.assertNotNull(vcfItem);
        Assert.assertNotNull(vcfItem.getBioDataItem());
        VcfFile loadedVcf = (VcfFile) vcfItem.getBioDataItem();
        Assert.assertNotNull(loadedVcf.getId());
        Assert.assertNotNull(loadedVcf.getIndex());
        Assert.assertFalse(loadedVcf.getIndex().getPath().isEmpty());
        Assert.assertFalse(loadedVcf.getPath().isEmpty());
        Assert.assertFalse(loadedVcf.getSamples().isEmpty());

        // Test BED item
        ProjectItem bedItem = loadedProject.getItems().stream().filter(i -> i.getBioDataItem().getFormat() ==
                BiologicalDataItemFormat.BED).findFirst().get();
        Assert.assertNotNull(bedItem);
        Assert.assertNotNull(bedItem.getBioDataItem());
        BedFile loadedBed = (BedFile) bedItem.getBioDataItem();
        Assert.assertNotNull(loadedBed.getId());
        Assert.assertNotNull(loadedBed.getIndex());
        Assert.assertFalse(loadedBed.getIndex().getPath().isEmpty());
        Assert.assertFalse(loadedBed.getPath().isEmpty());

        // Test SEG Files
        ProjectItem segItem = loadedProject.getItems().stream().filter(i -> i.getBioDataItem().getFormat() ==
                BiologicalDataItemFormat.SEG).findFirst().get();
        Assert.assertNotNull(segItem);
        Assert.assertNotNull(segItem.getBioDataItem());
        SegFile loadedSeg = (SegFile) segItem.getBioDataItem();
        Assert.assertNotNull(loadedSeg.getId());
        Assert.assertNotNull(loadedSeg.getIndex());
        Assert.assertFalse(loadedSeg.getPath().isEmpty());
        Assert.assertFalse(loadedSeg.getSamples().isEmpty());

        // Test MAF Files
        ProjectItem mafItem = loadedProject.getItems().stream().filter(i -> i.getBioDataItem().getFormat() ==
                BiologicalDataItemFormat.MAF).findFirst().get();
        Assert.assertNotNull(mafItem);
        Assert.assertNotNull(mafItem.getBioDataItem());
        SegFile loadedMaf = (SegFile) segItem.getBioDataItem();
        Assert.assertNotNull(loadedMaf.getId());
        Assert.assertNotNull(loadedMaf.getIndex());
        Assert.assertFalse(loadedMaf.getPath().isEmpty());
        Assert.assertFalse(loadedMaf.getSamples().isEmpty());

        // Test load my projects
        List<Project> myProjects = projectManager.loadTopLevelProjects();
        Assert.assertFalse(myProjects.isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    public void testLoadReferenceGenes() throws IOException {
        Resource resource = context.getResource("classpath:templates/genes_sorted.gtf");

        FeatureIndexedFileRegistrationRequest geneRequest = new FeatureIndexedFileRegistrationRequest();
        geneRequest.setReferenceId(referenceId);
        geneRequest.setName("genes");
        geneRequest.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(geneRequest);

        referenceGenomeManager.updateReferenceGeneFileId(referenceId, geneFile.getId());

        geneRequest.setName("genes1");
        GeneFile geneFile1 = gffManager.registerGeneFile(geneRequest);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId())),
                new ProjectItem(geneFile1)));

        projectManager.create(project);

        Project loadedProject = projectManager.load(project.getId());
        Reference projectReference =
            (Reference) loadedProject.getItems().stream()
                .filter(i -> i.getBioDataItem().getFormat() == BiologicalDataItemFormat.REFERENCE)
                .map(ProjectItem::getBioDataItem)
                .findFirst().get();

        GeneFile projectGeneFile =
            (GeneFile) loadedProject.getItems().stream()
                .filter(i -> i.getBioDataItem().getId().equals(geneFile1.getId()))
                .map(ProjectItem::getBioDataItem)
                .findFirst().get();

        Assert.assertEquals(projectGeneFile.getPath(), projectReference.getGeneFile().getPath());
        Assert.assertEquals(projectGeneFile.getFormat(), projectReference.getGeneFile().getFormat());
        Assert.assertEquals(projectGeneFile.getType(), projectReference.getGeneFile().getType());
        Assert.assertEquals(projectGeneFile.getReferenceId(), projectReference.getGeneFile().getReferenceId());
        Assert.assertNotNull(projectReference.getGeneFile().getId());
        Assert.assertNotNull(projectReference.getGeneFile().getName());
        Assert.assertNotNull(projectReference.getGeneFile().getCreatedDate());
    }

    private void addVcfFileToProject(long projectId, String name, String path) throws IOException {
        VcfFile vcfFile = addVcfFile(name, path);
        projectManager.addProjectItem(projectId, vcfFile.getBioDataItemId());
    }

    private VcfFile addVcfFile(String name, String path)
        throws IOException {
        Resource resource = context.getResource(path);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setName(name);
        request.setPath(resource.getFile().getAbsolutePath());

        return vcfManager.registerVcfFile(request);
    }
}
