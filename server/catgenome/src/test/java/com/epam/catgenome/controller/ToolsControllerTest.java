/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import com.epam.catgenome.controller.tools.FeatureFileSortRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.Equals;
import org.mockito.internal.matchers.Find;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.common.AbstractControllerTest;
import com.epam.catgenome.common.ResponseResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class ToolsControllerTest extends AbstractControllerTest {
    @Autowired
    ApplicationContext context;

    @Autowired
    private static final String URL_SORT = "/restapi/tools/sort";

    public static final String UNSORTED_BED_NAME = "example.bed";
    public static final String EXPECTED_SORTED_SUFFIX_BED_NAME = "example.sorted.bed";
    public static final String SPECIFIED_BED_NAME = "sorted.bed";



    @Before
    public void setup() throws Exception {
        super.setup();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void sortedFileWithSuffixShouldBeCreatedIfNotSpecified() throws Exception {
        File toBeSorted = context.getResource("classpath:templates/" + UNSORTED_BED_NAME).getFile();
        File copiedToBeSorted = new File(getTempDirectory(), UNSORTED_BED_NAME);
        Files.copy(toBeSorted.toPath(), copiedToBeSorted.toPath());

        FeatureFileSortRequest request = new FeatureFileSortRequest();
        request.setOriginalFilePath(copiedToBeSorted.getAbsolutePath());

        assertSortRequest(request, new Find(".*" + EXPECTED_SORTED_SUFFIX_BED_NAME + "$"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void specifiedSortedFileShouldBeCreated() throws Exception {
        File tempDirectory = getTempDirectory();

        File toBeSorted = context.getResource("classpath:templates/" + UNSORTED_BED_NAME).getFile();
        File copiedToBeSorted = new File(tempDirectory, UNSORTED_BED_NAME);
        Files.copy(toBeSorted.toPath(), copiedToBeSorted.toPath());

        File sortedPath = new File(tempDirectory, SPECIFIED_BED_NAME);

        FeatureFileSortRequest request = new FeatureFileSortRequest();
        request.setOriginalFilePath(copiedToBeSorted.getAbsolutePath());
        request.setSortedFilePath(sortedPath.getAbsolutePath());

        assertSortRequest(request, new Equals(sortedPath.getAbsolutePath()));
    }

    private void assertSortRequest(FeatureFileSortRequest request, ArgumentMatcher payloadMatcher) throws Exception {
        ResultActions actions = mvc()
                .perform(post(URL_SORT).content(getObjectMapper().writeValueAsString(request))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).value(payloadMatcher))
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        final ResponseResult<String> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                String.class));

        Assert.assertNotNull(res.getPayload());
    }

    @NotNull
    private File getTempDirectory() throws IOException {
        File tmpDir = Files.createTempDirectory("sorttest").toFile();
        tmpDir.deleteOnExit();
        return tmpDir;
    }
}
