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

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.unitils.reflectionassert.ReflectionAssert;

import com.epam.catgenome.common.AbstractControllerTest;
import com.epam.catgenome.common.ResponseResult;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.cytoband.Cytoband;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.helper.FileTemplates;



/**
 * Source:      CytobandControllerTest.java
 * Created:     10/20/15, 5:36 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code CytobandControllerTest} is used to test different REST API calls to the service, which
 * is responsible for cytobands management.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class CytobandControllerTest extends AbstractControllerTest {

    // describes REST API that should be covered by this test
    private static final String LOAD_CYTOBANDS = "/cytobands/%s/get";
    private static final String SAVE_CYTOBANDS = "/cytobands/upload";

    // describes expected test parameters
    private static final int CHR_NUMBER_OF_BANDS = 3;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testCytobandsManagement() throws Exception {
        Chromosome chromosome;
        ResultActions actions;
        MockMultipartHttpServletRequestBuilder builder;

        // creates a corresponded test genome 'classpath:templates/reference/hp.genome.fa'
        final Reference reference = createGenome();

        // 1. tries to save cytobands corresponded to the reference registered above
        final Resource resource = getTemplateResource(FileTemplates.HP_CYTOBANDS.getPath());
        final MockMultipartFile multipartFile = new MockMultipartFile(UPLOAD_FILE_PARAM, resource.getFilename(),
                null, resource.getInputStream());
        builder = MockMvcRequestBuilders.fileUpload(SAVE_CYTOBANDS);
        actions = mvc()
                .perform(builder
                        .file(multipartFile)
                        .param(REFERENCE_ID_PARAM, String.valueOf(reference.getId()))
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_MESSAGE).value(getMessage("info.cytobands.upload.done",
                        resource.getFilename())));
        actions.andDo(MockMvcResultHandlers.print());

        // 2. tries to get a track with cytobands for a particular chromosome
        chromosome = reference.getChromosomes().stream().filter(e -> e.getName().equals(CHR_A1))
                .findAny().get();
        actions = mvc()
                .perform(get(String.format(LOAD_CYTOBANDS, chromosome.getId())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        ResponseResult<Track<Cytoband>> result = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(Track.class, Track.class, Cytoband.class)));
        final Track<Cytoband> track = result.getPayload();
        Assert.assertTrue("Unexpected number of bands.", CHR_NUMBER_OF_BANDS == track.getBlocks().size());
        Assert.assertTrue("Unexpected a beginning position for a track.", 1 == track.getStartIndex());
        ReflectionAssert.assertReflectionEquals("Unexpected chromosome.", chromosome, track.getChromosome());
        Assert.assertTrue("Unexpected an ending position for a track.", track.getEndIndex().equals(
            chromosome.getSize()));
        actions.andDo(MockMvcResultHandlers.print());

        // 3. tries to get a track for a chromosome without cytobands
        chromosome = reference.getChromosomes().stream().filter(e -> e.getName().equals(CHR_A5))
                .findAny().get();
        actions = mvc()
                .perform(get(String.format(LOAD_CYTOBANDS, chromosome.getId())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.WARN.name()));
        actions.andDo(MockMvcResultHandlers.print());
    }

}
