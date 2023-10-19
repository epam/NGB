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

import com.epam.catgenome.entity.Interval;
import com.epam.catgenome.entity.externaldb.target.opentargets.DiseaseAssociation;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.target.AssociationSearchRequest;
import com.epam.catgenome.manager.externaldb.target.opentargets.DiseaseAssociationManager;
import com.epam.catgenome.manager.externaldb.target.opentargets.DiseaseField;
import com.epam.catgenome.manager.externaldb.target.opentargets.DiseaseManager;
import com.epam.catgenome.manager.index.Filter;
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
import java.util.Collections;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class DiseaseAssociationManagerTest extends TestCase {

    private static final int TOTAL_COUNT = 11;
    private static final List<String> GENE_IDS = Arrays.asList("ENSG00000007171", "ENSG00000006128");
    private static final float FROM = 0.0729517F;
    private static final float TO = 0.5592963F;
    @Autowired
    private DiseaseAssociationManager manager;
    @Autowired
    private DiseaseManager diseaseManager;

    @Autowired
    private ApplicationContext context;

    @Before
    public void setUp() throws IOException, ParseException {
        final String path = context.getResource("classpath:opentargets").getFile().getPath();
        diseaseManager.importData(path);
        manager.importData(path);
    }

    @Test
    public void totalCountTest() throws IOException, ParseException {
        final long totalCount = manager.totalCount(GENE_IDS);
        assertEquals(TOTAL_COUNT, totalCount);
    }

    @Test
    public void searchDiseaseAssociationsTest() throws IOException, ParseException {
        final AssociationSearchRequest request = new AssociationSearchRequest();
        request.setPage(2);
        request.setPageSize(3);
        request.setGeneIds(GENE_IDS);
        final SearchResult<DiseaseAssociation> result = manager.search(request);
        assertEquals(3, result.getItems().size());
    }

    @Test
    public void filterByRangeTest() throws IOException, ParseException {
        final AssociationSearchRequest request = new AssociationSearchRequest();
        request.setPage(1);
        request.setPageSize(10);
        request.setGeneIds(GENE_IDS);
        final Filter filter = Filter.builder()
                .field(DiseaseField.TEXT_MINING_SCORE.name())
                .range(new Interval<>(FROM, TO))
                .build();
        request.setFilters(Collections.singletonList(filter));
        final SearchResult<DiseaseAssociation> result = manager.search(request);
        assertEquals(5, result.getItems().size());
    }
}
