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

package com.epam.catgenome.dao;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.bed.BedFileDao;
import com.epam.catgenome.dao.reference.BookmarkDao;
import com.epam.catgenome.dao.seg.SegFileDao;
import com.epam.catgenome.dao.vcf.VcfFileDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.bed.BedFile;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.project.ProjectItem;
import com.epam.catgenome.entity.reference.Bookmark;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.seg.SegFile;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.project.ProjectManager;
import com.epam.catgenome.util.AuthUtils;

/**
 * Source:      BookmarkDaoTest
 * Created:     20.04.16, 18:00
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BookmarkDaoTest extends AbstractDaoTest {
    private static final int BOOKMARK_END_INDEX = 10000;

    @Autowired
    private BookmarkDao bookmarkDao;

    @Autowired
    private VcfFileDao vcfFileDao;

    @Autowired
    private BedFileDao bedFileDao;

    @Autowired
    private SegFileDao segFileDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private ProjectManager projectManager;

    private Chromosome chromosome;

    private static final String TEST_PATH = "///";
    private static final String TEST_INDEX_PATH = "////";
    private static final String TEST_FILE_NAME = "testFile";
    private static final String TEST_NAME = "test";
    private static final int TEST_SEARCH_LIMIT = 100;

    @Before
    public void setup() throws Exception {
        super.setup();
        chromosome = EntityHelper.createNewChromosome();
        referenceGenomeDao.saveChromosomes(reference.getId(), Collections.singletonList(chromosome));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testSaveLoadBookmark() throws IOException, InterruptedException, FeatureIndexException {
        VcfFile vcfFile = new VcfFile();

        vcfFile.setId(vcfFileDao.createVcfFileId());
        vcfFile.setName(TEST_PATH);
        vcfFile.setCreatedBy(AuthUtils.getCurrentUserId());
        vcfFile.setType(BiologicalDataItemResourceType.FILE);
        vcfFile.setPath(TEST_FILE_NAME);
        vcfFile.setCreatedDate(new Date());
        vcfFile.setReferenceId(reference.getId());

        BiologicalDataItem index = EntityHelper.createIndex(BiologicalDataItemFormat.VCF_INDEX,
                BiologicalDataItemResourceType.FILE, TEST_INDEX_PATH);
        vcfFile.setIndex(index);

        biologicalDataItemDao.createBiologicalDataItem(index);
        final Long realId = vcfFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(vcfFile);
        vcfFileDao.createVcfFile(vcfFile, realId);

        Project project = new Project();
        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(reference.getBioDataItemId())),
                new ProjectItem(vcfFile)));
        project.setName(TEST_NAME);
        project.setCreatedBy(AuthUtils.getCurrentUserId());
        project.setCreatedDate(new Date());

        projectManager.saveProject(project);

        Bookmark bookmark = new Bookmark();
        bookmark.setName(TEST_NAME);
        bookmark.setEndIndex(BOOKMARK_END_INDEX);
        bookmark.setStartIndex(0);
        bookmark.setChromosome(chromosome);
        bookmark.setOpenedItems(Collections.singletonList(vcfFile));
        bookmark.setCreatedDate(new Date());
        bookmark.setCreatedBy(AuthUtils.getCurrentUserId());

        bookmarkDao.saveBookmark(bookmark);
        bookmarkDao.insertBookmarkItems(bookmark.getOpenedItems(), bookmark.getId());

        List<Bookmark> loadedBookmarks = bookmarkDao.loadAllBookmarks(AuthUtils.getCurrentUserId());
        Assert.assertNotNull(loadedBookmarks);
        Assert.assertFalse(loadedBookmarks.isEmpty());

        Bookmark loadedBookmark = bookmarkDao.loadBookmarkById(loadedBookmarks.get(0).getId());
        Assert.assertNotNull(loadedBookmark);
        Assert.assertEquals(loadedBookmark.getId(), bookmark.getId());

        Map<Long, List<BiologicalDataItem>> loadedItemsMap = bookmarkDao.loadBookmarkItemsByBookmarkIds(
                Collections.singletonList(loadedBookmark.getId()));

        List<BiologicalDataItem> loadedItems = loadedItemsMap.get(loadedBookmark.getId());
        Assert.assertNotNull(loadedItems);
        Assert.assertFalse(loadedItems.isEmpty());
        Assert.assertEquals(loadedItems.get(0).getId(), vcfFile.getId());

        bookmarkDao.deleteBookmarkItems(bookmark.getId());
        loadedItemsMap = bookmarkDao.loadBookmarkItemsByBookmarkIds(Collections.singletonList(loadedBookmark.getId()));
        Assert.assertTrue(loadedItemsMap.isEmpty());

        bookmarkDao.deleteBookmark(bookmark.getId());
        loadedBookmark = bookmarkDao.loadBookmarkById(bookmark.getId());
        Assert.assertNull(loadedBookmark);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testAllItemTypes() throws IOException, InterruptedException, FeatureIndexException {
        // Add vcfFile
        VcfFile vcfFile = new VcfFile();

        vcfFile.setId(vcfFileDao.createVcfFileId());
        vcfFile.setName(TEST_FILE_NAME + BiologicalDataItemFormat.VCF.name());
        vcfFile.setCreatedBy(AuthUtils.getCurrentUserId());
        vcfFile.setType(BiologicalDataItemResourceType.FILE);
        vcfFile.setPath(TEST_PATH);
        vcfFile.setCreatedDate(new Date());
        vcfFile.setReferenceId(reference.getId());

        BiologicalDataItem vcfIndex = EntityHelper.createIndex(BiologicalDataItemFormat.VCF_INDEX,
                BiologicalDataItemResourceType.FILE, TEST_INDEX_PATH);
        vcfFile.setIndex(vcfIndex);

        biologicalDataItemDao.createBiologicalDataItem(vcfIndex);
        final Long vcfRealId = vcfFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(vcfFile);
        vcfFileDao.createVcfFile(vcfFile, vcfRealId);

        // Add BED file

        BedFile bedFile = new BedFile();

        bedFile.setId(bedFileDao.createBedFileId());
        bedFile.setName(TEST_FILE_NAME + BiologicalDataItemFormat.BED.name());
        bedFile.setCreatedBy(AuthUtils.getCurrentUserId());
        bedFile.setType(BiologicalDataItemResourceType.FILE);
        bedFile.setPath(TEST_PATH);
        bedFile.setCreatedDate(new Date());
        bedFile.setReferenceId(reference.getId());

        BiologicalDataItem bedIndex = EntityHelper.createIndex(BiologicalDataItemFormat.BED_INDEX,
                BiologicalDataItemResourceType.FILE, TEST_INDEX_PATH);
        bedFile.setIndex(bedIndex);

        biologicalDataItemDao.createBiologicalDataItem(bedIndex);
        final Long bedRealId = bedFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(bedFile);
        bedFileDao.createBedFile(bedFile, bedRealId);

        // Add SEG file

        SegFile segFile = new SegFile();

        segFile.setId(segFileDao.createSegFileId());
        segFile.setName(TEST_FILE_NAME + BiologicalDataItemFormat.SEG.name());
        segFile.setCreatedBy(AuthUtils.getCurrentUserId());
        segFile.setType(BiologicalDataItemResourceType.FILE);
        segFile.setPath(TEST_PATH);
        segFile.setCreatedDate(new Date());
        segFile.setReferenceId(reference.getId());

        BiologicalDataItem segIndex = EntityHelper.createIndex(BiologicalDataItemFormat.SEG_INDEX,
                BiologicalDataItemResourceType.FILE, TEST_INDEX_PATH);
        segFile.setIndex(segIndex);

        biologicalDataItemDao.createBiologicalDataItem(segIndex);
        final Long segRealId = segFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(segFile);
        segFileDao.createSegFile(segFile, segRealId);

        // Create a project
        Project project = new Project();
        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(reference.getBioDataItemId())),
                new ProjectItem(vcfFile), new ProjectItem(bedFile)));
        project.setName(TEST_NAME);
        project.setCreatedBy(AuthUtils.getCurrentUserId());
        project.setCreatedDate(new Date());

        projectManager.saveProject(project);

        // create a bookmark
        Bookmark bookmark = new Bookmark();
        bookmark.setName(TEST_NAME);
        bookmark.setEndIndex(BOOKMARK_END_INDEX);
        bookmark.setStartIndex(0);
        bookmark.setCreatedDate(new Date());
        bookmark.setCreatedBy(AuthUtils.getCurrentUserId());
        bookmark.setChromosome(chromosome);
        bookmark.setOpenedItems(Arrays.asList(vcfFile, bedFile, segFile));

        bookmarkDao.saveBookmark(bookmark);
        bookmarkDao.insertBookmarkItems(bookmark.getOpenedItems(), bookmark.getId());

        Map<Long, List<BiologicalDataItem>> loadedItemsMap = bookmarkDao.loadBookmarkItemsByBookmarkIds(
                Collections.singletonList(bookmark.getId()));

        List<BiologicalDataItem> loadedItems = loadedItemsMap.get(bookmark.getId());
        Assert.assertNotNull(loadedItems);
        Assert.assertFalse(loadedItems.isEmpty());
        Assert.assertEquals(loadedItems.get(0).getId(), vcfFile.getId());
        Assert.assertEquals(loadedItems.get(1).getId(), bedFile.getId());

        // Test VCF item
        BiologicalDataItem vcfItem = loadedItems.stream().filter(i -> i.getFormat() ==
                BiologicalDataItemFormat.VCF).findFirst().get();
        Assert.assertNotNull(vcfItem);
        Assert.assertNotNull(vcfItem);
        VcfFile loadedVcf = (VcfFile) vcfItem;
        Assert.assertNotNull(loadedVcf.getId());
        Assert.assertNotNull(loadedVcf.getIndex());
        Assert.assertFalse(loadedVcf.getIndex().getPath().isEmpty());
        Assert.assertFalse(loadedVcf.getPath().isEmpty());

        // Test BED item
        BiologicalDataItem bedItem = loadedItems.stream().filter(i -> i.getFormat() ==
                BiologicalDataItemFormat.BED).findFirst().get();
        Assert.assertNotNull(bedItem);
        Assert.assertNotNull(bedItem);
        BedFile loadedBed = (BedFile) bedItem;
        Assert.assertNotNull(loadedBed.getId());
        Assert.assertNotNull(loadedBed.getIndex());
        Assert.assertFalse(loadedBed.getIndex().getPath().isEmpty());
        Assert.assertFalse(loadedBed.getPath().isEmpty());

        // Test SEG Files
        BiologicalDataItem segItem = loadedItems.stream().filter(i -> i.getFormat() ==
                BiologicalDataItemFormat.SEG).findFirst().get();
        Assert.assertNotNull(segItem);
        Assert.assertNotNull(segItem);
        SegFile loadedSeg = (SegFile) segItem;
        Assert.assertNotNull(loadedSeg.getId());
        Assert.assertNotNull(loadedSeg.getIndex());
        Assert.assertFalse(loadedSeg.getIndex().getPath().isEmpty());
        Assert.assertFalse(loadedSeg.getPath().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testSearch() {
        Bookmark bookmark = new Bookmark();
        bookmark.setName(TEST_NAME);
        bookmark.setEndIndex(BOOKMARK_END_INDEX);
        bookmark.setStartIndex(0);
        bookmark.setChromosome(chromosome);
        bookmark.setCreatedDate(new Date());
        bookmark.setCreatedBy(AuthUtils.getCurrentUserId());

        bookmarkDao.saveBookmark(bookmark);

        List<Bookmark> bookmarks = bookmarkDao.searchBookmarks(TEST_NAME, AuthUtils.getCurrentUserId(),
                                                               TEST_SEARCH_LIMIT);
        Assert.assertFalse(bookmarks.isEmpty());
        Assert.assertEquals(bookmark.getId(), bookmarks.get(0).getId());
        Assert.assertEquals(bookmark.getName(), bookmarks.get(0).getName());
        Assert.assertEquals(bookmark.getStartIndex(), bookmarks.get(0).getStartIndex());
        Assert.assertEquals(bookmark.getChromosome().getId(), bookmarks.get(0).getChromosome().getId());
        Assert.assertEquals(bookmark.getCreatedBy(), bookmarks.get(0).getCreatedBy());
        Assert.assertEquals(bookmark.getCreatedDate(), bookmarks.get(0).getCreatedDate());
    }
}
