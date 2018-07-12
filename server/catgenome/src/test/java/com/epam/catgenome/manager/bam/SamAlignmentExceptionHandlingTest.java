/*
 * MIT License
 *
 * Copyright (c) 2018 EPAM Systems
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


import java.io.File;
import java.io.IOException;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.bam.BamQueryOption;
import com.epam.catgenome.entity.bam.BamTrackMode;
import com.epam.catgenome.entity.bam.TrackDirectionType;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.exception.SamAlignmentException;
import com.epam.catgenome.manager.bam.filters.Filter;
import com.epam.catgenome.manager.bam.filters.MiddleSAMRecordFilter;
import com.epam.catgenome.manager.bam.handlers.SAMRecordHandler;
import com.epam.catgenome.manager.bam.sifters.DownsamplingSifter;
import com.epam.catgenome.manager.bam.sifters.FullResultSifter;
import com.epam.catgenome.manager.parallel.TaskExecutorService;
import com.epam.catgenome.manager.reference.ReferenceManager;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordSetBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class SamAlignmentExceptionHandlingTest extends AbstractManagerTest {

    private final long timeout = 100_000L;
    private final int endTrack = 120;
    private final int readLength = 75;

    @Autowired
    ApplicationContext context;

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private TaskExecutorService taskExecutorService;

    private static final String TEST_REF_NAME = "//Test2.fa";

    private Resource resource;
    private String chromosomeName = "X";
    private BamQueryOption options;
    private Filter<SAMRecord> filter;
    private SAMRecordHandler recordHandler;

    @Before
    public void setup() throws IOException {
        resource = context.getResource("classpath:templates");
        File fastaFile = new File(resource.getFile().getAbsolutePath() + TEST_REF_NAME);

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName(TEST_REF_NAME + biologicalDataItemDao.createBioItemId());
        request.setPath(fastaFile.getPath());

        taskExecutorService.setForceSequential(true);

        Reference testReference = referenceManager.registerGenome(request);

        options = new BamQueryOption();
        options.setTrackDirection(TrackDirectionType.MIDDLE);
        options.setMode(BamTrackMode.FULL);
        options.setRefID(testReference.getId());
        options.setChromosomeName(chromosomeName);

        ResponseBodyEmitter emitter = new ResponseBodyEmitter(this.timeout);
        BamTrackEmitter trackEmitter = new BamTrackEmitter(emitter);
        DownsamplingSifter<SAMRecord> sifter = new FullResultSifter(false, trackEmitter);
        filter = new MiddleSAMRecordFilter(sifter);
    }

    @Test(expected = SamAlignmentException.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void matchBeyondReferenceTest() throws IOException {

        recordHandler = new SAMRecordHandler(1, endTrack, referenceManager, filter, options);

        final SAMRecordSetBuilder set = new SAMRecordSetBuilder();
        set.setReadLength(readLength);
        final SAMRecord rec = set.addFrag("read", 0, 100, false, false, "75M", "*", 151);

        recordHandler.add(rec);
    }
}
