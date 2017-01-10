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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

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

import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.controller.vo.externaldb.NCBIGeneVO;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIAuxiliaryManager;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneManager;
import com.epam.catgenome.manager.externaldb.ncbi.parser.NCBIGeneInfoParser;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBIDatabase;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created: 4/12/2016
 * Project: CATGenome Browser
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class NCBIGeneManagerTest {
    @Autowired
    private NCBIGeneInfoParser geneInfoParser;

    @Autowired
    private ApplicationContext context;

    @Mock
    private NCBIAuxiliaryManager ncbiAuxiliaryManager;

    @InjectMocks
    @Spy
    private NCBIGeneManager ncbiGeneManager;

    public static final int EXPECTED_INTERACTIONS_SIZE = 38;

    public static final int EXPECTED_PUBMED_SIZE = 5;

    public static final int EXPECTED_BIOSYSTEMS_SIZE = 20;

    public static final String TEST_GENE_ID = "3985";

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGeneFetch()
            throws IOException, IllegalAccessException, ExternalDbUnavailableException, InterruptedException {

        ncbiGeneManager.setGeneInfoParser(geneInfoParser);

        // arrange

        JsonMapper mapper = new JsonMapper();

        String fetchRes = readFile("ncbi_fetch_gene_3985.xml");
        String biosystemsRes = readFile("ncbi_link_gene_biosystems_3985.xml");
        String pubmedRes = readFile("ncbi_link_gene_pubmed_3985.xml");
        String homologsRes = readFile("ncbi_link_gene_homologene_3985.xml");
        String biosystemsSummary = readFile("ncbi_summary_biosystems_gene_3985.json");
        String pubmedSummary = readFile("ncbi_summary_pubmed_gene_3985.json");
        String homologsSummary = readFile("ncbi_summary_homologene_gene_3985.json");

        JsonNode biosystemsJson = mapper.readTree(biosystemsSummary);
        JsonNode pubmedJson = mapper.readTree(pubmedSummary);
        JsonNode homologJson = mapper.readTree(homologsSummary);

        //Mockito.when(ncbiAuxiliaryManager.searchDbForId(NCBIDatabase.GENE.name(), "")).thenReturn(historyQueryStr);

        Mockito.when(ncbiAuxiliaryManager
                .fetchXmlById(NCBIDatabase.GENE.name(), TEST_GENE_ID, null)).thenReturn(fetchRes);

        Mockito.when(ncbiAuxiliaryManager.link(TEST_GENE_ID, NCBIDatabase.GENE.name(),
                NCBIDatabase.PUBMED.name(), "gene_pubmed")).thenReturn(pubmedRes);

        Mockito.when(ncbiAuxiliaryManager.link(TEST_GENE_ID, NCBIDatabase.GENE.name(),
                NCBIDatabase.BIOSYSTEMS.name(), "gene_biosystems")).thenReturn(biosystemsRes);

        Mockito.when(ncbiAuxiliaryManager.link(TEST_GENE_ID, NCBIDatabase.GENE.name(),
                NCBIDatabase.HOMOLOGENE.name(), "gene_homologene")).thenReturn(homologsRes);

        Mockito.when(ncbiAuxiliaryManager.summaryWithHistory("1",
                "NCID_1_23738595_165.112.9.28_9001_1460538915_1245746636_0MetA0_S_MegaStore_F_1"))
                .thenReturn(biosystemsJson);

        Mockito.when(ncbiAuxiliaryManager.summaryWithHistory("1",
                "NCID_1_25615458_165.112.9.37_9001_1460538774_1139714798_0MetA0_S_MegaStore_F_1"))
                .thenReturn(pubmedJson);

        Mockito.when(ncbiAuxiliaryManager.summaryWithHistory("1",
                "NCID_1_773793_130.14.18.34_9001_1482423535_1399953144_0MetA0_S_MegaStore_F_1"))
                .thenReturn(homologJson);

        // act
        NCBIGeneVO ncbiGeneVO = ncbiGeneManager.fetchGeneById(TEST_GENE_ID);
        // assert
        Assert.assertNotNull(ncbiGeneVO);
        Assert.assertEquals("human", ncbiGeneVO.getOrganismCommon());
        Assert.assertEquals("Homo sapiens", ncbiGeneVO.getOrganismScientific());
        Assert.assertEquals("HGNC:6614", ncbiGeneVO.getPrimarySource());
        Assert.assertEquals("protein-coding", ncbiGeneVO.getGeneType());
        Assert.assertEquals("REVIEWED", ncbiGeneVO.getRefSeqStatus());
        Assert.assertEquals("LIMK2", ncbiGeneVO.getOfficialSymbol());
        Assert.assertEquals("LIM domain kinase 2", ncbiGeneVO.getOfficialFullName());
        Assert.assertEquals("HGNC", ncbiGeneVO.getPrimarySourcePrefix());
        Assert.assertNotNull(ncbiGeneVO.getGeneSummary());
        Assert.assertTrue(ncbiGeneVO.getGeneSummary().startsWith("There are approximately 40"));
        Assert.assertEquals(EXPECTED_INTERACTIONS_SIZE, ncbiGeneVO.getInteractions().size());
        Assert.assertEquals(EXPECTED_PUBMED_SIZE, ncbiGeneVO.getPubmedReferences().size());
        Assert.assertEquals(EXPECTED_BIOSYSTEMS_SIZE, ncbiGeneVO.getBiosystemsReferences().size());

    }

    private String readFile(String filename) throws IOException {
        Resource resource = context.getResource("classpath:externaldb//data//" + filename);
        String pathStr = resource.getFile().getPath();
        String content = new String(Files.readAllBytes(Paths.get(pathStr)), Charset.defaultCharset());
        return content;
    }

}
