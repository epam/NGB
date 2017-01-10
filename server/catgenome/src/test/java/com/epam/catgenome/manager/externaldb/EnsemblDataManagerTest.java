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

import com.epam.catgenome.controller.vo.externaldb.ensemblevo.EnsemblEntryVO;
import com.epam.catgenome.controller.vo.externaldb.ensemblevo.EnsemblVariationEntryVO;
import com.epam.catgenome.exception.ExternalDbUnavailableException;

/**
 * Source:    EnsemblDataManagerTest
 * Created:   4/13/2016, 1:47 PM
 * Project:   catgenome
 * Make:      IntelliJ IDEA
 *
 * @author Valerii Iakovlev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class EnsemblDataManagerTest {

    private static final long GENE_START = 74082933L;
    private static final long END = 74122525L;
    private static final long START_ENTRY = 140404043L;
    private static final long END_ENTRY = 140426250L;
    @Autowired
    private ApplicationContext context;

    @Mock
    private HttpDataManager httpDataManager;

    @InjectMocks
    @Spy
    private EnsemblDataManager ensemblDataManager;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFetchGenesOnRegion()
            throws IOException, ExternalDbUnavailableException, InterruptedException {

        // arrange
        String fetchRes = readFile("ensembl_overlap_gene.json");

        Mockito.when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(ParameterNameValue[].class))
        ).thenReturn(fetchRes);

        // act
        List<EnsemblEntryVO> ensemblGeneList =
                ensemblDataManager.fetchVariationsOnRegion("human", "140424943", "140624564", "7");

        // assert
        Assert.assertNotNull(ensemblGeneList);
        Assert.assertEquals(6, ensemblGeneList.size());

        EnsemblEntryVO ensemblEntryVO = ensemblGeneList.get(0);
        Assert.assertNotNull(ensemblEntryVO);
        Assert.assertTrue(ensemblEntryVO.getStart().equals(START_ENTRY));
        Assert.assertTrue(ensemblEntryVO.getEnd().equals(END_ENTRY));
    }

    @Test
    public void testEnsemblFetchVariationEntry()
            throws IOException, ExternalDbUnavailableException, InterruptedException {

        // arrange
        String fetchRes = readFile("ensembl_summary_variation_rs7412.json");

        Mockito.when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(ParameterNameValue[].class))
        ).thenReturn(fetchRes);

        // act
        EnsemblVariationEntryVO ensemblVariationEntryVO = ensemblDataManager.fetchVariationEntry("rs7412", "human");

        // assert
        Assert.assertNotNull(ensemblVariationEntryVO);
        Assert.assertEquals("rs7412", ensemblVariationEntryVO.getName());
        Assert.assertEquals(new Double("0.0750799"), ensemblVariationEntryVO.getMaf());
        Assert.assertEquals("Y", ensemblVariationEntryVO.getAmbiguity());
        Assert.assertEquals("SNP", ensemblVariationEntryVO.getVarClass());
        Assert.assertEquals("C", ensemblVariationEntryVO.getAncestralAllele());
        Assert.assertEquals("T", ensemblVariationEntryVO.getMinorAllele());
        Assert.assertEquals("missense_variant", ensemblVariationEntryVO.getMostSevereConsequence());
    }

    @Test
    public void testEnsemlFetchEnsemblEntry()
            throws IOException, ExternalDbUnavailableException, InterruptedException {

        // arrange
        String fetchRes = readFile("ensembl_lookup_id_ENSG00000106683.json");

        Mockito.when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(ParameterNameValue[].class))
        ).thenReturn(fetchRes);

        // act
        EnsemblEntryVO limk1Gene = ensemblDataManager.fetchEnsemblEntry("rs7412");

        // assert
        Assert.assertNotNull(limk1Gene);
        Assert.assertEquals("ENSG00000106683", limk1Gene.getId());
        Assert.assertTrue(limk1Gene.getStart().equals(GENE_START));
        Assert.assertTrue(limk1Gene.getEnd().equals(END));
        Assert.assertEquals("GRCh38", limk1Gene.getAssemblyName());
        Assert.assertEquals("LIMK1", limk1Gene.getDisplayName());
        Assert.assertEquals("LIM domain kinase 1 [Source:HGNC Symbol;Acc:HGNC:6613]", limk1Gene.getDescription());
    }

    private String readFile(String filename) throws IOException {
        Resource resource = context.getResource("classpath:externaldb//data//" + filename);
        String pathStr = resource.getFile().getPath();
        String content = new String(Files.readAllBytes(Paths.get(pathStr)), Charset.defaultCharset());
        return content;
    }
}


