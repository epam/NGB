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
package com.epam.catgenome.manager.externaldb.dgidb;

import com.epam.catgenome.entity.externaldb.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.target.AssociationSearchRequest;
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
public class DGIDBDrugAssociationManagerTest extends TestCase {

    private static final int ENTRIES_COUNT = 4;
    private static final int ENTRIES_TOTAL_COUNT = 4;
    private static final List<String> ENTREZ_IDS = Arrays.asList("1022", "7421");

    @Autowired
    private DGIDBDrugAssociationManager manager;

    @Autowired
    private ApplicationContext context;

    private String fileName;

    @Before
    public void setUp() throws IOException, ParseException {
        this.fileName = context.getResource("classpath:dgidb//interactions.tsv").getFile().getPath();
        manager.importData(fileName);
    }

    @Test
    public void totalCountTest() throws IOException, ParseException {
        final long totalCount = manager.totalCount(ENTREZ_IDS);
        assertEquals(ENTRIES_TOTAL_COUNT, totalCount);
    }

    @Test
    public void searchDrugAssociationsTest() throws IOException, ParseException {
        final AssociationSearchRequest request = AssociationSearchRequest.builder()
                .page(1)
                .pageSize(10)
                .geneIds(ENTREZ_IDS)
                .build();
        final SearchResult<DGIDBDrugAssociation> result = manager.search(request);
        assertEquals(ENTRIES_COUNT, result.getTotalCount().intValue());
    }
}
