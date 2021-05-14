/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

import com.epam.catgenome.common.AbstractControllerTest;
import com.epam.catgenome.common.ResponseResult;
import com.epam.catgenome.controller.vo.ProjectVO;
import com.epam.catgenome.entity.task.Task;
import com.epam.catgenome.entity.task.TaskStatus;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class TaskControllerTest extends AbstractControllerTest {
    public static final String PRETTY_NAME = "pretty";
    @Autowired
    ApplicationContext context;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private VcfManager vcfManager;

    @Autowired
    private GffManager gffManager;

    private static final String URL_SAVE_TASK = "/restapi/task/";
    private static final String URL_DELETE_TASK = "/restapi/task/%d";
    private static final String URL_LOAD_TASK = "/restapi/task/%d";

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveLoadDeleteTask() throws Exception {
        Task task = new Task();
        task.setTitle("TestTitle");
        task.setStatus(TaskStatus.CREATED);
        task.setCreatedDate(new Date());
        task.setOrganisms(Arrays.asList("Organism1", "Organism2"));

        // save
        ResultActions actions = mvc()
                .perform(post(URL_SAVE_TASK).content(getObjectMapper().writeValueAsString(task))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Task> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                Task.class));

        Task loadedTask = res.getPayload();
        Assert.assertNotNull(loadedTask);
        Assert.assertFalse(loadedTask.getOrganisms().isEmpty());

        // load
        actions = mvc()
                .perform(get(String.format(URL_LOAD_TASK, loadedTask.getId()))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                Task.class));

        loadedTask = res.getPayload();
        Assert.assertNotNull(loadedTask);
        Assert.assertFalse(loadedTask.getOrganisms().isEmpty());
//        loadedProject.getItems().forEach(i -> Assert.assertNotNull(i.getId()));
    }

    private Task saveTask(Task task) throws Exception {
        MockHttpServletRequestBuilder builder = post(URL_SAVE_TASK).content(getObjectMapper()
                .writeValueAsString(task)).contentType(EXPECTED_CONTENT_TYPE);

        // save task
        ResultActions actions = mvc()
                .perform(builder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Task> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                ProjectVO.class));

        Task loadedTask = res.getPayload();
        Assert.assertNotNull(loadedTask);

        return loadedTask;
    }

    private Task loadTask(long taskId) throws Exception {
        ResultActions actions = mvc()
                .perform(get(String.format(URL_LOAD_TASK, taskId)).contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Task> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                Task.class));

        Task loadedTask = res.getPayload();
        Assert.assertNotNull(loadedTask);
        return loadedTask;
    }
}
