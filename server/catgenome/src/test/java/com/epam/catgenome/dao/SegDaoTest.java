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

package com.epam.catgenome.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.seg.SegFileDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.seg.SegFile;
import com.epam.catgenome.entity.seg.SegSample;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.util.AuthUtils;

/**
 * Source:      SegDaoTest
 * Created:     14.06.16, 17:39
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class SegDaoTest extends AbstractDaoTest {
    @Autowired
    private SegFileDao segFileDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Override
    public void setup() throws Exception {
        assertNotNull("SegFileDao isn't provided.", segFileDao);
        super.setup();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testCreateLoad() {
        SegFile segFile = new SegFile();

        segFile.setId(segFileDao.createSegFileId());
        segFile.setName("testFile");
        segFile.setCreatedBy(AuthUtils.getCurrentUserId());
        segFile.setType(BiologicalDataItemResourceType.FILE);
        segFile.setPath("///");
        segFile.setCreatedDate(new Date());
        segFile.setReferenceId(reference.getId());

        BiologicalDataItem index = EntityHelper.createIndex(BiologicalDataItemFormat.SEG_INDEX,
                BiologicalDataItemResourceType.FILE, "////");
        segFile.setIndex(index);

        biologicalDataItemDao.createBiologicalDataItem(index);
        final Long realId = segFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(segFile);
        segFileDao.createSegFile(segFile, realId);

        SegFile loadedFile = segFileDao.loadSegFile(segFile.getId());

        assertNotNull(loadedFile);
        assertEquals(segFile.getName(), loadedFile.getName());
        assertEquals(segFile.getCreatedBy(), loadedFile.getCreatedBy());
        assertEquals(segFile.getType(), loadedFile.getType());
        assertEquals(segFile.getFormat(), loadedFile.getFormat());
        assertEquals(segFile.getPath(), loadedFile.getPath());
        assertEquals(segFile.getIndex().getPath(), loadedFile.getIndex().getPath());
        assertEquals(segFile.getCreatedDate(), loadedFile.getCreatedDate());
        assertEquals(segFile.getReferenceId(), loadedFile.getReferenceId());
        assertEquals(segFile.getBioDataItemId(), loadedFile.getBioDataItemId());

        List<SegFile> segFiles = segFileDao.loadSegFilesByReferenceId(reference.getId());

        assertFalse(segFiles.isEmpty());

        segFileDao.deleteSegFile(segFile.getId());
        assertNull(segFileDao.loadSegFile(segFile.getId()));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testCreateLoadSamples() {
        SegFile segFile = new SegFile();

        segFile.setId(segFileDao.createSegFileId());
        segFile.setName("testFile");
        segFile.setCreatedBy(AuthUtils.getCurrentUserId());
        segFile.setType(BiologicalDataItemResourceType.FILE);
        segFile.setPath("///");
        segFile.setCreatedDate(new Date());
        segFile.setReferenceId(reference.getId());

        BiologicalDataItem index = EntityHelper.createIndex(BiologicalDataItemFormat.VCF_INDEX,
                BiologicalDataItemResourceType.FILE, "////");
        segFile.setIndex(index);

        biologicalDataItemDao.createBiologicalDataItem(index);
        final Long realId = segFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(segFile);
        segFileDao.createSegFile(segFile, realId);

        SegFile loadedFile = segFileDao.loadSegFile(segFile.getId());
        List<SegSample> samples = Arrays.asList(new SegSample("testSample1"), new SegSample("testSample2"));
        segFileDao.createSamples(samples, loadedFile.getId());

        assertFalse(segFileDao.loadSamplesForFile(loadedFile.getId()).isEmpty());
        Map<Long, List<SegSample>> sampleMap = segFileDao.loadSamplesByFileIds(Collections.
                singletonList(loadedFile.getId()));
        assertFalse(sampleMap.isEmpty());
        assertFalse(sampleMap.get(loadedFile.getId()).isEmpty());

        sampleMap = segFileDao.loadSamplesForFilesByReference(reference.getId());
        assertFalse(sampleMap.isEmpty());
        assertFalse(sampleMap.get(loadedFile.getId()).isEmpty());
    }
}
