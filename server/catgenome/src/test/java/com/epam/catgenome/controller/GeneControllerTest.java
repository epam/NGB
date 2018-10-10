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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.epam.catgenome.controller.util.UrlTestingUtils;
import org.eclipse.jetty.server.Server;
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
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneHighLevel;
import com.epam.catgenome.entity.gene.GeneLowLevel;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.manager.gene.GeneFileManager;
import com.epam.catgenome.manager.reference.ReferenceManager;

/**
 * Source:      GeneControllerTest
 * Created:     05.12.15, 16:25
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class GeneControllerTest extends AbstractControllerTest {
    @Autowired
    ApplicationContext context;

    @Autowired
    private GeneFileManager geneFileManager;

    @Autowired
    private ReferenceManager referenceManager;

    private static final String URL_GENE_FILE_REGISTER = "/restapi/gene/register";
    private static final String URL_GENE_FILE_DELETE = "/restapi/secure/gene/register";
    private static final String URL_LOAD_GENES = "/restapi/gene/%d/track/get";
    private static final String URL_LOAD_GENES_HISTOGRAM = "/restapi/gene/track/histogram";
    private static final String URL_LOAD_GENE_FILES = "/restapi/gene/%d/loadAll";
    private static final String URL_GENE_NEXT = "/restapi/gene/%d/%d/next";
    private static final String URL_GENE_PREV = "/restapi/gene/%d/%d/prev";

    private long referenceId;

    private Reference testReference;
    private Chromosome testChromosome;

    @Before
    public void setup() throws Exception {
        super.setup();

        Resource resource = context.getResource("classpath:templates/A3.fa");

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName("a3");
        request.setPath(resource.getFile().getPath());
        request.setType(BiologicalDataItemResourceType.FILE);

        testReference = referenceManager.registerGenome(request);
        Assert.assertNotNull(testReference);
        testChromosome = testReference.getChromosomes().get(0);
        referenceId = testReference.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveLoadGeneByUri() throws Exception {
        Resource resource = context.getResource("classpath:templates/genes_sorted.gtf");
        Resource index = context.getResource("classpath:templates/genes_sorted.gtf.tbi");

        // Load a track by fileId
        TrackQuery trackQuery = initTrackQuery(1L);
        trackQuery.setId(null);

        ResultActions actions = mvc()
                .perform(post(String.format(URL_LOAD_GENES, testReference.getId()))
                        .content(getObjectMapper().writeValueAsString(trackQuery))
                        .param("fileUrl", resource.getFile().getAbsolutePath())
                        .param("indexUrl", index.getFile().getAbsolutePath())
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Track<GeneHighLevel>> geneRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(Track.class, Track.class,
                                        GeneHighLevel.class)));

        Assert.assertFalse(geneRes.getPayload().getBlocks().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveLoadGeneByUrl() throws Exception {
        String geneUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + "/genes_sorted.gtf";
        String indexUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + "/genes_sorted.gtf.tbi";
        Server server = UrlTestingUtils.getFileServer(context);

        try {
            server.start();

            // Load a track by fileId
            TrackQuery trackQuery = initTrackQuery(1L);
            trackQuery.setId(null);

            ResultActions actions = mvc()
                    .perform(post(String.format(URL_LOAD_GENES, testReference.getId()))
                            .content(getObjectMapper().writeValueAsString(trackQuery))
                            .param("fileUrl", geneUrl)
                            .param("indexUrl", indexUrl)
                            .contentType(EXPECTED_CONTENT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                    .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                    .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
            actions.andDo(MockMvcResultHandlers.print());

            ResponseResult<Track<GeneHighLevel>> geneRes = getObjectMapper()
                    .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                            getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                    getTypeFactory().constructParametrizedType(Track.class, Track.class,
                                            GeneHighLevel.class)));

            Assert.assertFalse(geneRes.getPayload().getBlocks().isEmpty());
        } finally {
            server.stop();
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveLoadGeneTestNew() throws Exception {
        Resource resource = context.getResource("classpath:templates/genes_sorted.gtf");

        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        ResultActions actions = mvc()
                .perform(post(URL_GENE_FILE_REGISTER).content(getObjectMapper().writeValueAsString(request))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        final ResponseResult<GeneFile> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                GeneFile.class));

        Assert.assertNotNull(res.getPayload().getId());
        Assert.assertEquals(res.getPayload().getName(), "genes_sorted.gtf");
        Long fileId = res.getPayload().getId();
        Assert.assertNotNull("Test chromosome is not saved", testChromosome.getId());

        // Load all GeneFiles for testReference
        actions = mvc()
                .perform(get(String.format(URL_LOAD_GENE_FILES, testReference.getId()))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<List<GeneFile>> geneFilesRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(List.class, List.class, GeneFile.class)));

        Assert.assertFalse(geneFilesRes.getPayload().isEmpty());
        Assert.assertEquals(geneFilesRes.getPayload().get(0).getId(), fileId);

        // Load a track by fileId
        TrackQuery trackQuery = initTrackQuery(fileId);

        actions = mvc()
                .perform(post(String.format(URL_LOAD_GENES, testReference.getId()))
                        .content(getObjectMapper().writeValueAsString(trackQuery))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Track<GeneHighLevel>> geneRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(Track.class, Track.class,
                                        GeneHighLevel.class)));

        Assert.assertFalse(geneRes.getPayload().getBlocks().isEmpty());

        // Test load histogram
        testLoadHistogram(fileId);

        // jump to next feature test
        List<GeneLowLevel> exons = new ArrayList<>();
        for (GeneHighLevel gene : geneRes.getPayload().getBlocks()) {
            if (gene.getItems() != null) {
                for (GeneLowLevel mRna : gene.getItems()) {
                    if (mRna.getItems() != null) {
                        exons.addAll(mRna.getItems().stream().filter(s -> "exon".equals(s.getFeature()))
                                .collect(Collectors.toList()));
                    }
                }
            }
        }
        Assert.assertFalse(exons.isEmpty());
        Collections.sort(exons, (o1, o2) -> o1.getStartIndex().compareTo(o2.getStartIndex()));
        int middle = exons.size() / 2;
        GeneLowLevel firstExon = exons.get(middle);
        GeneLowLevel secondExon = exons.get(middle + 1);

        actions = mvc()
                .perform(get(String.format(URL_GENE_NEXT, fileId, testChromosome.getId()))
                        .param("fromPosition", firstExon.getEndIndex().toString())
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk()).andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Gene> nextGeneRes = getObjectMapper().readValue(
                actions.andReturn().getResponse().getContentAsByteArray(),
                getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class, Gene.class));

        Assert.assertNotNull(nextGeneRes.getPayload());
        Assert.assertEquals(secondExon.getStartIndex(), nextGeneRes.getPayload().getStartIndex());
        Assert.assertEquals(secondExon.getEndIndex(), nextGeneRes.getPayload().getEndIndex());
        actions = mvc()
                .perform(get(String.format(URL_GENE_PREV, fileId, testChromosome.getId()))
                        .param("fromPosition", secondExon.getStartIndex().toString())
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk()).andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        nextGeneRes = getObjectMapper().readValue(
                actions.andReturn().getResponse().getContentAsByteArray(),
                getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class, Gene.class));

        Assert.assertNotNull(nextGeneRes.getPayload());
        Assert.assertEquals(firstExon.getStartIndex(), nextGeneRes.getPayload().getStartIndex());
        Assert.assertEquals(firstExon.getEndIndex(), nextGeneRes.getPayload().getEndIndex());

        // delete file
        actions = mvc()
                .perform(delete(URL_GENE_FILE_DELETE).param("geneFileId", geneFilesRes.getPayload().get(0).getId()
                        .toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        boolean failed = false;
        try {
            geneFileManager.load(geneFilesRes.getPayload().get(0).getId());
        } catch (IllegalArgumentException e) {
            failed = true;
        }
        Assert.assertTrue(failed);
    }

    private void testLoadHistogram(long fileId) throws Exception {
        TrackQuery histogramQuery = initTrackQuery(fileId);
        ResultActions actions = mvc().perform(post(URL_LOAD_GENES_HISTOGRAM).content(getObjectMapper()
                                                                           .writeValueAsString(histogramQuery))
                                    .contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(status().isOk())
            .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Track<Wig>> histogram = getObjectMapper()
            .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                       getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                                              getTypeFactory().constructParametrizedType(Track.class,
                                                                                                         Track.class,
                                                                                                         Wig.class)));

        Assert.assertFalse(histogram.getPayload().getBlocks().isEmpty());
        Assert.assertTrue(histogram.getPayload().getBlocks()
                          .stream()
                          .allMatch(b -> b.getStartIndex() != null && b.getEndIndex() != null && b.getValue() != null));
    }

    private TrackQuery initTrackQuery(final long fileId) {
        TrackQuery trackQuery = new TrackQuery();
        trackQuery.setChromosomeId(testChromosome.getId());
        trackQuery.setStartIndex(1);
        trackQuery.setEndIndex(testChromosome.getSize());
        trackQuery.setScaleFactor(1D);
        trackQuery.setId(fileId);
        return trackQuery;
    }
}
