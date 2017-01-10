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
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.project.ProjectItem;
import com.epam.catgenome.entity.reference.Bookmark;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.VcfReadingException;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.project.ProjectManager;
import com.epam.catgenome.manager.reference.BookmarkManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.vcf.VcfManager;

/**
 * Source:      BookmarkManagerTest
 * Created:     29.01.16, 17:37
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BookmarkManagerTest {
    @Autowired
    private BookmarkManager bookmarkManager;

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private VcfManager vcfManager;

    @Autowired
    private GffManager gffManager;

    @Autowired
    private ApplicationContext context;

    private long referenceId;
    private Reference testReference;
    private Chromosome testChromosome;
    private static final String TEST_VCF_FILE_PATH = "classpath:templates/Felis_catus.vcf.gz";
    private static final String TEST_VCF_FILE_NAME1 = "file1";

    private static final int BOOKMARK_END_INDEX = 10000;
    private static final int BOOKMARK_END_INDEX2 = 20000;

    @Before
    public void setup() throws Exception {
        testChromosome = EntityHelper.createNewChromosome();
        testReference = EntityHelper.createNewReference(testChromosome, referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(testReference);
        referenceId = testReference.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testBookmarkCreateLoad()
        throws IOException, InterruptedException, NoSuchAlgorithmException, VcfReadingException, FeatureIndexException {
        VcfFile file = addVcfFile(TEST_VCF_FILE_NAME1, TEST_VCF_FILE_PATH);
        BiologicalDataItem item = new BiologicalDataItem();
        item.setId(file.getBioDataItemId());

        Project project = new Project();
        project.setName("testProject");

        ArrayList<ProjectItem> items = new ArrayList<>();
        items.add(new ProjectItem(testReference));
        items.add(new ProjectItem(item));
        project.setItems(items);

        projectManager.saveProject(project);

        Resource resource = context.getResource("classpath:templates/genes_sorted.gtf");

        FeatureIndexedFileRegistrationRequest geneRequest = new FeatureIndexedFileRegistrationRequest();
        geneRequest.setReferenceId(referenceId);
        geneRequest.setName("genes");
        geneRequest.setPath(resource.getFile().getAbsolutePath());

        GeneFile geneFile = gffManager.registerGeneFile(geneRequest);

        referenceGenomeManager.updateReferenceGeneFileId(referenceId, geneFile.getId());

        Bookmark bookmark = new Bookmark();
        bookmark.setOpenedItems(Collections.singletonList(item));
        bookmark.setStartIndex(1);
        bookmark.setEndIndex(BOOKMARK_END_INDEX);
        bookmark.setChromosome(testChromosome);
        bookmark.setName("testBookmark");

        bookmarkManager.saveBookmark(bookmark);

        List<Bookmark> loadedBookmarks = bookmarkManager.loadBookmarksByProject();

        Assert.assertNotNull(loadedBookmarks);
        Assert.assertFalse(loadedBookmarks.isEmpty());
        Assert.assertNull(loadedBookmarks.get(0).getOpenedItems());

        Bookmark loadedBookmark = loadedBookmarks.get(0);

        loadedBookmark = bookmarkManager.loadBookmark(loadedBookmark.getId());
        Assert.assertFalse(loadedBookmark.getOpenedItems().isEmpty());
        Assert.assertEquals(BiologicalDataItem.getBioDataItemId(loadedBookmark.getOpenedItems()
                .get(0)), item.getId());

        BiologicalDataItem item2 = new BiologicalDataItem();
        item2.setId(testReference.getBioDataItemId());
        loadedBookmark.getOpenedItems().add(item2);
        loadedBookmark.setStartIndex(BOOKMARK_END_INDEX);
        loadedBookmark.setEndIndex(BOOKMARK_END_INDEX2);

        bookmarkManager.saveBookmark(loadedBookmark);

        loadedBookmark = bookmarkManager.loadBookmark(loadedBookmark.getId());
        Assert.assertEquals(loadedBookmark.getOpenedItems().size(), 2);

        bookmarkManager.deleteBookmark(loadedBookmark.getId());
        loadedBookmarks = bookmarkManager.loadBookmarksByProject();

        Assert.assertTrue(loadedBookmarks.isEmpty());
    }

    private VcfFile addVcfFile(String name, String path)
        throws IOException, InterruptedException, NoSuchAlgorithmException, VcfReadingException {
        Resource resource = context.getResource(path);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setName(name);
        request.setPath(resource.getFile().getAbsolutePath());

        return vcfManager.registerVcfFile(request);
    }
}
