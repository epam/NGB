/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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
package com.epam.catgenome.manager.externaldb.ncbi;

import com.epam.catgenome.entity.externaldb.ncbi.GeneId;
import junit.framework.TestCase;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class NCBIEnsemblIdsManagerTest extends TestCase {

    private static final int ENSEMBL_COUNT = 1;
    private static final int ENTREZ_COUNT = 2;
    private static final List<String> ENSEMBL_IDS = Arrays.asList("ENSG00000111424", "ENSG00000134058");
    private static final List<String> ENTREZ_IDS = Arrays.asList("100189549", "7421");


    @Autowired
    private NCBIEnsemblIdsManager manager;

    @Autowired
    private ApplicationContext context;

    @Before
    public void setUp() throws IOException, ParseException {
        String fileName = context.getResource("classpath:ncbi//gene2ensembl").getFile().getPath();
        manager.importData(fileName);
    }

    @Test
    public void searchByEnsemblIdsTest() throws ParseException, IOException {
        final List<GeneId> result = manager.searchByEnsemblIds(ENSEMBL_IDS);
        assertEquals(ENSEMBL_COUNT, result.size());
    }

    @Test
    public void searchByEntrezIdsTest() throws ParseException, IOException {
        final List<GeneId> result = manager.searchByEntrezIds(ENTREZ_IDS);
        assertEquals(ENTREZ_COUNT, result.size());
    }
}
