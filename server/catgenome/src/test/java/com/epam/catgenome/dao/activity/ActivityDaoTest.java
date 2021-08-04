/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.dao.activity;

import com.epam.catgenome.dao.AbstractDaoTest;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.gene.GeneFileDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.activity.Activity;
import com.epam.catgenome.entity.activity.ActivityType;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.helper.EntityHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class ActivityDaoTest extends AbstractDaoTest {

    private static final String TEST_UID = "uid";
    private static final String TEST_VALUE_NEW = "new";
    private static final String TEST_VALUE_OLD = "old";
    private static final String TEST_FIELD = "field";
    private static final String TEST_USER = "user";

    @Autowired
    private ActivityDao activityDao;

    @Autowired
    private GeneFileDao geneFileDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Test
    @Transactional
    public void shouldCreateReadDeleteActivity() {
        final GeneFile geneFile = createGeneFile();
        final Long itemId = geneFile.getId();

        final Activity expectedActivity = Activity.builder()
                .uid(TEST_UID)
                .itemId(itemId)
                .itemType(BiologicalDataItemFormat.GENE)
                .actionType(ActivityType.CREATE)
                .datetime(LocalDateTime.now())
                .username(TEST_USER)
                .newValue(TEST_VALUE_NEW)
                .oldValue(TEST_VALUE_OLD)
                .field(TEST_FIELD)
                .build();
        activityDao.save(expectedActivity);

        final List<Activity> loaded = activityDao.getByItemIdAndUid(itemId, TEST_UID);
        assertThat(loaded.size(), is(1));
        final Activity actualActivity = loaded.get(0);
        assertThat(actualActivity, is(expectedActivity));

        activityDao.deleteByItemId(itemId);

        assertThat(activityDao.getByItemIdAndUid(itemId, TEST_UID).size(), is(0));
    }

    private GeneFile createGeneFile() {
        final GeneFile geneFile = new GeneFile();
        geneFile.setId(geneFileDao.createGeneFileId());
        geneFile.setName("testFile");
        geneFile.setCreatedDate(new Date());
        geneFile.setReferenceId(reference.getId());
        geneFile.setType(BiologicalDataItemResourceType.FILE);
        geneFile.setFormat(BiologicalDataItemFormat.GENE);
        geneFile.setPath("/path/to/file");
        geneFile.setSource("/path/to/file");
        geneFile.setOwner(EntityHelper.TEST_OWNER);

        final BiologicalDataItem index = EntityHelper.createIndex(BiologicalDataItemFormat.GENE_INDEX,
                BiologicalDataItemResourceType.FILE, "////");
        geneFile.setIndex(index);
        biologicalDataItemDao.createBiologicalDataItem(index);
        final Long realId = geneFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(geneFile);

        geneFileDao.createGeneFile(geneFile, realId);
        return geneFile;
    }
}
