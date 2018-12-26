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

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.gene.GeneFileDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.helper.EntityHelper;

/**
 * Source:      GeneDaoTest
 * Created:     05.12.15, 12:55
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class GeneDaoTest extends AbstractDaoTest {
    @Autowired
    private GeneFileDao geneFileDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Override
    public void setup() throws Exception {
        assertNotNull("GeneFileDao isn't provided.", geneFileDao);
        super.setup();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveLoadGeneFile() {
        GeneFile geneFile = new GeneFile();

        geneFile.setId(geneFileDao.createGeneFileId());
        geneFile.setName("testFile");
        geneFile.setCreatedDate(new Date());
        geneFile.setReferenceId(reference.getId());
        geneFile.setType(BiologicalDataItemResourceType.FILE);
        geneFile.setFormat(BiologicalDataItemFormat.GENE);
        geneFile.setPath("///");
        geneFile.setOwner(EntityHelper.TEST_OWNER);

        BiologicalDataItem index = EntityHelper.createIndex(BiologicalDataItemFormat.GENE_INDEX,
                BiologicalDataItemResourceType.FILE, "////");
        geneFile.setIndex(index);

        biologicalDataItemDao.createBiologicalDataItem(index);
        final Long realId = geneFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(geneFile);
        geneFileDao.createGeneFile(geneFile, realId);

        GeneFile loadedFile = geneFileDao.loadGeneFile(geneFile.getId());

        assertNotNull(loadedFile);
        assertEquals(geneFile.getOwner(), loadedFile.getOwner());

    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadGeneFiles() {
        GeneFile geneFile = new GeneFile();

        geneFile.setId(geneFileDao.createGeneFileId());
        geneFile.setName("testFile");
        geneFile.setCreatedDate(new Date());
        geneFile.setReferenceId(reference.getId());
        geneFile.setType(BiologicalDataItemResourceType.FILE);
        geneFile.setFormat(BiologicalDataItemFormat.GENE);
        geneFile.setPath("///");
        geneFile.setOwner(EntityHelper.TEST_OWNER);

        BiologicalDataItem index = EntityHelper.createIndex(BiologicalDataItemFormat.GENE_INDEX,
                BiologicalDataItemResourceType.FILE, "////");
        geneFile.setIndex(index);

        biologicalDataItemDao.createBiologicalDataItem(index);
        final Long realId = geneFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(geneFile);
        geneFileDao.createGeneFile(geneFile, realId);

        List<Long> ids = Arrays.asList(null, geneFile.getId());

        List<GeneFile> geneFiles = geneFileDao.loadGeneFiles(ids);
        Assert.assertFalse(geneFiles.isEmpty());
    }
}
