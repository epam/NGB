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
package com.epam.catgenome.manager.externaldb.pharmgkb;

import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBDisease;
import com.epam.catgenome.manager.externaldb.SearchResult;
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
public class PharmGKBDiseaseAssociationManagerTest extends TestCase {

    private static final List<String> GENE_IDS = Arrays.asList("ENSG00000175899", "ENSG00000153930");
    private static final int ENTRIES_COUNT = 2;
    private static final int ENTRIES_TOTAL_COUNT = 2;

    @Autowired
    private PharmGKBDiseaseAssociationManager pharmGKBDiseaseAssociationManager;
    @Autowired
    private PharmGKBGeneManager pharmGKBGeneManager;

    @Autowired
    private ApplicationContext context;

    @Before
    public void setUp() throws IOException, ParseException {
        final String genesPath = context.getResource("classpath:pharmgkb//genes.tsv").getFile().getPath();
        pharmGKBGeneManager.importData(genesPath);
        final String path = context.getResource("classpath:pharmgkb//relationships.tsv").getFile().getPath();
        pharmGKBDiseaseAssociationManager.importData(path);
    }

    @Test
    public void totalCountTest() throws IOException, ParseException {
        final long totalCount = pharmGKBDiseaseAssociationManager.totalCount(GENE_IDS);
        assertEquals(ENTRIES_TOTAL_COUNT, totalCount);
    }

    @Test
    public void searchDrugAssociationsTest() throws IOException, ParseException {
        final PharmGKBDiseaseSearchRequest request = new PharmGKBDiseaseSearchRequest();
        request.setPage(1);
        request.setPageSize(10);
        request.setGeneIds(GENE_IDS);
        final SearchResult<PharmGKBDisease> result = pharmGKBDiseaseAssociationManager.search(request);
        assertEquals(ENTRIES_COUNT, result.getTotalCount().intValue());
    }
}
