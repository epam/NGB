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

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.dao.bucket.BucketDao;
import com.epam.catgenome.entity.bucket.Bucket;
import com.epam.catgenome.helper.EntityHelper;

/**
 * Source:      BucketDaoTest.java
 * Created:     2/16/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Semen_Dmitriev
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BucketDaoTest extends AbstractDaoTest {

    @Autowired
    private BucketDao bucketDao;

    private static final String BUCKET_NAME = "Bucket1";
    private static final String ACCESS_KEY = "assess";
    private static final String SECRET_KEY = "secret";

    @Override
    public void setup() throws Exception {
        assertNotNull("BucketDao isn't provided.", bucketDao);
        super.setup();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void createBucketIdTest() {
        final Long testId = bucketDao.createBucketFileId();
        Assert.notNull(testId);
        Assert.isTrue(testId > 0);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveLoadBucketTest() {
        Bucket bucket = new Bucket();

        bucket.setBucketName(BUCKET_NAME);
        bucket.setAccessKeyId(ACCESS_KEY);
        bucket.setSecretAccessKey(SECRET_KEY);
        bucket.setOwner(EntityHelper.TEST_OWNER);

        bucketDao.createBucket(bucket);

        final Bucket loadedBucket = bucketDao.loadBucketById(bucket.getId());

        assertNotNull(loadedBucket);
        assertEquals(loadedBucket.getBucketName(), bucket.getBucketName());
        assertEquals(loadedBucket.getAccessKeyId(), bucket.getAccessKeyId());
        assertEquals(loadedBucket.getSecretAccessKey(), bucket.getSecretAccessKey());
        assertEquals(bucket.getOwner(), loadedBucket.getOwner());

        List<Bucket> bucketList = bucketDao.loadAllBucket();

        assertFalse(bucketList.isEmpty());
    }
}

