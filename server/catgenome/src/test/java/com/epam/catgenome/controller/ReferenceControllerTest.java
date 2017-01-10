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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.unitils.reflectionassert.ReflectionAssert;

import com.epam.catgenome.common.AbstractControllerTest;
import com.epam.catgenome.common.ResponseResult;
import com.epam.catgenome.controller.util.ResultReference;
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.FileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Source:      ReferenceControllerTest.java
 * Created:     10/7/15, 2:00 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code ReferenceControllerTest} is used to test different REST API calls to the service, which
 * is responsible for references' management
 *
 * @author Mikhail Miroliubov
 * @author Denis Medvedev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class ReferenceControllerTest extends AbstractControllerTest {

    // describes Test2.fa resource properties
    private static final int NUMBER_CHROMOSOMES_IN_HP = 5;
    private static final String HP_GENOME_PATH = "/Test2.fa";
    private static final String PLAIN_GENOME_NAME = "Test2";
    private static final String HP_GENOME_NAME = "Harry Potter v1.0";

    // describes reference track query parameters
    private static final int END_INDEX = 120;
    private static final int START_INDEX = 100;
    private static final double SCALE_FACTOR = 1D;
    private static final String TEST_SEQUENCE_STRING = "ACCCTAACCCTAACCCCTAAC";

    // describes REST API that should be covered by this test
    private static final String LOAD_REFERENCE = "/reference/%s/load";
    private static final String LOAD_ALL_REFERENCES = "/reference/loadAll";
    private static final String GET_REFERENCE_TRACK = "/reference/track/get";
    private static final String LOAD_CHROMOSOME = "/reference/chromosomes/%s/load";
    private static final String LOAD_ALL_CHROMOSOMES = "/reference/%s/loadChromosomes";
    private static final String REGISTER_GENOME_IN_FASTA_FORMAT = "/secure/reference/register/fasta";
    private static final String UPDATE_REFERENCE_GENE_FILE = "/secure/reference/%d/genes";

    //describes GA4GH API Google genomic
    private static final String REFERENCE_SET_ID = "EJjur6DxjIa6KQ";

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GffManager gffManager;

    @Before
    @Override
    public void setup() throws Exception {
        Assert.assertNotNull("ReferenceGenomeManager isn't provided.", referenceGenomeManager);
        super.setup();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testReferenceGenomesManagement() throws Exception {
        // creates a new reference and saves its metadata
        final Reference source = EntityHelper.createReference();
        source.setId(referenceGenomeManager.createReferenceId());
        referenceGenomeManager.register(source);


        // 0. cleans up 'path' parameters, because they never should be sent to the client
        source.getChromosomes().stream().forEach(e -> e.setPath(null));
        source.setBucketId(null);

        // 1. load all references registered in the system, at least one reference should
        //    be returned
        ResultActions actions = mvc()
                .perform(get(LOAD_ALL_REFERENCES))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).isArray())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(print());

        // 2. load reference by the given ID
        actions = mvc()
                .perform(get(String.format(LOAD_REFERENCE, source.getId())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        final ResponseResult<Reference> res2 = parseReference(actions.andReturn().getResponse()
                .getContentAsByteArray());
        ReflectionAssert.assertReflectionEquals(source, res2.getPayload());
        actions.andDo(print());

        // 3. load chromosome by the given ID
        final Chromosome chrSource = source.getChromosomes().get(0);
        actions = mvc()
                .perform(get(String.format(LOAD_CHROMOSOME, chrSource.getId())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        final ResponseResult<Chromosome> res3 = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                Chromosome.class));
        ReflectionAssert.assertReflectionEquals(chrSource, res3.getPayload());
        actions.andDo(print());

        // 4. load all chromosomes that belong to reference with the given ID
        actions = mvc()
                .perform(get(String.format(LOAD_ALL_CHROMOSOMES, source.getId())))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).isArray())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(print());
    }

    @Ignore //TODOL fix
    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testUnregister() throws Exception {
        // Register.
        // creates a new referenceResult and saves its metadata
        final Reference source = EntityHelper.createReference();
        source.setId(referenceGenomeManager.createReferenceId());
        referenceGenomeManager.register(source);


        // 0. cleans up 'path' parameters, because they never should be sent to the client
        source.getChromosomes().stream().forEach(e -> e.setPath(null));
        source.setPath(null);
        source.setBucketId(null);

        // 1. load all references registered in the system, at least one referenceResult should
        //    be returned
        MvcResult result = mvc()
                .perform(get(LOAD_ALL_REFERENCES))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).isArray())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()))
                .andReturn();
        String response = result.getResponse().getContentAsString();
        ResultReference referenceResult = objectMapper.readValue(response, ResultReference.class);
        Assert.assertNotNull(referenceResult);
        Assert.assertTrue(referenceResult.getPayload().size() > 0);

        referenceGenomeManager.unregister(referenceResult.getPayload().get(0));
        result = mvc()
                .perform(get(LOAD_ALL_REFERENCES))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andReturn();
        response = result.getResponse().getContentAsString();
        referenceResult = objectMapper.readValue(response, ResultReference.class);
        Assert.assertNotNull(referenceResult);
        Assert.assertNull(referenceResult.getPayload());
    }

    @Ignore
    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveAndGetTrackDataGA4GH() throws Exception {

        FileRegistrationRequest request;
        ResultActions actions;
        // 1. tries to save a genome with all parameters
        request = new FileRegistrationRequest();
        request.setPath(REFERENCE_SET_ID);
        request.setType(BiologicalDataItemResourceType.GA4GH);
        request.setName(PLAIN_GENOME_NAME);

        actions = mvc()
                .perform(post(REGISTER_GENOME_IN_FASTA_FORMAT).content(getObjectMapper().writeValueAsString(request))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        final Reference ref2 = parseReference(actions.andReturn().getResponse().getContentAsByteArray()).getPayload();
        Assert.assertNotNull("Genome ID shouldn't be null.", ref2.getId());
        Assert.assertEquals("Unexpected auto-generated name for a genome.", PLAIN_GENOME_NAME, ref2.getName());
        actions.andDo(print());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveAndGetTrackData() throws Exception {
        Resource resource;
        ResultActions actions;
        ReferenceRegistrationRequest request;

        // 1. tries to save a genome without the given user-friendly name, a name should be
        //    generated from the original filename by cutting its extension
        resource = getTemplateResource(HP_GENOME_PATH);

        request = new ReferenceRegistrationRequest();
        request.setPath(resource.getFile().getAbsolutePath());

        actions = mvc()
                .perform(post(REGISTER_GENOME_IN_FASTA_FORMAT).content(getObjectMapper().writeValueAsString(request))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        final Reference ref1 = parseReference(actions.andReturn().getResponse().getContentAsByteArray()).getPayload();
        Assert.assertNotNull("Genome ID shouldn't be null.", ref1.getId());
        Assert.assertEquals("Unexpected auto-generated name for a genome.", PLAIN_GENOME_NAME, ref1.getName());
        Assert.assertEquals("Unexpected number of chromosomes.", NUMBER_CHROMOSOMES_IN_HP,
                            ref1.getChromosomes().size());
        actions.andDo(print());

        // 2. tries to save a genome with the specified user-friendly name
        resource = getTemplateResource(HP_GENOME_PATH);
        request = new ReferenceRegistrationRequest();
        request.setName(HP_GENOME_NAME);
        request.setPath(resource.getFile().getAbsolutePath());
        actions = mvc()
                .perform(post(REGISTER_GENOME_IN_FASTA_FORMAT).content(getObjectMapper().writeValueAsString(request))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        final Reference ref2 = parseReference(actions.andReturn().getResponse().getContentAsByteArray()).getPayload();
        Assert.assertNotNull("Genome ID shouldn't be null.", ref1.getId());
        Assert.assertEquals("Unexpected user-friendly name for a genome.", HP_GENOME_NAME, ref2.getName());
        Assert.assertEquals("Unexpected number of chromosomes.", NUMBER_CHROMOSOMES_IN_HP,
                            ref2.getChromosomes().size());
        actions.andDo(print());

        // 3. tries to get information to fill in a track for 'ref2' genome
        final TrackQuery refTrackQuery = new TrackQuery();
        refTrackQuery.setId(ref2.getId());
        refTrackQuery.setChromosomeId(ref2.getChromosomes().iterator().next().getId());
        refTrackQuery.setScaleFactor(SCALE_FACTOR);
        refTrackQuery.setStartIndex(START_INDEX);
        refTrackQuery.setEndIndex(END_INDEX);
        actions = mvc()
                .perform(post(GET_REFERENCE_TRACK).content(getObjectMapper().writeValueAsString(refTrackQuery))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        final ResponseResult<Track<Sequence>> result = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsString(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(Track.class, Track.class, Sequence.class)));
        List<Sequence> resultSequence = result.getPayload().getBlocks();
        Assert.assertFalse(resultSequence.isEmpty());
        for (Sequence sequence : resultSequence) {
            Assert.assertTrue(String.valueOf(TEST_SEQUENCE_STRING.charAt(sequence.getStartIndex() - START_INDEX))
                    .equals(sequence.getText()));
        }
        actions.andDo(print());

    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testUpdateReferenceGeneFile() throws Exception {
        Resource resource = getTemplateResource(HP_GENOME_PATH);
        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName(HP_GENOME_NAME);
        request.setPath(resource.getFile().getAbsolutePath());
        ResultActions actions = mvc()
            .perform(post(REGISTER_GENOME_IN_FASTA_FORMAT).content(getObjectMapper().writeValueAsString(request))
                         .contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        final Reference ref2 = parseReference(actions.andReturn().getResponse().getContentAsByteArray()).getPayload();
        Assert.assertNotNull(ref2.getId());

        FeatureIndexedFileRegistrationRequest geneRequest = new FeatureIndexedFileRegistrationRequest();
        resource = getTemplateResource("genes_sorted.gtf");
        geneRequest.setPath(resource.getFile().getAbsolutePath());
        geneRequest.setReferenceId(ref2.getId());

        GeneFile geneFile = gffManager.registerGeneFile(geneRequest);

        actions = mvc()
            .perform(put(String.format(UPDATE_REFERENCE_GENE_FILE, ref2.getId())).param("geneFileId",
                                                                                        geneFile.getId().toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));

        final Reference updatedRef = parseReference(actions.andReturn().getResponse().getContentAsByteArray())
            .getPayload();
        Assert.assertNotNull(updatedRef.getId());
        Assert.assertNotNull(updatedRef.getGeneFile());
        Assert.assertNotNull(updatedRef.getGeneFile().getId());
    }

    private ResponseResult<Reference> parseReference(final byte[] response) throws IOException {
        return getObjectMapper()
                .readValue(response, getTypeFactory().constructParametrizedType(ResponseResult.class,
                        ResponseResult.class, Reference.class));
    }

}