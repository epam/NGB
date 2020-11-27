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

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

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
import com.epam.catgenome.controller.vo.externaldb.NCBIClinVarVO;
import com.epam.catgenome.controller.vo.externaldb.NCBIShortVarVO;
import com.epam.catgenome.controller.vo.externaldb.NCBITaxonomyVO;
import com.epam.catgenome.controller.vo.externaldb.NCBIVariationVO;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIAuxiliaryManager;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIClinVarManager;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIShortVarManager;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBIDatabase;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created: 4/12/2016
 * Project: CATGenome Browser
 *
 * @author Valeri Iakovlev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class NCBIShortVarManagerTest {

    JsonMapper mapper = new JsonMapper();

    @Autowired
    private ApplicationContext context;

    @Mock
    private NCBIAuxiliaryManager ncbiAuxiliaryManager;

    @InjectMocks
    @Spy
    private NCBIShortVarManager ncbiShortVarManager;

    @InjectMocks
    @Spy
    private NCBIClinVarManager ncbiClinVarManager;

//    @InjectMocks
//    @Spy
//    private NCBIStructVarManager ncbiStructVarManager;

    private static final int EXPECTED_VARIATION_RS_ID = 7412;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testStructVar()
            throws IOException, IllegalAccessException, ExternalDbUnavailableException, InterruptedException {
        // TODO
    }

    @Test
    public void testAggregatedVariationFetch()
            throws IOException, IllegalAccessException, ExternalDbUnavailableException, InterruptedException {

        // arrange
        ncbiShortVarManager.setNcbiClinVarManager(ncbiClinVarManager);

        String summaryJson = readFile("ncbi_summary_snp_rs7412.json");
        String fasta = readFile("ncbi_fetch_snp_variation_7421_fasta.txt");
        String fetched = readFile("ncbi_fetch_snp_variation_7412.xml");

        JsonNode root = jsonResultParse(summaryJson);

        Mockito.when(ncbiAuxiliaryManager.
                summaryEntityById(NCBIDatabase.SNP.name(), EXPECTED_VARIATION_RS_ID + "")).thenReturn(root);
        Mockito.when(ncbiAuxiliaryManager.
                fetchTextById(NCBIDatabase.SNP.name(), EXPECTED_VARIATION_RS_ID + "", "fasta")).thenReturn(fasta);
        Mockito.when(ncbiAuxiliaryManager.
                fetchXmlById(NCBIDatabase.SNP.name(), EXPECTED_VARIATION_RS_ID + "", null)).thenReturn(fetched);
        Mockito.when(ncbiAuxiliaryManager.
                getMapper()).thenReturn(mapper);

        //Mockito.when(ncbiAuxiliaryManager.searchDbForId("annotinfo", contigLabel))

        Mockito.when(ncbiAuxiliaryManager.searchDbForId(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn("99464");

        String annotInfoJson = readFile("ncbi_summary_annotinfo_99464.json");
        String clinvarJson = readFile("ncbi_summary_clinvar_7412.json");
        String taxonomyJson = readFile("ncbi_summary_taxonomy_9606.json");

        JsonNode jsonNode = jsonResultParse(annotInfoJson);
        JsonNode clinvarJsonNode = jsonResultParse(clinvarJson);
        JsonNode taxonomyJsonNode = jsonResultParse(taxonomyJson);
        NCBITaxonomyVO ncbiTaxonomyVO = mapper.treeToValue(taxonomyJsonNode, NCBITaxonomyVO.class);


        Mockito.when(ncbiAuxiliaryManager.fetchAnnotationInfoById("99464")).thenReturn(jsonNode);
        Mockito.when(ncbiAuxiliaryManager.summaryEntityById(NCBIDatabase.CLINVAR.name(), "7412"))
                .thenReturn(clinvarJsonNode);
        Mockito.when(ncbiAuxiliaryManager.fetchTaxonomyInfoById("9606"))
                .thenReturn(ncbiTaxonomyVO);

        // act
        NCBIVariationVO ncbiVariationVO =
                ncbiShortVarManager.fetchAggregatedVariationById(EXPECTED_VARIATION_RS_ID + "");

        // assert
        Assert.assertNotNull(ncbiVariationVO);
        ncbiVariationVO.getAlleles();
        Assert.assertEquals("GRCh38.p2", ncbiVariationVO.getAssemblyName());
        Assert.assertEquals("APOE (348)", ncbiVariationVO.getGene());
        Assert.assertEquals("1000GENOMES", ncbiVariationVO.getMafSource());
        Assert.assertEquals("human (Homo sapiens)", ncbiVariationVO.getOrganism());
        Assert.assertEquals("NG_007084.2", ncbiVariationVO.getRefSeqGeneMapping().getRefSeqGene());
        Assert.assertEquals("8041", ncbiVariationVO.getRefSeqGeneMapping().getPosition());
        Assert.assertEquals("APOE:348", ncbiVariationVO.getRefSeqGeneMapping().getGene());
        Assert.assertEquals("C", ncbiVariationVO.getRefSeqGeneMapping().getAllele());

        Assert.assertNotNull(ncbiVariationVO.getNcbiTaxonomy());
    }

    @Test
    public void testClinVar()
            throws IOException, IllegalAccessException, ExternalDbUnavailableException, InterruptedException {

        // arrange
        String clinvarRes = readFile("ncbi_summary_clinvar_variation_7412.json");

        JsonNode root = jsonResultParse(clinvarRes);

        Mockito.when(ncbiAuxiliaryManager.summaryEntityById(NCBIDatabase.CLINVAR.name(),
                EXPECTED_VARIATION_RS_ID + "")).thenReturn(root);

        // act

        NCBIClinVarVO ncbiClinVarVO = ncbiClinVarManager.fetchVariationById(EXPECTED_VARIATION_RS_ID + "");

        // assert

        Assert.assertNotNull(ncbiClinVarVO);
        Assert.assertNotNull(ncbiClinVarVO.getClinicalSignificance());

        Assert.assertEquals("Pathogenic", ncbiClinVarVO.getClinicalSignificance().getDescription());
        Assert.assertEquals("2009/10/01 00:00", ncbiClinVarVO.getClinicalSignificance().getLastEvaluated());
        Assert.assertEquals("no assertion criteria provided",
                ncbiClinVarVO.getClinicalSignificance().getReviewStatus());

        List<NCBIClinVarVO.NCBITraitSet> traitSet = ncbiClinVarVO.getTraitSet();

        Assert.assertNotNull(traitSet);
        Assert.assertTrue(traitSet.size() == 1);

        List<NCBIClinVarVO.NCBIVariationSet> variationSet = ncbiClinVarVO.getVariationSet();

        Assert.assertNotNull(variationSet);
        Assert.assertTrue(variationSet.size() == 1);
        // TODO add asserts
    }

    @Test
    public void testVariationsOnInterval()
            throws IOException, IllegalAccessException, ExternalDbUnavailableException, InterruptedException {

        // arrange
        String historyQueryStr = readFile("ncbi_search_snp_variations_on_region.xml");
        String ncbiFetchResponseStr = readFile("ncbi_fetch_snp_variations_on_region.xml");

        Mockito.when(ncbiAuxiliaryManager.searchWithHistory(anyString(),
                anyString(), anyInt())).thenReturn(historyQueryStr);
        Mockito.when(ncbiAuxiliaryManager.fetchWithHistory(anyString(),
                anyString(), anyObject())).thenReturn(ncbiFetchResponseStr);

        // act
        List<Variation> variationsList =
                ncbiShortVarManager.fetchVariationsOnRegion("human", "97735879", "97736093", "1");

        // assert
        Assert.assertNotNull(variationsList);
        Assert.assertEquals(3, variationsList.size());

    }

    @Test
    public void testFetchVariation()
            throws IOException, IllegalAccessException, ExternalDbUnavailableException, InterruptedException {

        // arrange
        String summaryJson = readFile("ncbi_summary_snp_rs7412.json");
        String fasta = readFile("ncbi_fetch_snp_variation_7421_fasta.txt");
        String fetched = readFile("ncbi_fetch_snp_variation_7412.xml");

        JsonNode root = jsonResultParse(summaryJson);

        Mockito.when(ncbiAuxiliaryManager.
                summaryEntityById(NCBIDatabase.SNP.name(), EXPECTED_VARIATION_RS_ID + "")).thenReturn(root);
        Mockito.when(ncbiAuxiliaryManager.
                fetchTextById(NCBIDatabase.SNP.name(), EXPECTED_VARIATION_RS_ID + "", "fasta")).thenReturn(fasta);
        Mockito.when(ncbiAuxiliaryManager.
                fetchXmlById(NCBIDatabase.SNP.name(), EXPECTED_VARIATION_RS_ID + "", null)).thenReturn(fetched);
        Mockito.when(ncbiAuxiliaryManager.
                getMapper()).thenReturn(mapper);

        // act
        NCBIShortVarVO ncbiShortVarVO = ncbiShortVarManager.fetchVariationById(EXPECTED_VARIATION_RS_ID + "");

        // assert
        Assert.assertNotNull(ncbiShortVarVO);
        Assert.assertEquals(Long.valueOf(EXPECTED_VARIATION_RS_ID), ncbiShortVarVO.getSnpId());
        Assert.assertEquals("Pathogenic", ncbiShortVarVO.getClinicalSignificance());
        Assert.assertEquals("T=0.0751/376", ncbiShortVarVO.getGlobalMaf());
        Assert.assertEquals("19", ncbiShortVarVO.getChr());
        Assert.assertEquals("19:44908822", ncbiShortVarVO.getChromosomePosition());
        Assert.assertEquals("9606", ncbiShortVarVO.getTaxId());
        Assert.assertEquals("GRCh38.p2", ncbiShortVarVO.getGenomeLabel());
        Assert.assertEquals("snp", ncbiShortVarVO.getSnpClass());
        Assert.assertEquals("NT_011109.17:17667948", ncbiShortVarVO.getContigPosition());
    }

    private String readFile(String filename) throws IOException {
        Resource resource = context.getResource("classpath:externaldb//data//" + filename);
        String pathStr = resource.getFile().getPath();
        return new String(Files.readAllBytes(Paths.get(pathStr)), Charset.defaultCharset());
    }

    private JsonNode jsonResultParse(String srcJson) throws IOException {
        JsonNode root = mapper.readTree(srcJson).path("result");
        Iterator<JsonNode> uids = root.path("uids").iterator();

        if (uids.hasNext()) {
            int uid = uids.next().asInt();
            root = root.path("" + uid);
        }

        return root;
    }

}
