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

package com.epam.catgenome.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.common.AbstractControllerTest;
import com.epam.catgenome.common.ResponseResult;
import com.epam.catgenome.controller.vo.BookmarkVO;
import com.epam.catgenome.controller.vo.converter.BookmarkConverter;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.project.ProjectItem;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.exception.VcfReadingException;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.project.ProjectManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.vcf.VcfManager;

/**
 * Source:      BookmarksController
 * Created:     01.02.16, 13:51
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class BookmarksControllerTest extends AbstractControllerTest {
    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private VcfManager vcfManager;

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private ApplicationContext context;

    private long referenceId = 1;
    private Reference testReference;
    private Chromosome testChromosome;
    private Project testProject;
    private static final String TEST_VCF_FILE_PATH = "classpath:templates/Felis_catus.vcf.gz";
    private static final String TEST_VCF_FILE_NAME1 = "file1";

    private static final int BOOKMARK_END_INDEX = 10000;
    private static final int BOOKMARK_END_INDEX2 = 20000;

    private static final String URL_BOOKMARK_SAVE = "/bookmark/save";
    private static final String URL_BOOKMARKS_LOAD = "/bookmarks";
    private static final String URL_BOOKMARK_LOAD = "/bookmark/%d";
    private static final String URL_BOOKMARK_DELETE = "/bookmark/%d";

    @Before
    public void setup() throws Exception {
        super.setup();

        referenceId = referenceGenomeManager.createReferenceId();
        testReference = new Reference();
        testReference.setId(referenceId);
        testReference.setName("testReference");
        testReference.setSize(0L);
        testReference.setPath("");
        testReference.setCreatedDate(new Date());

        testChromosome = EntityHelper.createNewChromosome();

        testReference.setChromosomes(Collections.singletonList(testChromosome));

        referenceGenomeManager.register(testReference);

        VcfFile file = addVcfFile(TEST_VCF_FILE_NAME1, TEST_VCF_FILE_PATH);
        BiologicalDataItem item = new BiologicalDataItem();
        item.setId(file.getBioDataItemId());

        testProject = new Project();
        testProject.setName("testProject");

        ArrayList<ProjectItem> items = new ArrayList<>();
        items.add(new ProjectItem(testReference));
        items.add(new ProjectItem(item));
        testProject.setItems(items);

        projectManager.saveProject(testProject);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testBookmarkService() throws Exception {
        BookmarkVO bookmarkVO = new BookmarkVO();

        bookmarkVO.setOpenedItems(Collections.singletonList(BookmarkConverter.convertTo(
            testProject.getItems().get(0).getBioDataItem())));
        bookmarkVO.setStartIndex(1);
        bookmarkVO.setEndIndex(BOOKMARK_END_INDEX);
        bookmarkVO.setChromosome(testChromosome);
        bookmarkVO.setName("testBookmark");

        // save bookmarks
        ResultActions actions = mvc()
                .perform(post(URL_BOOKMARK_SAVE).content(getObjectMapper().writeValueAsString(bookmarkVO))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<BookmarkVO> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                BookmarkVO.class));

        Assert.assertNotNull(res.getPayload());
        Assert.assertNotNull(res.getPayload().getId());
        Assert.assertEquals(bookmarkVO.getName(), res.getPayload().getName());
        Assert.assertFalse(res.getPayload().getOpenedItems().isEmpty());

        // load bookmarks
        actions = mvc()
                .perform(get(URL_BOOKMARKS_LOAD)
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<List<BookmarkVO>> loadedRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory()
                                        .constructParametrizedType(List.class, List.class, BookmarkVO.class)));

        Assert.assertNotNull(loadedRes.getPayload());
        Assert.assertFalse(loadedRes.getPayload().isEmpty());

        BookmarkVO loadedBookmark = loadedRes.getPayload().get(0);

        Assert.assertEquals(bookmarkVO.getName(), loadedBookmark.getName());
        Assert.assertEquals(bookmarkVO.getStartIndex(), loadedBookmark.getStartIndex());
        Assert.assertEquals(bookmarkVO.getEndIndex(), loadedBookmark.getEndIndex());
        Assert.assertNull(loadedBookmark.getOpenedItems());

        actions = mvc()
                .perform(get(String.format(URL_BOOKMARK_LOAD, loadedBookmark.getId()))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<BookmarkVO> loadedOneRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                BookmarkVO.class));

        loadedBookmark = loadedOneRes.getPayload();
        Assert.assertFalse(loadedBookmark.getOpenedItems().isEmpty());
        Assert.assertEquals(bookmarkVO.getOpenedItems().get(0).getId(), loadedBookmark.getOpenedItems().get(0).getId());

        // update

        loadedBookmark.getOpenedItems().add(BookmarkConverter.convertTo(
            testProject.getItems().get(1).getBioDataItem()));
        loadedBookmark.setStartIndex(BOOKMARK_END_INDEX);
        loadedBookmark.setEndIndex(BOOKMARK_END_INDEX2);

        actions = mvc()
                .perform(post(URL_BOOKMARK_SAVE).content(getObjectMapper().writeValueAsString(loadedBookmark))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                BookmarkVO.class));

        Assert.assertEquals(2, res.getPayload().getOpenedItems().size());

        // delete
        actions = mvc()
                .perform(delete(String.format(URL_BOOKMARK_DELETE, loadedBookmark.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Boolean> boolRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                Boolean.class));

        Assert.assertTrue(boolRes.getPayload());


        actions = mvc()
                .perform(get(URL_BOOKMARKS_LOAD)
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).doesNotExist()) // should not have payload
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());
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
