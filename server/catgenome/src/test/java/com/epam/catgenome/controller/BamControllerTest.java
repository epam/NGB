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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.util.List;

import com.epam.catgenome.manager.parallel.TaskExecutorService;
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
import com.epam.catgenome.controller.vo.ReadQuery;
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.bam.BamQueryOption;
import com.epam.catgenome.entity.bam.BamTrack;
import com.epam.catgenome.entity.bam.Read;
import com.epam.catgenome.entity.bam.TrackDirectionType;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.manager.reference.ReferenceManager;

/**
 * Test suite for {@code BamController} class
 * Source:      BamControllerTest.java
 * Created:     1/15/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class BamControllerTest extends AbstractControllerTest {
    private static final String BAM_FILE_REGISTER = "/restapi/bam/register";
    private static final String BAM_FILE_UNREGISTER = "/restapi/secure/bam/register";
    private static final String LOAD_BAM_FILES = "/restapi/bam/%d/loadAll";
    private static final String BAM_TRACK_GET = "/restapi/bam/track/get";
    private static final String BAM_READ_LOAD = "/restapi/bam/read/load";
    private static final String TEST_NSAME = "BIG " + BamControllerTest.class.getSimpleName();
    private static final String TEST_BAM = "classpath:templates/agnX1.09-28.trim.dm606.realign.bam";
    private static final int TEST_START_INDEX = 12584188;
    private static final int TEST_END_INDEX = 12584688;

    @Autowired
    ApplicationContext context;

    private long referenceId;
    private Resource resource;
    private String chromosomeName = "X";
    private Reference testReference;
    private Chromosome testChromosome;

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private TaskExecutorService taskExecutorService;

    @Before
    public void setup() throws Exception {
        super.setup();

        taskExecutorService.setForceSequential(true);

        resource = context.getResource("classpath:templates");
        File fastaFile = new File(resource.getFile().getAbsolutePath() + "//dm606.X.fa");


        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName(TEST_NSAME);
        request.setPath(fastaFile.getPath());

        testReference = referenceManager.registerGenome(request);

        List<Chromosome> chromosomeList = testReference.getChromosomes();
        for (Chromosome chromosome : chromosomeList) {
            if (chromosome.getName().equals(chromosomeName)) {
                testChromosome = chromosome;
                break;
            }
        }
        referenceId = testReference.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void registerBamTest() throws Exception {
        File resource = getResourceFileCopy(TEST_BAM);
        getResourceFileCopy(TEST_BAM + ".bai");
        IndexedFileRegistrationRequest request = initRequest(resource.getAbsolutePath());
        ResultActions actions = mvc()
                .perform(post(BAM_FILE_REGISTER).content(getObjectMapper().writeValueAsString(request))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        final ResponseResult<BamFile> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                BamFile.class));

        Assert.assertNotNull(res.getPayload().getId());
        Assert.assertNotNull(res.getPayload().getName());

        final Long fileId = res.getPayload().getId();

        actions = mvc()
                .perform(get(String.format(LOAD_BAM_FILES, testReference.getId()))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));

        actions.andDo(MockMvcResultHandlers.print());
        ResponseResult<List<BamFile>> bamFilesRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(List.class, List.class, BamFile.class)));

        Assert.assertNotNull(bamFilesRes.getPayload());
        Assert.assertFalse(bamFilesRes.getPayload().isEmpty());
        Assert.assertEquals(bamFilesRes.getPayload().get(0).getId(), fileId);

        mvc()
                .perform(delete(BAM_FILE_UNREGISTER)
                        .param("bamFileId", String.valueOf(fileId))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void bamTrackGetTest() throws Exception {
        Resource resource = context.getResource(TEST_BAM);
        IndexedFileRegistrationRequest request = initRequest(resource.getFile().getAbsolutePath());

        ResultActions actions = mvc()
                .perform(post(BAM_FILE_REGISTER).content(getObjectMapper().writeValueAsString(request))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        final ResponseResult<BamFile> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                BamFile.class));

        BamFile bamFile = res.getPayload();

        // Load a track by fileId
        TrackQuery bamTrackQuery = initTrackQuery(res.getPayload().getId());
        BamQueryOption bamQueryOption = new BamQueryOption();
        bamQueryOption.setDownSampling(true);
        bamQueryOption.setShowSpliceJunction(true);
        bamQueryOption.setTrackDirection(TrackDirectionType.LEFT);
        bamTrackQuery.setOption(bamQueryOption);

        actions = mvc()
                .perform(post(BAM_TRACK_GET).content(getObjectMapper().writeValueAsString(bamTrackQuery))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<BamTrack<Read>> readSumFullRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(), getTypeFactory()
                        .constructParametrizedType(ResponseResult.class, ResponseResult.class, getTypeFactory()
                                .constructParametrizedType(BamTrack.class, BamTrack.class, Read.class)));

        Assert.assertFalse(readSumFullRes.getPayload().getBlocks().isEmpty());
        assertIsReadCorrect(readSumFullRes.getPayload().getBlocks().get(1));


        // Load a track by fileId full
        bamQueryOption.setTrackDirection(TrackDirectionType.MIDDLE);
        actions = mvc()
                .perform(post(BAM_TRACK_GET).content(getObjectMapper().writeValueAsString(bamTrackQuery))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        readSumFullRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(), getTypeFactory()
                        .constructParametrizedType(ResponseResult.class, ResponseResult.class, getTypeFactory()
                                .constructParametrizedType(BamTrack.class, BamTrack.class, Read.class)));

        Assert.assertFalse(readSumFullRes.getPayload().getBlocks().isEmpty());
        assertIsReadCorrect(readSumFullRes.getPayload().getBlocks().get(1));

        Read read = readSumFullRes.getPayload().getBlocks().get(0);

        // Load a track by fileId left
        bamQueryOption.setTrackDirection(TrackDirectionType.LEFT);
        actions = mvc()
                .perform(post(BAM_TRACK_GET).content(getObjectMapper().writeValueAsString(bamTrackQuery))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());
        ResponseResult<BamTrack<Read>> readSumLeftRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(), getTypeFactory()
                        .constructParametrizedType(ResponseResult.class, ResponseResult.class, getTypeFactory()
                                .constructParametrizedType(BamTrack.class, BamTrack.class, Read.class)));

        Assert.assertFalse(readSumLeftRes.getPayload().getBlocks().isEmpty());
        assertIsReadCorrect(readSumLeftRes.getPayload().getBlocks().get(1));

        // Load a track by fileId right
        bamQueryOption.setTrackDirection(TrackDirectionType.RIGHT);
        actions = mvc()
                .perform(post(BAM_TRACK_GET).content(getObjectMapper().writeValueAsString(bamTrackQuery))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<BamTrack<Read>> readSumRightRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(), getTypeFactory()
                        .constructParametrizedType(ResponseResult.class, ResponseResult.class, getTypeFactory()
                                .constructParametrizedType(BamTrack.class, BamTrack.class, Read.class)));

        Assert.assertFalse(readSumRightRes.getPayload().getBlocks().isEmpty());
        assertIsReadCorrect(readSumRightRes.getPayload().getBlocks().get(1));

        // load read

        ReadQuery query = new ReadQuery();
        query.setName(read.getName());
        query.setChromosomeId(testChromosome.getId());
        query.setStartIndex(read.getStartIndex());
        query.setEndIndex(read.getEndIndex());
        query.setId(bamFile.getId());

        actions = mvc()
            .perform(post(BAM_READ_LOAD).content(getObjectMapper().writeValueAsString(query))
                         .contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(status().isOk())
            .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Read> readRes = getObjectMapper()
            .readValue(actions.andReturn().getResponse().getContentAsByteArray(), getTypeFactory()
                .constructParametrizedType(ResponseResult.class, ResponseResult.class, Read.class));
        Assert.assertNotNull(readRes.getPayload());
        Assert.assertNotNull(readRes.getPayload().getName());
    }

    private void assertIsReadCorrect(Read read) {
        Assert.assertNotNull(read);
        Assert.assertNotNull(read.getStand());
        Assert.assertNotNull(read.getName());
        Assert.assertNotNull(read.getStartIndex());
        Assert.assertNotNull(read.getEndIndex());
        Assert.assertNotNull(read.getCigarString());
        Assert.assertNotNull(read.getReadGroup());
        Assert.assertFalse(read.getCigarString().isEmpty());
    }

    private IndexedFileRegistrationRequest initRequest(final String path) {
        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(path);
        request.setName(TEST_BAM);
        request.setIndexPath(request.getPath() + ".bai");
        request.setS3BucketId(null);
        request.setType(BiologicalDataItemResourceType.FILE);
        return request;
    }

    private TrackQuery initTrackQuery(final long fileId) {
        //get read
        TrackQuery bamTrackQuery = new TrackQuery();
        bamTrackQuery.setChromosomeId(testChromosome.getId());
        bamTrackQuery.setStartIndex(TEST_START_INDEX);
        bamTrackQuery.setEndIndex(TEST_END_INDEX);
        bamTrackQuery.setScaleFactor(1d);
        bamTrackQuery.setId(fileId);
        return bamTrackQuery;
    }
}
