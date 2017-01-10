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
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.controller.vo.registration.FileRegistrationRequest;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.entity.wig.WigFile;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.wig.WigFileManager;

/**
 * Source:      WigControllerTest
 * Created:     21.01.16, 13:51
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class WigControllerTest extends AbstractControllerTest {
    private static final String LOAD_WIG_FILES = "/wig/%d/loadAll";
    private static final String WIG_FILE_REGISTER = "/wig/register";
    private static final String WIG_FILE_DELETE = "/secure/wig/register";
    private static final String WIG_GET_TRACK = "/wig/track/get";


    private static final String TEST_WIG_NSAME = "Hi Harry";

    private static final int TEST_CHROMOSOME_SIZE = 239107476;

    private static final int TEST_START_INDEX = 12587700;
    private static final int TEST_END_INDEX = 12588800;
    private static final double TEST_SCALE_FACTOR = 0.01;

    private long referenceId;

    private Reference testReference;
    private Chromosome testChromosome;

    @Autowired
    private WigFileManager wigFileManager;

    @Autowired
    ApplicationContext context;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Before
    public void setup() throws Exception {
        super.setup();
        testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setName("X");
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        testReference = EntityHelper.createNewReference(testChromosome, referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(testReference);

        referenceId = testReference.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadWigFiles() throws Exception {

        Resource resource = context.getResource("classpath:templates//agnX1.09-28.trim.dm606.realign.bw");

        FileRegistrationRequest request = new FileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());
        request.setName(TEST_WIG_NSAME);

        ResultActions actions = mvc()
                .perform(post(WIG_FILE_REGISTER).content(getObjectMapper().writeValueAsString(request))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        final ResponseResult<WigFile> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                WigFile.class));

        WigFile wigFile = res.getPayload();
        Assert.assertNotNull(wigFile.getId());
        Assert.assertNotNull(wigFile.getName());

        // Load all file by reference ID
        final Long fileId = res.getPayload().getId();

        actions = mvc()
                .perform(get(String.format(LOAD_WIG_FILES, testReference.getId()))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<List<WigFile>> wigFilesRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(List.class, List.class, WigFile.class)));

        Assert.assertNotNull(wigFilesRes.getPayload());
        Assert.assertFalse(wigFilesRes.getPayload().isEmpty());
        Assert.assertEquals(wigFilesRes.getPayload().get(0).getId(), wigFile.getId());


        // Load a track by fileId
        TrackQuery wigTrackQuery = new TrackQuery();
        //not realy
        wigTrackQuery.setChromosomeId(testChromosome.getId());
        wigTrackQuery.setStartIndex(TEST_START_INDEX);
        wigTrackQuery.setEndIndex(TEST_END_INDEX);
        wigTrackQuery.setScaleFactor(TEST_SCALE_FACTOR);
        wigTrackQuery.setId(fileId);

        actions = mvc()
                .perform(post(WIG_GET_TRACK).content(getObjectMapper().writeValueAsString(wigTrackQuery))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Track<Wig>> readSumRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(Track.class, Track.class, Wig.class)));

        Assert.assertFalse(readSumRes.getPayload().getBlocks().isEmpty());
        Wig wig = readSumRes.getPayload().getBlocks().get(0);
        Assert.assertNotNull(wig);
        Assert.assertNotNull(wig.getStartIndex());
        Assert.assertNotNull(wig.getEndIndex());
        Assert.assertNotNull(wig.getValue());

        //delete file
        actions = mvc()
                .perform(delete(WIG_FILE_DELETE).param("wigFileId", wigFilesRes.getPayload().get(0).getId()
                        .toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        wigFile = wigFileManager.loadWigFile(wigFilesRes.getPayload().get(0).getId());
        Assert.assertNull(wigFile);

    }
}
