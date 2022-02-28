/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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

package com.epam.catgenome.manager.bam;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.Interval;
import com.epam.catgenome.entity.bam.BamCoverage;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.bam.CoverageInterval;
import com.epam.catgenome.entity.bam.CoverageQueryParams;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.manager.parallel.TaskExecutorService;
import com.epam.catgenome.manager.reference.ReferenceManager;
import com.epam.catgenome.util.db.Page;
import com.epam.catgenome.util.db.PagingInfo;
import com.epam.catgenome.util.db.SortInfo;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
public class BamCoverageManagerTest extends AbstractManagerTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private BamCoverageManager coverageManager;

    @Autowired
    @InjectMocks
    private BamManager bamManager;

    @Autowired
    private TaskExecutorService taskExecutorService;

    private static final String TEST_REF_NAME = "//dm606.X.fa";
    private static final String TEST_BAM_NAME = "//agnX1.09-28.trim.dm606.realign.bam";
    private static final String BAI_EXTENSION = ".bai";

    private static final int STEP = 100000;
    private static final float COVERAGE_FROM = 0.002f;
    private static final float COVERAGE_TO = 0.01f;

    private Resource resource;
    private Reference testReference;

    @Before
    public void setup() throws IOException {
        resource = context.getResource("classpath:templates");
        final File fastaFile = new File(resource.getFile().getAbsolutePath() + TEST_REF_NAME);

        final ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName(TEST_REF_NAME + biologicalDataItemDao.createBioItemId());
        request.setPath(fastaFile.getPath());

        taskExecutorService.setForceSequential(true);
        testReference = referenceManager.registerGenome(request);
    }

    @Test
    public void testCreateCoverage() throws IOException {
        final BamCoverage coverage = registerBamCoverage();
        assertNotNull(coverage);
        coverageManager.deleteCoverageDocument(coverage.getCoverageId());
    }

    @Test
    public void testSearchCoverage() throws IOException, ParseException {
        final BamCoverage coverage = registerBamCoverage();
        final CoverageQueryParams params = CoverageQueryParams.builder()
                .coverageId(coverage.getCoverageId())
                .chromosomes(Arrays.asList("2R", "3R"))
                .coverage(new Interval<>(COVERAGE_FROM, COVERAGE_TO))
                .build();
        final PagingInfo pagingInfo = PagingInfo.builder()
                .pageSize(10)
                .pageNum(1)
                .build();
        final SortInfo sortInfo = SortInfo.builder()
                .field("coverage")
                .ascending(true)
                .build();
        params.setPagingInfo(pagingInfo);
        params.setSortInfo(Collections.singletonList(sortInfo));
        Page<CoverageInterval> intervalPage = coverageManager.search(params);
        coverageManager.deleteCoverageDocument(coverage.getCoverageId());
        assertEquals(2, intervalPage.getItems().size());
    }

    private BamFile setUpTestFile() throws IOException {
        final String path = resource.getFile().getAbsolutePath() + TEST_BAM_NAME;
        final IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(path);
        request.setIndexPath(path + BAI_EXTENSION);
        request.setName(TEST_BAM_NAME + biologicalDataItemDao.createBioItemId());
        request.setReferenceId(testReference.getId());
        request.setType(BiologicalDataItemResourceType.FILE);

        final BamFile bamFile = bamManager.registerBam(request);
        assertNotNull(bamFile);
        return bamFile;
    }

    private BamCoverage registerBamCoverage() throws IOException {
        final BamFile bamFile = setUpTestFile();
        final BamCoverage coverage = BamCoverage.builder()
                .bamId(bamFile.getId())
                .step(STEP)
                .build();
        return coverageManager.create(coverage);
    }
}
