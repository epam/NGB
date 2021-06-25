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

import com.epam.catgenome.dao.wig.WigFileDao;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.wig.WigFile;
import com.epam.catgenome.helper.EntityHelper;

/**
 * Source:      WigFileDaoTest
 * Created:     20.01.16, 18:03
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class WigFileDaoTest extends AbstractDaoTest {
    @Autowired
    private WigFileDao wigFileDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Override
    public void setup() throws Exception {
        assertNotNull("GeneFileDao isn't provided.", wigFileDao);
        super.setup();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveLoadWigFile() {
        WigFile wigFile = new WigFile();

        wigFile.setName("testFile");
        wigFile.setCreatedDate(new Date());
        wigFile.setReferenceId(reference.getId());
        wigFile.setType(BiologicalDataItemResourceType.FILE);
        wigFile.setFormat(BiologicalDataItemFormat.WIG);
        wigFile.setPath("///");
        wigFile.setSource("///");
        wigFile.setOwner(EntityHelper.TEST_OWNER);

        long id = wigFileDao.createWigFileId();
        biologicalDataItemDao.createBiologicalDataItem(wigFile);
        wigFile.setBioDataItemId(wigFile.getId());
        wigFile.setId(id);
        wigFileDao.createWigFile(wigFile);

        WigFile loadedFile = wigFileDao.loadWigFile(wigFile.getId());

        assertNotNull(loadedFile);
        assertEquals(wigFile.getOwner(), loadedFile.getOwner());

        wigFileDao.deleteWigFile(loadedFile.getId());

        loadedFile = wigFileDao.loadWigFile(loadedFile.getId());
        assertNull(loadedFile);
    }
}
