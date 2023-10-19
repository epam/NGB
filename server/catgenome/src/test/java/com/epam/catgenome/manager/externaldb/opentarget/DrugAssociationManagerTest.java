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
package com.epam.catgenome.manager.externaldb.opentarget;

import com.epam.catgenome.entity.externaldb.target.opentargets.DrugAssociation;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.target.AssociationSearchRequest;
import com.epam.catgenome.manager.externaldb.target.opentargets.DiseaseManager;
import com.epam.catgenome.manager.externaldb.target.opentargets.DrugAssociationManager;
import junit.framework.TestCase;
import org.apache.commons.lang3.tuple.Pair;
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
public class DrugAssociationManagerTest extends TestCase {

    private static final List<String> GENE_IDS = Arrays.asList("ENSG00000167325", "ENSG00000136573");
    @Autowired
    private DrugAssociationManager drugAssociationManager;
    @Autowired
    private DiseaseManager diseaseManager;

    @Autowired
    private ApplicationContext context;

    @Before
    public void setUp() throws IOException, ParseException {
        final String path = context.getResource("classpath:opentargets").getFile().getPath();
        diseaseManager.importData(path);
        drugAssociationManager.importData(path);
    }

    @Test
    public void totalCountTest() throws IOException, ParseException {
        final Pair<Long, Long> totalCount = drugAssociationManager.recordsCount(GENE_IDS);
        assertEquals(2, totalCount.getLeft().intValue());
        assertEquals(2, totalCount.getRight().intValue());
    }

    @Test
    public void searchDrugAssociationsTest() throws IOException, ParseException {
        final AssociationSearchRequest request = new AssociationSearchRequest();
        request.setPage(1);
        request.setPageSize(3);
        request.setGeneIds(GENE_IDS);
        final SearchResult<DrugAssociation> result = drugAssociationManager.search(request);
        assertEquals(2, result.getItems().size());
    }
}
