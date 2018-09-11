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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jetty.server.Server;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.common.AbstractControllerTest;
import com.epam.catgenome.common.ResponseResult;
import com.epam.catgenome.controller.util.UrlTestingUtils;
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.controller.vo.VcfTrackQuery;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.project.ProjectItem;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationQuery;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.FeatureIndexManager;
import com.epam.catgenome.manager.project.ProjectManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.util.TestUtils;

/**
 * Source:      VcfControllerTest.java
 * Created:     30/10/15, 2:03 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * A test class for VcfController
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class VcfControllerTest extends AbstractControllerTest {

    private static final String URL_LOAD_VARIATIONS = "/restapi/vcf/track/get";
    private static final String URL_LOAD_VARIATION_INFO = "/restapi/vcf/variation/load";
    private static final String URL_LOAD_VCF_FILES = "/restapi/vcf/%d/loadAll";
    private static final String URL_LOAD_VCF_FILTERS = "/restapi/vcf/%d/fieldInfo";
    private static final String URL_VCF_NEXT_VARIATION = "/restapi/vcf/%d/next";
    private static final String URL_VCF_PREV_VARIATION = "/restapi/vcf/%d/prev";

    private static final String VCF_FILE_REGISTER = "/restapi/vcf/register";

    @Autowired
    ApplicationContext context;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private FeatureIndexManager featureIndexManager;

    private static final int TEST_END_INDEX = 239107476;
    private static final int TES_CHROMOSOME_SIZE = 239107476;

    private long referenceId;
    private Reference testReference;
    private Chromosome testChromosome;

    @Before
    public void setup() throws Exception {
        super.setup();

        testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TES_CHROMOSOME_SIZE);
        testReference = EntityHelper.createNewReference(testChromosome, referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(testReference);
        referenceId = testReference.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveLoadVcfTest() throws Exception {
        Resource resource = context.getResource("classpath:templates/samples.vcf");

        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        ResultActions actions = mvc()
                .perform(post(VCF_FILE_REGISTER).content(getObjectMapper().writeValueAsString(request))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        final ResponseResult<VcfFile> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                VcfFile.class));

        Assert.assertNotNull(res.getPayload().getId());
        Assert.assertEquals(res.getPayload().getName(), "samples.vcf");
        Long fileId = res.getPayload().getId();

        Assert.assertNotNull("Test chromosome is not saved", testChromosome.getId());

        // Load all VcfFiles for testReference
        actions = mvc()
                .perform(get(String.format(URL_LOAD_VCF_FILES, testReference.getId()))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<List<VcfFile>> vcfFilesRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(List.class, List.class, VcfFile.class)));

        Assert.assertNotNull(vcfFilesRes.getPayload());
        Assert.assertFalse(vcfFilesRes.getPayload().isEmpty());
        Assert.assertEquals(vcfFilesRes.getPayload().get(0).getId(), fileId);

        // Load a track by fileId

        VcfTrackQuery vcfTrackQuery = new VcfTrackQuery();

        vcfTrackQuery.setChromosomeId(testChromosome.getId());
        vcfTrackQuery.setStartIndex(1);
        vcfTrackQuery.setEndIndex(TEST_END_INDEX);
        vcfTrackQuery.setScaleFactor(1d);
        vcfTrackQuery.setSampleId(vcfFilesRes.getPayload().get(0).getSamples().get(0).getId());
        vcfTrackQuery.setId(fileId);

        MvcResult mvcResult = mvc()
                .perform(post(URL_LOAD_VARIATIONS).content(getObjectMapper().writeValueAsString(vcfTrackQuery))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(request().asyncStarted())
                .andReturn();

        actions = mvc()
                .perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Track<Variation>> vcfRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(Track.class, Track.class, Variation.class)));

        Assert.assertFalse(vcfRes.getPayload().getBlocks().isEmpty());

        Assert.assertNull(vcfRes.getPayload().getBlocks().get(0).getInfo());
        Assert.assertNull(vcfRes.getPayload().getBlocks().get(0).getGenotypeData().getInfo());

        // Load variation extended info
        final VariationQuery query = new VariationQuery();
        query.setId(fileId);
        query.setChromosomeId(testChromosome.getId());
        query.setPosition(vcfRes.getPayload().getBlocks().get(0).getStartIndex());
        query.setSampleId(vcfFilesRes.getPayload().get(0).getSamples().get(0).getId());

        actions = mvc()
                .perform(post(URL_LOAD_VARIATION_INFO)
                        .contentType(EXPECTED_CONTENT_TYPE)
                        .content(getObjectMapper().writeValueAsBytes(query)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Variation> variationExtendedRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(), getTypeFactory()
                        .constructParametrizedType(ResponseResult.class, ResponseResult.class, Variation.class));

        Assert.assertNotNull(variationExtendedRes.getPayload());
        Assert.assertFalse(variationExtendedRes.getPayload().getInfo().isEmpty());
        Assert.assertFalse(variationExtendedRes.getPayload().getGenotypeData().getInfo().isEmpty());

        // jump to next/prev
        testJump(fileId, vcfFilesRes, vcfRes);

        actions = mvc()
                .perform(get(String.format(URL_LOAD_VCF_FILTERS, fileId))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<VcfFilterInfo> vcfFiltersRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                VcfFilterInfo.class));


        Assert.assertNotNull(vcfFiltersRes);
        Assert.assertNotNull(vcfFiltersRes.getPayload());
        Assert.assertFalse(vcfFiltersRes.getPayload().getInfoItems().isEmpty());
        Assert.assertFalse(vcfFiltersRes.getPayload().getAvailableFilters().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testNotIndex() throws Exception {
        Resource resource = context.getResource("classpath:templates/Felis_catus.vcf");

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setDoIndex(false);
        request.setPath(resource.getFile().getAbsolutePath());

        ResultActions actions = mvc()
            .perform(post(VCF_FILE_REGISTER).content(getObjectMapper().writeValueAsString(request))
                         .contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(status().isOk())
            .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        final ResponseResult<VcfFile> res = getObjectMapper()
            .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                       getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                                                  VcfFile.class));

        VcfFile vcfFile = res.getPayload();

        Project project = new Project();
        project.setName("testProject");
        project.setItems(Arrays.asList(new ProjectItem(new BiologicalDataItem(vcfFile.getBioDataItemId())),
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));

        projectManager.saveProject(project); // Index is created when vcf file is added

        TestUtils.assertFail(() -> featureIndexManager.filterVariations(new VcfFilterForm(), project.getId()),
                             Collections.singletonList(IllegalArgumentException.class));
    }

    @Test
    @Ignore
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testUrlNotRegistered() throws Exception {
        final String path = "/Felis_catus.vcf";
        String vcfUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + path;
        String indexUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + "/Felis_catus.idx";

        Server server = UrlTestingUtils.getFileServer(context);
        try {
            server.start();

            TrackQuery vcfTrackQuery = new TrackQuery();
            vcfTrackQuery.setChromosomeId(testChromosome.getId());
            vcfTrackQuery.setStartIndex(1);
            vcfTrackQuery.setEndIndex(TEST_END_INDEX);
            vcfTrackQuery.setScaleFactor(1D);

            ResultActions actions = mvc()
                .perform(post(URL_LOAD_VARIATIONS).content(getObjectMapper().writeValueAsString(vcfTrackQuery))
                             .param("fileUrl", vcfUrl).param("indexUrl", indexUrl)
                             .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
            actions.andDo(MockMvcResultHandlers.print());

            ResponseResult<Track<Variation>> vcfRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                           getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                                  getTypeFactory().constructParametrizedType(Track.class, Track.class,
                                                                                             Variation.class)));

            Assert.assertFalse(vcfRes.getPayload().getBlocks().isEmpty());
        } finally {
            server.stop();
        }
    }

    private void testJump(Long fileId, ResponseResult<List<VcfFile>> vcfFilesRes,
                          ResponseResult<Track<Variation>> vcfRes) throws Exception {
        int middle = vcfRes.getPayload().getBlocks().size() / 2;
        Variation var1 = vcfRes.getPayload().getBlocks().get(middle);
        Variation var2 = vcfRes.getPayload().getBlocks().get(middle + 1);

        ResultActions actions = mvc()
                .perform(get(String.format(URL_VCF_NEXT_VARIATION, testChromosome.getId()))
                             .param("trackId", fileId.toString())
                             .param("fromPosition", var1.getEndIndex().toString())
                        .param("sampleId", vcfFilesRes.getPayload().get(0).getSamples().get(0).getId().toString())
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Variation> nextVarRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                Variation.class));

        Assert.assertNotNull(nextVarRes.getPayload());
        Assert.assertEquals(var2.getStartIndex(), nextVarRes.getPayload().getStartIndex());
        Assert.assertEquals(var2.getEndIndex(), nextVarRes.getPayload().getEndIndex());

        actions = mvc()
                .perform(get(String.format(URL_VCF_PREV_VARIATION, testChromosome.getId()))
                             .param("trackId", fileId.toString())
                             .param("fromPosition", var2.getStartIndex().toString())
                        .param("sampleId", vcfFilesRes.getPayload().get(0).getSamples().get(0).getId().toString())
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        nextVarRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                Variation.class));

        Assert.assertNotNull(nextVarRes.getPayload());
        Assert.assertEquals(var1.getStartIndex(), nextVarRes.getPayload().getStartIndex());
        Assert.assertEquals(var1.getEndIndex(), nextVarRes.getPayload().getEndIndex());
    }
}
