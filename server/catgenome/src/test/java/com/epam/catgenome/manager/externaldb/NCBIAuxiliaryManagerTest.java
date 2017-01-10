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

package com.epam.catgenome.manager.externaldb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.controller.vo.externaldb.NCBITaxonomyVO;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIAuxiliaryManager;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBIDatabase;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Source:      NCBIAuxiliaryManagerTest
 * Created:     04.10.16, 17:20
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class NCBIAuxiliaryManagerTest {
    @Spy
    private NCBIAuxiliaryManager ncbiAuxiliaryManager;

    @Autowired
    private ApplicationContext context;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSearchDbForId() throws InterruptedException, IOException, ExternalDbUnavailableException {
        Mockito.doReturn("{\"esearchresult\":{\"idlist\":[99464]}}").when(ncbiAuxiliaryManager).fetchData(Mockito
                .anyString(), Mockito.any(ParameterNameValue[].class));

        String id = ncbiAuxiliaryManager.searchDbForId("testDb", "test");
        assertEquals(id, "99464");
    }

    @Test
    public void testFetchTaxonomyInfoById() throws Exception {
        String taxonomyJson = readFile("ncbi_summary_taxonomy_9606.json");

        Mockito.doReturn(taxonomyJson).when(ncbiAuxiliaryManager).fetchData(Mockito
                .anyString(), Mockito.any(ParameterNameValue[].class));

        NCBITaxonomyVO taxonomyVO = ncbiAuxiliaryManager.fetchTaxonomyInfoById("9606");
        assertNotNull(taxonomyVO);
    }

    @Test
    public void testSearchWithHistory() throws Exception {
        String historyQueryStr = readFile("ncbi_search_snp_variations_on_region.xml");

        Mockito.doReturn(historyQueryStr).when(ncbiAuxiliaryManager).fetchData(Mockito
                .anyString(), Mockito.any(ParameterNameValue[].class));

        String query = ncbiAuxiliaryManager.searchWithHistory("testDb", "test", 1);
        assertEquals(historyQueryStr, query);
    }

    @Test
    public void testFetchAnnotationInfoById() throws Exception {
        String clinvarJson = readFile("ncbi_summary_clinvar_7412.json");
        Mockito.doReturn(clinvarJson).when(ncbiAuxiliaryManager).fetchData(Mockito
                .anyString(), Mockito.any(ParameterNameValue[].class));

        JsonNode node = ncbiAuxiliaryManager.fetchAnnotationInfoById("99464");
        assertEquals(jsonResultParse(clinvarJson), node);
    }

    @Test
    public void testFetchWithHistory() throws Exception {
        String ncbiFetchResponseStr = readFile("ncbi_fetch_snp_variations_on_region.xml");

        Mockito.doReturn(ncbiFetchResponseStr).when(ncbiAuxiliaryManager).fetchData(Mockito
                .anyString(), Mockito.any(ParameterNameValue[].class));

        String history = ncbiAuxiliaryManager.fetchWithHistory("testDb", "test", NCBIDatabase.ASSEMBLY);
        assertEquals(ncbiFetchResponseStr, history);
    }

    private String readFile(String filename) throws IOException {
        Resource resource = context.getResource("classpath:externaldb//data//" + filename);
        String pathStr = resource.getFile().getPath();
        return new String(Files.readAllBytes(Paths.get(pathStr)), Charset.defaultCharset());
    }

    private JsonNode jsonResultParse(String srcJson) throws IOException {
        JsonNode root = new JsonMapper().readTree(srcJson).path("result");
        Iterator<JsonNode> uids = root.path("uids").iterator();

        if (uids.hasNext()) {
            int uid = uids.next().asInt();
            root = root.path("" + uid);
        }

        return root;
    }
}