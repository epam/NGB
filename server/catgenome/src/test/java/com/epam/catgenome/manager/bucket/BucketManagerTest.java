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

package com.epam.catgenome.manager.bucket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.entity.bucket.Bucket;

/**
 * Source:      BucketManagerTest.java
 * Created:     2/16/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Semen_Dmitriev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BucketManagerTest extends AbstractManagerTest {

    @Autowired
    private BucketManager bucketManager;

    private static final String BUCKET_NAME = "Bucket1";
    private static final String ACCESS_KEY = "assess";
    private static final String SECRET_KEY = "secret";

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveBucket() throws IOException {
        Bucket bucket = new Bucket();
        bucket.setSecretAccessKey(SECRET_KEY);
        bucket.setBucketName(BUCKET_NAME);
        bucket.setAccessKeyId(ACCESS_KEY);
        bucket = bucketManager.saveBucket(bucket);

        final Bucket loadBucket = bucketManager.loadBucket(bucket.getId());

        assertNotNull(loadBucket);
        assertEquals(loadBucket.getBucketName(), bucket.getBucketName());
        assertEquals(loadBucket.getAccessKeyId(), bucket.getAccessKeyId());
        assertEquals(loadBucket.getSecretAccessKey(), bucket.getSecretAccessKey());

        List<Bucket> bucketList = bucketManager.loadAllBucket();

        assertFalse(bucketList.isEmpty());
    }
}
