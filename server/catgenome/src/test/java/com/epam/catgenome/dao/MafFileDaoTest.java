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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.maf.MafFileDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.maf.MafFile;
import com.epam.catgenome.helper.EntityHelper;

/**
 * Source:      MafFileDaoTest
 * Created:     12.07.16, 11:42
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class MafFileDaoTest extends AbstractDaoTest {
    @Autowired
    private MafFileDao mafFileDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Override
    public void setup() throws Exception {
        assertNotNull("MafFileDao isn't provided.", mafFileDao);
        super.setup();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testCreateLoad() {
        MafFile mafFile = new MafFile();

        mafFile.setId(mafFileDao.createMafFileId());
        mafFile.setName("testFile");
        mafFile.setType(BiologicalDataItemResourceType.FILE);
        mafFile.setPath("///");
        mafFile.setCreatedDate(new Date());
        mafFile.setReferenceId(reference.getId());
        mafFile.setOwner(EntityHelper.TEST_OWNER);

        BiologicalDataItem index = EntityHelper.createIndex(BiologicalDataItemFormat.MAF_INDEX,
                BiologicalDataItemResourceType.FILE, "////");
        mafFile.setIndex(index);

        biologicalDataItemDao.createBiologicalDataItem(index);
        final Long realId = mafFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(mafFile);
        mafFileDao.createMafFile(mafFile, realId);

        MafFile loadedFile = mafFileDao.loadMafFile(mafFile.getId());

        assertNotNull(loadedFile);
        assertEquals(mafFile.getName(), loadedFile.getName());
        assertEquals(mafFile.getType(), loadedFile.getType());
        assertEquals(mafFile.getFormat(), loadedFile.getFormat());
        assertEquals(mafFile.getPath(), loadedFile.getPath());
        assertEquals(mafFile.getIndex().getPath(), loadedFile.getIndex().getPath());
        assertEquals(mafFile.getCreatedDate(), loadedFile.getCreatedDate());
        assertEquals(mafFile.getReferenceId(), loadedFile.getReferenceId());
        assertEquals(mafFile.getBioDataItemId(), loadedFile.getBioDataItemId());
        assertEquals(mafFile.getOwner(), loadedFile.getOwner());

        mafFileDao.deleteMafFile(mafFile.getId());
        assertNull(mafFileDao.loadMafFile(mafFile.getId()));
    }
}
