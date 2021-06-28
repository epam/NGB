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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.vcf.VcfFileDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfSample;
import com.epam.catgenome.helper.EntityHelper;

/**
 * Source:      VcfFileDaoTest.java
 * Created:     11/11/15, 3:06 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * A test class for VcfFileDao
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class VcfFileDaoTest extends AbstractDaoTest {
    @Autowired
    private VcfFileDao vcfFileDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Override
    public void setup() throws Exception {
        assertNotNull("VcfFileDao isn't provided.", vcfFileDao);
        super.setup();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveLoadVcfFile() {
        VcfFile vcfFile = new VcfFile();

        vcfFile.setId(vcfFileDao.createVcfFileId());
        vcfFile.setName("testFile");
        vcfFile.setType(BiologicalDataItemResourceType.FILE);
        vcfFile.setPath("///");
        vcfFile.setSource("///");
        vcfFile.setCreatedDate(new Date());
        vcfFile.setReferenceId(reference.getId());
        vcfFile.setOwner(EntityHelper.TEST_OWNER);

        BiologicalDataItem index = EntityHelper.createIndex(BiologicalDataItemFormat.VCF_INDEX,
                BiologicalDataItemResourceType.FILE, "////");
        vcfFile.setIndex(index);

        biologicalDataItemDao.createBiologicalDataItem(index);
        final Long realId = vcfFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(vcfFile);
        vcfFileDao.createVcfFile(vcfFile, realId);

        VcfFile loadedFile = vcfFileDao.loadVcfFile(vcfFile.getId());

        assertNotNull(loadedFile);
        assertEquals(vcfFile.getName(), loadedFile.getName());
        assertEquals(vcfFile.getType(), loadedFile.getType());
        assertEquals(vcfFile.getFormat(), loadedFile.getFormat());
        assertEquals(vcfFile.getPath(), loadedFile.getPath());
        assertEquals(vcfFile.getSource(), loadedFile.getPath());
        assertEquals(vcfFile.getIndex().getPath(), loadedFile.getIndex().getPath());
        assertEquals(vcfFile.getCreatedDate(), loadedFile.getCreatedDate());
        assertEquals(vcfFile.getReferenceId(), loadedFile.getReferenceId());
        assertEquals(vcfFile.getBioDataItemId(), loadedFile.getBioDataItemId());
        assertEquals(vcfFile.getOwner(), loadedFile.getOwner());

    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveLoadSamples() {
        VcfFile vcfFile = new VcfFile();

        vcfFile.setId(vcfFileDao.createVcfFileId());
        vcfFile.setName("testFile");
        vcfFile.setCreatedDate(new Date());
        vcfFile.setReferenceId(reference.getId());
        vcfFile.setType(BiologicalDataItemResourceType.FILE);
        vcfFile.setFormat(BiologicalDataItemFormat.VCF);
        vcfFile.setPath("testPath");
        vcfFile.setSource("testPath");
        vcfFile.setOwner(EntityHelper.TEST_OWNER);

        BiologicalDataItem index = EntityHelper.createIndex(BiologicalDataItemFormat.VCF_INDEX,
                BiologicalDataItemResourceType.FILE, "testIndexPath");
        vcfFile.setIndex(index);

        biologicalDataItemDao.createBiologicalDataItem(index);

        final Long realId = vcfFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(vcfFile);
        vcfFileDao.createVcfFile(vcfFile, realId);

        VcfSample sample = new VcfSample("testSample", 0);
        List<VcfSample> samples = Collections.singletonList(sample);

        vcfFileDao.createSamples(samples, vcfFile.getId());

        List<VcfSample> loadedSamples = vcfFileDao.loadSamplesForFile(vcfFile.getId());

        assertNotNull(loadedSamples);
        assertFalse(loadedSamples.isEmpty());
    }
}
