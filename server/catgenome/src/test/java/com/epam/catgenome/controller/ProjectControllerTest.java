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

import static java.util.Collections.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.common.AbstractControllerTest;
import com.epam.catgenome.common.ResponseResult;
import com.epam.catgenome.controller.vo.GeneSearchQuery;
import com.epam.catgenome.controller.vo.ProjectVO;
import com.epam.catgenome.controller.vo.converter.ProjectConverter;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.project.ProjectItem;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.VcfReadingException;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.vcf.VcfManager;

/**
 * Source:      ProjectControllerTest
 * Created:     15.01.16, 16:41
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class ProjectControllerTest extends AbstractControllerTest {
    public static final String PRETTY_NAME = "pretty";
    @Autowired
    ApplicationContext context;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private VcfManager vcfManager;

    @Autowired
    private GffManager gffManager;

    private static final String URL_SAVE_PROJECT = "/restapi/project/save";
    private static final String URL_LOAD_TREE = "/restapi/project/tree";
    private static final String URL_LOAD_PROJECT = "/restapi/project/%d/load";
    private static final String URL_MOVE_PROJECT = "/restapi/project/%d/move";
    private static final String URL_LOAD_PROJECT_BY_NAME = "/restapi/project/load";
    private static final String URL_LOAD_MY_PROJECTS = "/restapi/project/loadMy";
    private static final String URL_ADD_PROJECT_ITEM = "/restapi/project/%d/add/%d";
    private static final String URL_REMOVE_PROJECT_ITEM = "/restapi/project/%d/remove/%d";
    private static final String URL_HIDE_PROJECT_ITEM = "/restapi/project/%d/hide/%d";
    private static final String URL_SEARCH_FEATURE = "/restapi/project/%d/search";
    private static final String URL_DELETE_PROJECT = "/restapi/project/%d";
    private static final String URL_FILTER_VCF = "/restapi/project/%d/filter/vcf";
    private static final String URL_FILTER_VCF_SEARCH_GENES = "/restapi/project/%d/filter/vcf/searchGenes";
    private static final String URL_FILTER_VCF_INFO = "/restapi/project/%d/filter/vcf/info";

    private static final String TEST_VCF_FILE_PATH = "classpath:templates/Felis_catus.vcf.gz";
    private static final String TEST_VCF_UNK_FILE_PATH = "classpath:templates/samples.vcf";
    private static final String TEST_GENE_FILE_PATH = "classpath:templates/genes_sorted.gtf";
    private static final String TEST_VCF_FILE_NAME1 = "file1";
    private static final String TEST_GENE_FILE_NAME1 = "gene file1";
    private static final String TEST_VCF_FILE_NAME2 = "file2";
    private static final String TEST_FEATURE_ID = "ENSFCAG00000031108";
    private static final String TEST_PROJECT_NAME = "testProject";

    private long referenceId = 1;
    private Reference testReference;
    private static final int TEST_CHROMOSOME_SIZE = 239107476;

    @Before
    public void setup() throws Exception {
        super.setup();
        Chromosome testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        testReference = EntityHelper.createNewReference(testChromosome,
                referenceGenomeManager.createReferenceId());
        referenceGenomeManager.create(testReference);
        referenceId = testReference.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveLoadDeleteProject() throws Exception {
        VcfFile file = addVcfFile(TEST_VCF_FILE_NAME1, TEST_VCF_FILE_PATH);
        BiologicalDataItem item = new BiologicalDataItem();
        item.setId(file.getBioDataItemId());
        ProjectItem projectItem = new ProjectItem();
        projectItem.setBioDataItem(item);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId())),
                projectItem));

        ProjectVO projectVO = ProjectConverter.convertTo(project);

        // save project
        ResultActions actions = mvc()
                .perform(post(URL_SAVE_PROJECT).content(getObjectMapper().writeValueAsString(projectVO))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<ProjectVO> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                ProjectVO.class));

        ProjectVO loadedProject = res.getPayload();
        Assert.assertNotNull(loadedProject);
        Assert.assertFalse(loadedProject.getItems().isEmpty());

        // load
        actions = mvc()
                .perform(get(String.format(URL_LOAD_PROJECT, loadedProject.getId()))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                ProjectVO.class));

        loadedProject = res.getPayload();
        Assert.assertNotNull(loadedProject);
        Assert.assertFalse(loadedProject.getItems().isEmpty());
        loadedProject.getItems().forEach(i -> Assert.assertNotNull(i.getId()));

        // load by name
        actions = mvc()
            .perform(get(URL_LOAD_PROJECT_BY_NAME).param("projectName", loadedProject.getName())
                         .contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        res = getObjectMapper()
            .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                       getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                                                  ProjectVO.class));

        loadedProject = res.getPayload();
        Assert.assertNotNull(loadedProject);
        Assert.assertFalse(loadedProject.getItems().isEmpty());
        loadedProject.getItems().forEach(i -> Assert.assertNotNull(i.getId()));

        // load my
        actions = mvc()
                .perform(get(URL_LOAD_MY_PROJECTS)
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<List<ProjectVO>> myProjects = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(List.class, List.class, ProjectVO.class)));

        Assert.assertNotNull(myProjects.getPayload());
        Assert.assertFalse(myProjects.getPayload().isEmpty());
        Assert.assertFalse(myProjects.getPayload().stream().anyMatch(p -> p.getItems().isEmpty()));
        Assert.assertFalse(myProjects.getPayload().stream().anyMatch(p -> p.getItemsCount() == null));

        actions = mvc()
                .perform(delete(String.format(URL_DELETE_PROJECT, loadedProject.getId()))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Boolean> resp = getObjectMapper().readValue(actions.andReturn().getResponse()
                .getContentAsByteArray(), getTypeFactory().constructParametrizedType(ResponseResult.class,
                ResponseResult.class, Boolean.class));

        Assert.assertTrue(resp.getPayload());

        actions = mvc()
                .perform(get(String.format(URL_LOAD_PROJECT, loadedProject.getId()))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.ERROR.name()));
        actions.andDo(MockMvcResultHandlers.print());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveLoadProjectWithPrettyName() throws Exception {
        VcfFile file = addVcfFile(TEST_VCF_FILE_NAME1, TEST_VCF_FILE_PATH);
        BiologicalDataItem item = new BiologicalDataItem();
        item.setId(file.getBioDataItemId());
        ProjectItem projectItem = new ProjectItem();
        projectItem.setBioDataItem(item);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
        project.setPrettyName(PRETTY_NAME);
        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId())),
                projectItem));

        ProjectVO projectVO = ProjectConverter.convertTo(project);

        // save project
        ResultActions actions = mvc()
                .perform(post(URL_SAVE_PROJECT).content(getObjectMapper().writeValueAsString(projectVO))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<ProjectVO> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                ProjectVO.class));

        ProjectVO loadedProject = res.getPayload();
        Assert.assertNotNull(loadedProject);
        Assert.assertFalse(loadedProject.getItems().isEmpty());
        Assert.assertEquals(PRETTY_NAME, loadedProject.getPrettyName());

        actions = mvc()
                .perform(get(URL_LOAD_PROJECT_BY_NAME).param("projectName", TEST_PROJECT_NAME)
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                ProjectVO.class));

        loadedProject = res.getPayload();
        Assert.assertNotNull(loadedProject);
        Assert.assertFalse(loadedProject.getItems().isEmpty());
        Assert.assertEquals("pretty", loadedProject.getPrettyName());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testMoveLoadNested() throws Exception {
        Project parent = new Project();
        parent.setName("testParent");
        parent.setItems(singletonList(new ProjectItem(new BiologicalDataItem(
            testReference.getBioDataItemId()))));

        ProjectVO createdParentProject = saveProject(parent, null);

        Project child1 = new Project();
        child1.setName("testChild1");
        child1.setItems(singletonList(new ProjectItem(new BiologicalDataItem(
            testReference.getBioDataItemId()))));

        ProjectVO createdChild1 = saveProject(child1, createdParentProject.getId());

        ProjectVO loadedParent = loadProject(createdParentProject.getId());
        Assert.assertFalse(loadedParent.getNestedProjects().isEmpty());

        Project child2 = new Project();
        child2.setName("testChild2");
        child2.setItems(singletonList(new ProjectItem(new BiologicalDataItem(
            testReference.getBioDataItemId()))));

        ProjectVO createdChild2 = saveProject(child2, null);

        // move
        ResultActions actions = mvc()
            .perform(put(String.format(URL_MOVE_PROJECT, createdChild2.getId()))
                         .param("parentId", loadedParent.getId().toString()).contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        loadedParent = loadProject(loadedParent.getId());
        Assert.assertNotNull(loadedParent.getNestedProjects());
        Assert.assertEquals(2, loadedParent.getNestedProjects().size());

        List<ProjectVO> topLevel = loadMy();
        Assert.assertEquals(1, topLevel.size());

        // test load hierarchy
        VcfFile file2 = addVcfFile(TEST_VCF_FILE_NAME2, TEST_VCF_FILE_PATH);

        actions = mvc()
            .perform(put(String.format(URL_ADD_PROJECT_ITEM, createdChild1.getId(), file2.getBioDataItemId())))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        MvcResult mvcResult = mvc()
                .perform(get(URL_LOAD_TREE)
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(request().asyncStarted())
                .andReturn();

        actions = mvc()
                .perform(asyncDispatch(mvcResult))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<List<ProjectVO>> treeRes = getObjectMapper()
            .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                       getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                                      getTypeFactory().constructParametrizedType(List.class, List.class,
                                                                                                 ProjectVO.class)));

        List<ProjectVO> tree = treeRes.getPayload();
        Assert.assertFalse(tree.isEmpty());
        Assert.assertFalse(tree.stream().anyMatch(p -> p.getNestedProjects().isEmpty()));
        Assert.assertFalse(tree.get(0).getNestedProjects().get(0).getItems().isEmpty());
        // move to top level
        actions = mvc()
            .perform(put(String.format(URL_MOVE_PROJECT, createdChild2.getId())).contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        loadedParent = loadProject(loadedParent.getId());
        Assert.assertEquals(1, loadedParent.getNestedProjects().size());

        topLevel = loadMy();
        Assert.assertEquals(2, topLevel.size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadTreeWithParent() throws Exception {
        Project parent = new Project();
        parent.setName("testParent");
        parent.setItems(singletonList(new ProjectItem(new BiologicalDataItem(
                testReference.getBioDataItemId()))));

        ProjectVO createdParentProject = saveProject(parent, null);

        Project child1 = new Project();
        child1.setName("testChild1");
        child1.setItems(singletonList(new ProjectItem(new BiologicalDataItem(
                testReference.getBioDataItemId()))));

        ProjectVO createdChild1 = saveProject(child1, createdParentProject.getId());

        Project child2 = new Project();
        child2.setName("testChild2");
        child2.setItems(singletonList(new ProjectItem(new BiologicalDataItem(
                testReference.getBioDataItemId()))));

        saveProject(child2, createdParentProject.getId());

        Project child11 = new Project();
        child11.setName("testChild11");
        child11.setItems(singletonList(new ProjectItem(new BiologicalDataItem(
                testReference.getBioDataItemId()))));

        saveProject(child11, createdChild1.getId());

        MvcResult mvcResult = mvc()
                .perform(get(URL_LOAD_TREE)
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(request().asyncStarted())
                .andReturn();

        ResultActions actions = mvc()
                .perform(asyncDispatch(mvcResult))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<List<ProjectVO>> treeRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(List.class, List.class,
                                        ProjectVO.class)));

        List<ProjectVO> tree = treeRes.getPayload();
        Assert.assertFalse(tree.isEmpty());
        Assert.assertEquals(2, tree.get(0).getNestedProjects().size());

        mvcResult = mvc()
                .perform(get(URL_LOAD_TREE)
                        .param("parentId", String.valueOf(createdChild1.getId()))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(request().asyncStarted())
                .andReturn();

        actions = mvc()
                .perform(asyncDispatch(mvcResult))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<List<ProjectVO>> treeResWithParent = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(List.class, List.class,
                                        ProjectVO.class)));

        List<ProjectVO> treeWithParent = treeResWithParent.getPayload();
        Assert.assertFalse(treeWithParent.isEmpty());
        Assert.assertEquals(1, treeWithParent.get(0).getNestedProjects().size());
    }

    private ProjectVO saveProject(Project project, Long parentId) throws Exception {
        ProjectVO projectVO = ProjectConverter.convertTo(project);

        MockHttpServletRequestBuilder builder = post(URL_SAVE_PROJECT).content(getObjectMapper()
                                                   .writeValueAsString(projectVO)).contentType(EXPECTED_CONTENT_TYPE);
        if (parentId != null) {
            builder.param("parentId", parentId.toString());
        }

        // save project
        ResultActions actions = mvc()
            .perform(builder)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<ProjectVO> res = getObjectMapper()
            .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                       getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                                                  ProjectVO.class));

        ProjectVO loadedProject = res.getPayload();
        Assert.assertNotNull(loadedProject);

        return loadedProject;
    }

    private List<ProjectVO> loadMy() throws Exception {
        ResultActions actions = mvc()
            .perform(get(URL_LOAD_MY_PROJECTS)
                         .contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<List<ProjectVO>> myProjects = getObjectMapper()
            .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                       getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                                                  getTypeFactory().constructParametrizedType(List.class,
                                                                                         List.class, ProjectVO.class)));

        Assert.assertNotNull(myProjects.getPayload());
        Assert.assertFalse(myProjects.getPayload().isEmpty());

        return myProjects.getPayload();
    }

    private ProjectVO loadProject(long projcetId) throws Exception {
        ResultActions actions = mvc()
            .perform(get(String.format(URL_LOAD_PROJECT, projcetId)).contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<ProjectVO> res = getObjectMapper()
            .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                       getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                                                  ProjectVO.class));

        ProjectVO loadedProject = res.getPayload();
        Assert.assertNotNull(loadedProject);
        return loadedProject;
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testAddRemoveHide() throws Exception {
        VcfFile file = addVcfFile(TEST_VCF_FILE_NAME1, TEST_VCF_FILE_PATH);
        BiologicalDataItem item = new BiologicalDataItem();
        item.setId(file.getBioDataItemId());
        ProjectItem projectItem = new ProjectItem();
        projectItem.setBioDataItem(item);

        Project project = new Project();
        project.setName("testProject");
        project.setItems(Arrays.asList(projectItem,
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));

        ProjectVO projectVO = ProjectConverter.convertTo(project);

        // save project
        ResultActions actions = mvc()
                .perform(post(URL_SAVE_PROJECT).content(getObjectMapper().writeValueAsString(projectVO))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<ProjectVO> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                ProjectVO.class));

        ProjectVO loadedProject = res.getPayload();
        Assert.assertNotNull(loadedProject);
        Assert.assertFalse(loadedProject.getItems().isEmpty());

        // add item
        VcfFile file2 = addVcfFile(TEST_VCF_FILE_NAME2, TEST_VCF_FILE_PATH);

        actions = mvc()
                .perform(put(String.format(URL_ADD_PROJECT_ITEM, loadedProject.getId(),
                        file2.getBioDataItemId())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                ProjectVO.class));

        loadedProject = res.getPayload();
        Assert.assertEquals(loadedProject.getItems().size(), 3);

        // remove item
        actions = mvc()
                .perform(delete(String.format(URL_REMOVE_PROJECT_ITEM, loadedProject.getId(),
                        file2.getBioDataItemId())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                ProjectVO.class));

        loadedProject = res.getPayload();
        Assert.assertEquals(loadedProject.getItems().size(), 2);

        // hide item
        actions = mvc()
                .perform(put(String.format(URL_HIDE_PROJECT_ITEM, loadedProject.getId(), file.getBioDataItemId())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                ProjectVO.class));

        loadedProject = res.getPayload();
        Assert.assertNotNull(loadedProject.getItems());
        Assert.assertTrue(loadedProject.getItems().get(0).getHidden());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testSearchFeature() throws Exception {
        GeneFile file = addGeneFile(TEST_VCF_FILE_NAME1, TEST_GENE_FILE_PATH);
        BiologicalDataItem item = new BiologicalDataItem();
        item.setId(file.getBioDataItemId());
        ProjectItem projectItem = new ProjectItem();
        projectItem.setBioDataItem(item);

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), file.getId());

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
        project.setItems(Arrays.asList(projectItem, new ProjectItem(testReference)));

        ProjectVO projectVO = ProjectConverter.convertTo(project);

        // save project
        ResultActions actions = mvc()
                .perform(post(URL_SAVE_PROJECT).content(getObjectMapper().writeValueAsString(projectVO))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<ProjectVO> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                ProjectVO.class));

        ProjectVO loadedProject = res.getPayload();
        Assert.assertNotNull(loadedProject);
        Assert.assertFalse(loadedProject.getItems().isEmpty());

        // search by feature "ENSFCAG00000031108"
        actions = mvc()
                .perform(get(String.format(URL_SEARCH_FEATURE, loadedProject.getId()))
                        .param("featureId", TEST_FEATURE_ID)
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<IndexSearchResult<FeatureIndexEntry>> indexEntriesRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                IndexSearchResult.class));

        Assert.assertFalse(indexEntriesRes.getPayload().getEntries().isEmpty());
        Assert.assertTrue(TEST_FEATURE_ID.equalsIgnoreCase(indexEntriesRes.getPayload().getEntries()
                .get(0).getFeatureId()));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testFilterVcf() throws Exception {
        GeneFile geneFile = addGeneFile(TEST_GENE_FILE_NAME1, TEST_GENE_FILE_PATH);
        BiologicalDataItem geneItem = new BiologicalDataItem();
        geneItem.setId(geneFile.getBioDataItemId());
        ProjectItem geneProjectItem = new ProjectItem();
        geneProjectItem.setBioDataItem(geneItem);

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), geneFile.getId());

        VcfFile vcfFile = addVcfFile(TEST_VCF_FILE_NAME1, "classpath:templates/Felis_catus.vcf");
        BiologicalDataItem vcfItem = new BiologicalDataItem();
        vcfItem.setId(vcfFile.getBioDataItemId());
        ProjectItem vcfProjectItem = new ProjectItem();
        vcfProjectItem.setBioDataItem(vcfItem);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
        project.setItems(Arrays.asList(geneProjectItem, vcfProjectItem,
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));

        ProjectVO projectVO = ProjectConverter.convertTo(project);

        // save project
        ResultActions actions = mvc()
                .perform(post(URL_SAVE_PROJECT).content(getObjectMapper().writeValueAsString(projectVO))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<ProjectVO> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                ProjectVO.class));

        ProjectVO loadedProject = res.getPayload();
        Assert.assertNotNull(loadedProject);
        Assert.assertEquals(3, loadedProject.getItems().size());

        // filter vcf
        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIdsByProject(singletonMap(loadedProject.getId(), singletonList(vcfFile.getId())));
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(singletonList("ENS"), false));
        vcfFilterForm.setVariationTypes(new VcfFilterForm.FilterSection<>(Arrays.asList(VariationType.MNP,
                VariationType.INS), false));

        actions = mvc()
                .perform(post(String.format(URL_FILTER_VCF, loadedProject.getId())).content(
                        getObjectMapper().writeValueAsString(vcfFilterForm)).contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()))
                .andDo(MockMvcResultHandlers.print());

        ResponseResult<List<FeatureIndexEntry>> filterRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(List.class, List.class,
                                        VcfIndexEntry.class)));

        Assert.assertFalse(filterRes.getPayload().isEmpty());

        // filter by additional fields
        Map<String, Object> additionalFilters = new HashMap<>();
        additionalFilters.put("SVTYPE", "BND");
        vcfFilterForm.setAdditionalFilters(additionalFilters);
        vcfFilterForm.setGenes(null);
        vcfFilterForm.setVariationTypes(null);

        actions = mvc()
                .perform(post(String.format(URL_FILTER_VCF, loadedProject.getId())).content(
                        getObjectMapper().writeValueAsString(vcfFilterForm)).contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()))
                .andDo(MockMvcResultHandlers.print());

        filterRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(List.class, List.class,
                                        VcfIndexEntry.class)));

        Assert.assertFalse(filterRes.getPayload().isEmpty());

        // search genes
        GeneSearchQuery geneSearchQuery = new GeneSearchQuery();
        geneSearchQuery.setSearch("ENS");
        geneSearchQuery.setVcfIdsByProject(singletonMap(loadedProject.getId(), singletonList(vcfFile.getId())));

        actions = mvc()
                .perform(post(String.format(URL_FILTER_VCF_SEARCH_GENES, loadedProject.getId())).content(
                        getObjectMapper().writeValueAsString(geneSearchQuery)).contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()))
                .andDo(MockMvcResultHandlers.print());

        ResponseResult<Set<String>> geneNamesAvailable = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(Set.class, Set.class, String.class)));

        Assert.assertFalse(geneNamesAvailable.getPayload().isEmpty());

        // test get filter info
        actions = mvc()
                .perform(get(String.format(URL_FILTER_VCF_INFO, loadedProject.getId()))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<VcfFilterInfo> infoRes = getObjectMapper().readValue(
                actions.andReturn().getResponse().getContentAsByteArray(),
                getTypeFactory().constructParametrizedType(ResponseResult.class,
                ResponseResult.class, VcfFilterInfo.class));

        Assert.assertNotNull(infoRes.getPayload());
        Assert.assertFalse(infoRes.getPayload().getAvailableFilters().isEmpty());
        Assert.assertNull(infoRes.getPayload().getInfoItemMap());
        Assert.assertFalse(infoRes.getPayload().getInfoItems().isEmpty());
    }

    private GeneFile addGeneFile(String name, String path) throws IOException, FeatureIndexException,
            InterruptedException, NoSuchAlgorithmException {
        Resource resource = context.getResource(path);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setName(name);
        request.setPath(resource.getFile().getAbsolutePath());

        return gffManager.registerGeneFile(request);
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

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    // TODO: code of this test is equal to testFilterVcf, remove duplicated code
    public void testFilterVcfWithUnknownVariations() throws Exception {
        VcfFile vcfFile = addVcfFile(TEST_VCF_FILE_NAME1, TEST_VCF_UNK_FILE_PATH);
        BiologicalDataItem vcfItem = new BiologicalDataItem();
        vcfItem.setId(vcfFile.getBioDataItemId());
        ProjectItem vcfProjectItem = new ProjectItem();
        vcfProjectItem.setBioDataItem(vcfItem);

        GeneFile geneFile = addGeneFile(TEST_GENE_FILE_NAME1, TEST_GENE_FILE_PATH);
        BiologicalDataItem geneItem = new BiologicalDataItem();
        geneItem.setId(geneFile.getBioDataItemId());
        ProjectItem geneProjectItem = new ProjectItem();
        geneProjectItem.setBioDataItem(geneItem);

        Project project = new Project();
        project.setName(TEST_PROJECT_NAME);
        project.setItems(Arrays.asList(geneProjectItem, vcfProjectItem,
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));

        ProjectVO projectVO = ProjectConverter.convertTo(project);

        // save project
        ResultActions actions = mvc()
                .perform(post(URL_SAVE_PROJECT).content(getObjectMapper().writeValueAsString(projectVO))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<ProjectVO> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                ProjectVO.class));

        ProjectVO loadedProject = res.getPayload();
        Assert.assertNotNull(loadedProject);
        Assert.assertEquals(loadedProject.getItems().size(), 3);

        // filter vcf
        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIdsByProject(singletonMap(loadedProject.getId(), singletonList(vcfFile.getId())));
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(singletonList("ENS"), false));
        vcfFilterForm.setVariationTypes(new VcfFilterForm.FilterSection<>(Arrays.asList(VariationType.MNP,
                VariationType.INS), false));

        actions = mvc()
                .perform(post(String.format(URL_FILTER_VCF, loadedProject.getId())).content(
                        getObjectMapper().writeValueAsString(vcfFilterForm)).contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()))
                .andDo(MockMvcResultHandlers.print());

    }
}
