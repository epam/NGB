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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.epam.catgenome.common.AbstractControllerTest;
import com.epam.catgenome.common.ResponseResult;
import com.epam.catgenome.controller.externaldb.ExternalDBController;
import com.epam.catgenome.controller.vo.externaldb.UniprotEntryVO;
import com.epam.catgenome.controller.vo.externaldb.ensemblevo.EnsemblEntryVO;
import com.epam.catgenome.manager.externaldb.EnsemblDataManager;
import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.epam.catgenome.manager.externaldb.ParameterNameValue;
import com.epam.catgenome.manager.externaldb.UniprotDataManager;

/**
 * Source:      ExternalDBControllerTest
 * Created:     30.09.16, 13:10
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class ExternalDBControllerTest extends AbstractControllerTest {
    private static final String URL_FETCH_ENSEMBL_DATA = "/restapi/externaldb/ensembl/%s/get";
    private static final String URL_FETCH_UNIPROT_DATA = "/restapi/externaldb/uniprot/%s/get";

    private static final long GENE_START = 74082933L;
    private static final long END = 74122525L;

    @Mock
    private HttpDataManager httpDataManager;

    @InjectMocks
    @Spy
    private EnsemblDataManager ensemblDataManager;

    @InjectMocks
    @Spy
    private UniprotDataManager uniprotDataManager;

    @InjectMocks
    @Spy
    private ExternalDBController dbControllerMock;

    @Mock
    private NCBIGeneManager ncbiGeneManager;

    @Autowired
    private ApplicationContext context;

    private MockMvc mvc;

    @Before
    public void setup() throws Exception {
        super.setup();
        MockitoAnnotations.initMocks(this);
        mvc = MockMvcBuilders.standaloneSetup(dbControllerMock).build();
        Assert.assertNotNull(ensemblDataManager);
        Assert.assertNotNull(uniprotDataManager);
    }

    @Test
    public void testFetchEnsemblData() throws Exception {
        // arrange
        String fetchRes = readFile("ensembl_lookup_id_ENSG00000106683.json");

        when(
                httpDataManager.fetchData(Mockito.anyString(), Mockito.any(ParameterNameValue[].class))
        ).thenReturn(fetchRes);

        // act
        ResultActions actions = mvc
                .perform(get(String.format(URL_FETCH_ENSEMBL_DATA, "rs7412"))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));

        ResponseResult<EnsemblEntryVO> res = getObjectMapper().readValue(actions.andReturn().getResponse()
                .getContentAsByteArray(), getTypeFactory().constructParametricType(ResponseResult.class,
                EnsemblEntryVO.class));

        EnsemblEntryVO ensemblEntryVO = res.getPayload();

        // assert
        Assert.assertNotNull(ensemblEntryVO);
        Assert.assertEquals("ENSG00000106683", ensemblEntryVO.getId());
        Assert.assertTrue(ensemblEntryVO.getStart().equals(GENE_START));
        Assert.assertTrue(ensemblEntryVO.getEnd().equals(END));
        Assert.assertEquals("GRCh38", ensemblEntryVO.getAssemblyName());
        Assert.assertEquals("LIMK1", ensemblEntryVO.getDisplayName());
        Assert.assertEquals("LIM domain kinase 1 [Source:HGNC Symbol;Acc:HGNC:6613]", ensemblEntryVO.getDescription());
    }

    @Test
    public void testFetchUniprotData() throws Exception {
        // arrange
        String fetchRes = readFile("uniprot_fetch_gene_ENSG00000106683.xml");

        when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(ParameterNameValue[].class))
        ).thenReturn(fetchRes);

        // act
        ResultActions actions = mvc
                .perform(get(String.format(URL_FETCH_UNIPROT_DATA, "ENSG00000106683"))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));

        ResponseResult<List<UniprotEntryVO>> res = getObjectMapper().readValue(actions.andReturn().getResponse()
                .getContentAsByteArray(), getTypeFactory().constructParametricType(ResponseResult.class,
                getTypeFactory().constructParametricType(List.class, UniprotEntryVO.class)));

        List<UniprotEntryVO> entryList = res.getPayload();

        // assert
        Assert.assertNotNull(entryList);
        Assert.assertEquals(2, entryList.size());

        UniprotEntryVO entryVO = entryList.get(0);
        Assert.assertNotNull(entryVO);
        Assert.assertEquals("LIMK1_HUMAN", entryVO.getName());
        Assert.assertEquals("LIMK1", entryVO.getGeneNames().get(0));
    }

    private String readFile(String filename) throws IOException {
        Resource resource = context.getResource("classpath:externaldb//data//" + filename);
        String pathStr = resource.getFile().getPath();
        String content = new String(Files.readAllBytes(Paths.get(pathStr)), Charset.defaultCharset());
        return content;
    }
}
