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

import javax.xml.bind.JAXBException;

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

import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.DbReferenceType;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.Entry;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.Uniprot;

/**
 * Source:    UniprotDataManagerTest
 * Created:   4/13/2016, 1:48 PM
 * Project:   catgenome
 * Make:      IntelliJ IDEA
 *
 * @author Valerii Iakovlev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class UniprotDataManagerTest {

    public static final int EXPECTED_REFERENCE_NUMBER = 31;
    public static final int EXPECTED_COMMENT_NUMBER = 22;
    public static final int EXPECTED_DBREF_NUMBER = 135;
    public static final int EXPECTED_VERSION = 177;
    public static final int EXPECTED_SEQUENCE_VERSION_1 = 3;
    @Autowired
    private ApplicationContext context;

    @Mock
    private HttpDataManager httpDataManager;

    @InjectMocks
    @Spy
    private UniprotDataManager uniprotDataManager;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUniprotFetchVariationEntry()
            throws IOException, ExternalDbUnavailableException, InterruptedException, JAXBException {

        // arrange
        String fetchRes = readFile("uniprot_fetch_gene_ENSG00000106683.xml");

        Mockito.when(
                httpDataManager.fetchData(Mockito.any(), Mockito.any(ParameterNameValue[].class))
        ).thenReturn(fetchRes);

        // act
        Uniprot uniprotGene = uniprotDataManager.fetchUniprotEntry("ENSG00000106683");

        // assert
        Assert.assertNotNull(uniprotGene);

        List<Entry> entryList = uniprotGene.getEntry();
        Assert.assertNotNull(entryList);
        Assert.assertEquals(2, entryList.size());

        Entry entry = entryList.get(0);
        Assert.assertNotNull(entry);
        Assert.assertEquals("LIMK1_HUMAN", entry.getName().get(0));
        Assert.assertEquals("LIM domain kinase 1", entry.getProtein().getRecommendedName().getFullName().getValue());
        Assert.assertEquals("LIMK-1", entry.getProtein().getRecommendedName().
                getShortName().get(0).getValue());

        Assert.assertNull(entry.getProtein().getAllergenName());
        Assert.assertEquals(0, entry.getProtein().getSubmittedName().size());
        Assert.assertEquals(0, entry.getProtein().getAlternativeName().size());
        Assert.assertEquals(0, entry.getProtein().getCdAntigenName().size());
        Assert.assertEquals(0, entry.getProtein().getInnName().size());
        Assert.assertEquals(0, entry.getProtein().getDomain().size());

        Assert.assertNotNull("LIMK1", entry.getGene().get(0));
        Assert.assertEquals("LIMK1", entry.getGene().get(0).getName().get(0).getValue());
        Assert.assertEquals(EXPECTED_REFERENCE_NUMBER, entry.getReference().size());
        Assert.assertEquals(EXPECTED_COMMENT_NUMBER, entry.getComment().size());

        Assert.assertNotNull(entry.getComment().get(0));
        Assert.assertNotNull(entry.getComment().get(0).getText().get(0));
        Assert.assertNotNull(entry.getComment().get(0).getText().get(0));
        Assert.assertNotNull(entry.getComment().get(0).getText().get(0).getValue());
        Assert.assertTrue(entry.getComment().get(0).getText().get(0).getValue().startsWith("Serine"));
        Assert.assertTrue("function".equals(entry.getComment().get(0).getType()));

        Assert.assertEquals(EXPECTED_DBREF_NUMBER, entry.getDbReference().size());
        Assert.assertEquals("Swiss-Prot", entry.getDataset());
        Assert.assertEquals(EXPECTED_VERSION, entry.getVersion());

        Assert.assertTrue("9838BEFBE7006447".equals(entry.getSequence().getChecksum()));
        Assert.assertEquals(EXPECTED_SEQUENCE_VERSION_1, entry.getSequence().getVersion());
        Assert.assertEquals("evidence at protein level", entry.getProteinExistence().getType());

        DbReferenceType dbReferenceType = entry.getDbReference().get(0);
        Assert.assertNotNull(dbReferenceType);

        Assert.assertEquals("EC", dbReferenceType.getType());
        Assert.assertTrue(dbReferenceType.getId().startsWith("2.7.11"));

    }

    private String readFile(String filename) throws IOException {
        Resource resource = context.getResource("classpath:externaldb//data//" + filename);
        String pathStr = resource.getFile().getPath();
        String content = new String(Files.readAllBytes(Paths.get(pathStr)), Charset.defaultCharset());
        return content;
    }

}
