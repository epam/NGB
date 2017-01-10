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
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.bam.BamFileDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.helper.EntityHelper;

/**
 * Source:      BamFileDaoTest.java
 * Created:     12/10/2015
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Semen_Dmitriev
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BamFileDaoTest extends AbstractDaoTest  {
    @Autowired
    private BamFileDao bamFileDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    private static final long TEST_USER_ID = 42;

    private static final String BAM_PATH = "132456";
    private static final String INDEX_PATH = "qwerty";


    @Override
    public void setup() throws Exception {
        assertNotNull("VcfFileDao isn't provided.", bamFileDao);
        super.setup();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void createBamFileIdTest() {
        final Long testId = bamFileDao.createBamFileId();
        assertNotNull(testId);
        assertTrue(testId > 0);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveLoadBamFileTest() {
        BamFile bamFile = new BamFile();

        bamFile.setName("testFile");
        bamFile.setCreatedBy(TEST_USER_ID);
        bamFile.setType(BiologicalDataItemResourceType.FILE);
        bamFile.setFormat(BiologicalDataItemFormat.BAM);
        bamFile.setPath(BAM_PATH);
        bamFile.setCreatedDate(new Date());
        bamFile.setReferenceId(reference.getId());

        BiologicalDataItem index = EntityHelper.createIndex(BiologicalDataItemFormat.BAM_INDEX,
                BiologicalDataItemResourceType.FILE, INDEX_PATH);
        bamFile.setIndex(index);

        biologicalDataItemDao.createBiologicalDataItem(index);
        biologicalDataItemDao.createBiologicalDataItem(bamFile);
        bamFileDao.createBamFile(bamFile);

        BamFile loadedFile = bamFileDao.loadBamFile(bamFile.getId());

        assertNotNull(loadedFile);
        assertEquals(bamFile.getName(), loadedFile.getName());
        assertEquals(bamFile.getCreatedBy(), loadedFile.getCreatedBy());
        assertEquals(bamFile.getType(), loadedFile.getType());
        assertEquals(bamFile.getFormat(), loadedFile.getFormat());
        assertEquals(bamFile.getPath(), loadedFile.getPath());
        assertEquals(bamFile.getIndex().getPath(), loadedFile.getIndex().getPath());
        assertEquals(bamFile.getCreatedDate(), loadedFile.getCreatedDate());
        assertEquals(bamFile.getReferenceId(), loadedFile.getReferenceId());
        assertEquals(bamFile.getBioDataItemId(), loadedFile.getBioDataItemId());

        List<BamFile> bamFiles = bamFileDao.loadBamFilesByReferenceId(reference.getId());

        assertFalse(bamFiles.isEmpty());
        bamFileDao.deleteBamFile(loadedFile.getId());

        loadedFile = bamFileDao.loadBamFile(loadedFile.getId());
        assertNull(loadedFile);
    }
}
