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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.epam.catgenome.dao.gene.GeneFileDao;
import com.epam.catgenome.helper.EntityHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.reference.ReferenceGenomeDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.manager.gene.GeneFileManager;

/**
 * Source:      BiologicalDataItemDaoTest
 * Created:     17.12.15, 14:05
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BiologicalDataItemDaoTest extends AbstractTransactionalJUnit4SpringContextTests {
    private static final String TEST_OWNER = "TEST_USER";
    public static final String TEST_GENE_FILE_NAME = "testGeneFile";
    public static final String TEST_GENE_FILE_NAME_2 = "testGeneFile2";

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private ReferenceGenomeDao referenceGenomeDao;

    @Autowired
    private GeneFileManager geneFileManager;

    @Autowired
    private GeneFileDao geneFileDao;

    private static final String TEST_NAME = "test1";
    private static final String TEST_PATH = "///";
    private static final List<String> MATCHING_NAMES = new ArrayList<String>(){{
            add("test1");
            add("est");
            add("TEST");
        }};
    private static final List<String> NOT_MATCHING_NAMES = new ArrayList<String>(){{
            add("tst11");
            add("tsT");
            add("SET");
        }};

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveAndLoadItems() {
        BiologicalDataItem item = new BiologicalDataItem();
        item.setName(TEST_NAME);
        item.setPath(TEST_PATH);
        item.setSource(TEST_PATH);
        item.setFormat(BiologicalDataItemFormat.REFERENCE);
        item.setType(BiologicalDataItemResourceType.FILE);
        item.setCreatedDate(new Date());
        item.setOwner(TEST_OWNER);

        biologicalDataItemDao.createBiologicalDataItem(item);

        Reference reference = createTestReference(item);
        GeneFile geneFile = createTestGeneFile(reference, TEST_GENE_FILE_NAME);
        referenceGenomeDao.updateReferenceGeneFileId(reference.getId(), geneFile.getId());

        List<BiologicalDataItem> loadedItems = biologicalDataItemDao.loadBiologicalDataItemsByIds(Collections
                .singletonList(item.getId()));

        Assert.assertFalse(loadedItems.isEmpty());

        BiologicalDataItem loadedItem = loadedItems.get(0);
        Assert.assertEquals(item.getId(), BiologicalDataItem.getBioDataItemId(loadedItem));
        Assert.assertEquals(item.getName(), loadedItem.getName());
        Assert.assertEquals(item.getPath(), loadedItem.getPath());
        Assert.assertEquals(item.getSource(), loadedItem.getPath());
        Assert.assertEquals(item.getCreatedDate(), loadedItem.getCreatedDate());
        Assert.assertEquals(item.getType(), loadedItem.getType());
        Assert.assertEquals(item.getFormat(), loadedItem.getFormat());
        Assert.assertEquals(item.getOwner(), loadedItem.getOwner());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSearchStrict() {
        BiologicalDataItem item = new BiologicalDataItem();
        item.setName(TEST_NAME);
        item.setPath(TEST_PATH);
        item.setSource(TEST_PATH);
        item.setFormat(BiologicalDataItemFormat.REFERENCE);
        item.setType(BiologicalDataItemResourceType.FILE);
        item.setCreatedDate(new Date());
        item.setOwner(TEST_OWNER);

        biologicalDataItemDao.createBiologicalDataItem(item);

        Reference reference = createTestReference(item);
        GeneFile geneFile = createTestGeneFile(reference, TEST_GENE_FILE_NAME);
        referenceGenomeDao.updateReferenceGeneFileId(reference.getId(), geneFile.getId());

        List<BiologicalDataItem> loadedItems = biologicalDataItemDao.loadFilesByNameStrict(TEST_NAME);

        Assert.assertFalse(loadedItems.isEmpty());

        BiologicalDataItem loadedItem = loadedItems.get(0);
        Assert.assertEquals(item.getId(), BiologicalDataItem.getBioDataItemId(loadedItem));
        Assert.assertEquals(item.getName(), loadedItem.getName());
        Assert.assertEquals(item.getPath(), loadedItem.getPath());
        Assert.assertEquals(item.getSource(), loadedItem.getPath());
        Assert.assertEquals(item.getCreatedDate(), loadedItem.getCreatedDate());
        Assert.assertEquals(item.getType(), loadedItem.getType());
        Assert.assertEquals(item.getFormat(), loadedItem.getFormat());

        List<BiologicalDataItem> empty = biologicalDataItemDao.loadFilesByNameStrict(TEST_NAME + "/");
        Assert.assertTrue(empty.isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSearchNotStrict() {
        BiologicalDataItem item = new BiologicalDataItem();
        item.setName(TEST_NAME);
        item.setPath(TEST_PATH);
        item.setSource(TEST_PATH);
        item.setFormat(BiologicalDataItemFormat.REFERENCE);
        item.setType(BiologicalDataItemResourceType.FILE);
        item.setCreatedDate(new Date());
        item.setOwner(TEST_OWNER);

        biologicalDataItemDao.createBiologicalDataItem(item);

        Reference reference = createTestReference(item);
        GeneFile geneFile = createTestGeneFile(reference, TEST_GENE_FILE_NAME);
        referenceGenomeDao.updateReferenceGeneFileId(reference.getId(), geneFile.getId());

        for (final String match : MATCHING_NAMES) {
            List<BiologicalDataItem> loadedItems = biologicalDataItemDao.loadFilesByName(match);
            Assert.assertFalse(loadedItems.isEmpty());
        }

        for (final String notMatch : NOT_MATCHING_NAMES) {
            List<BiologicalDataItem> empty = biologicalDataItemDao.loadFilesByNameStrict(notMatch);
            Assert.assertTrue(empty.isEmpty());
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSearchNotStrictProperLoadAllFields() {
        BiologicalDataItem item = new BiologicalDataItem();
        item.setName(TEST_NAME);
        item.setPath(TEST_PATH);
        item.setSource(TEST_PATH);
        item.setFormat(BiologicalDataItemFormat.REFERENCE);
        item.setType(BiologicalDataItemResourceType.FILE);
        item.setCreatedDate(new Date());
        item.setOwner(TEST_OWNER);

        biologicalDataItemDao.createBiologicalDataItem(item);

        Reference reference = createTestReference(item);
        GeneFile geneFile = createTestGeneFile(reference, TEST_GENE_FILE_NAME);
        GeneFile geneFile2 = createTestGeneFile(reference, TEST_GENE_FILE_NAME_2);

        List<BiologicalDataItem> loadedItems = biologicalDataItemDao.loadFilesByName(TEST_GENE_FILE_NAME);
        Assert.assertFalse(loadedItems.isEmpty());
        Assert.assertEquals(2, loadedItems.size());

        final GeneFile foundGeneFile = loadedItems.stream()
                .filter(bdi -> bdi.getName().equals(TEST_GENE_FILE_NAME)).findFirst()
                .map(bdi -> (GeneFile) bdi)
                .orElseThrow(IllegalStateException::new);
        compareTwoBioDataItems(geneFile, foundGeneFile, () -> compareTwoGeneFiles(geneFile, foundGeneFile));

        final GeneFile foundGeneFile2 = loadedItems.stream()
                .filter(bdi -> bdi.getName().equals(TEST_GENE_FILE_NAME_2)).findFirst()
                .map(bdi -> (GeneFile) bdi)
                .orElseThrow(IllegalStateException::new);
        compareTwoBioDataItems(geneFile2, foundGeneFile2, () -> compareTwoGeneFiles(geneFile2, foundGeneFile2));
    }

    private void compareTwoBioDataItems(final BiologicalDataItem expected, final BiologicalDataItem actual,
                                        final Runnable additionalCheck) {
        Assert.assertEquals(expected.getId(), actual.getId());
        Assert.assertEquals(expected.getPath(), actual.getPath());
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getFormat(), actual.getFormat());
        additionalCheck.run();
    }

    private void compareTwoGeneFiles(GeneFile expected, GeneFile actual) {
        BiologicalDataItem expectedIndex = expected.getIndex();
        BiologicalDataItem actualIndex = actual.getIndex();
        Assert.assertEquals(expectedIndex != null, actualIndex != null);
        if (expectedIndex != null) {
            Assert.assertEquals(expectedIndex.getId(), actualIndex.getId());
            Assert.assertEquals(expectedIndex.getPath(), actualIndex.getPath());
            Assert.assertEquals(expectedIndex.getName(), actualIndex.getName());
            Assert.assertEquals(expectedIndex.getFormat(), actualIndex.getFormat());
        }
    }

    private Reference createTestReference(BiologicalDataItem item) {
        Reference reference = new Reference();

        reference.setSize(1L);
        reference.setName(item.getName());
        reference.setPath(TEST_PATH);
        reference.setSource(TEST_PATH);
        reference.setType(BiologicalDataItemResourceType.FILE);
        reference.setId(item.getId());

        reference.setCreatedDate(new Date());
        reference.setBioDataItemId(item.getId());
        reference.setIndex(EntityHelper.createIndex(BiologicalDataItemFormat.REFERENCE_INDEX,
                BiologicalDataItemResourceType.FILE, ""));

        biologicalDataItemDao.createBiologicalDataItem(reference.getIndex());
        return referenceGenomeDao.createReferenceGenome(reference, referenceGenomeDao.createReferenceGenomeId());
    }

    private GeneFile createTestGeneFile(Reference reference, String geneFileName) {
        BiologicalDataItem indexItem = new BiologicalDataItem();
        indexItem.setCreatedDate(new Date());
        indexItem.setPath(TEST_PATH);
        indexItem.setSource(TEST_PATH);
        indexItem.setFormat(BiologicalDataItemFormat.GENE_INDEX);
        indexItem.setType(BiologicalDataItemResourceType.FILE);
        indexItem.setName("");
        indexItem.setOwner(TEST_OWNER);

        biologicalDataItemDao.createBiologicalDataItem(indexItem);

        GeneFile geneFile = new GeneFile();
        geneFile.setId(geneFileManager.createGeneFileId());
        geneFile.setName(geneFileName);
        geneFile.setCompressed(false);
        geneFile.setPath(TEST_PATH);
        geneFile.setSource(TEST_PATH);
        geneFile.setType(BiologicalDataItemResourceType.FILE); // For now we're working only with files
        geneFile.setCreatedDate(new Date());
        geneFile.setReferenceId(reference.getId());
        geneFile.setIndex(indexItem);
        geneFile.setOwner(TEST_OWNER);

        long id = geneFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(geneFile);
        geneFileDao.createGeneFile(geneFile, id);
        return geneFile;
    }
}
