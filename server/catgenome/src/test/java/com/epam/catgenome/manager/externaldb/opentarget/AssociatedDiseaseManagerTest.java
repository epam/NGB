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

import com.epam.catgenome.entity.externaldb.opentarget.AssociatedDiseaseAggregated;
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
public class AssociatedDiseaseManagerTest extends TestCase {

    private static final int TOTAL_COUNT = 11;
    @Autowired
    private AssociatedDiseaseManager manager;

    @Autowired
    private ApplicationContext context;

    @Before
    public void setUp() throws IOException, ParseException {
        final String path = context.getResource("classpath:opentargets//associations").getFile().getPath();
        final String pathOverall = context.getResource("classpath:opentargets//associations_overall")
                .getFile().getPath();
        manager.importData(path, pathOverall);
    }

    @Test
    public void searchDiseasesCountTest() throws IOException, ParseException {
        final List<String> targetIds = Arrays.asList("ENSG00000007171", "ENSG00000006128");
        final int totalCount = manager.totalCount(targetIds);
        assertEquals(TOTAL_COUNT, totalCount);
    }

    @Test
    public void searchDiseasesTest() throws IOException, ParseException {
        final List<String> targetIds = Arrays.asList("ENSG00000007171", "ENSG00000006128");
        final AssociationSearchRequest request = AssociationSearchRequest.builder()
                .page(2)
                .pageSize(3)
                .targetIds(targetIds)
                .build();
        final List<AssociatedDiseaseAggregated> diseases = manager.search(request);
        assertEquals(3, diseases.size());
    }
}
