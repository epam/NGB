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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.bucket.BucketDao;
import com.epam.catgenome.entity.bucket.Bucket;

/**
 * {@code BucketManager} This component provides logic, connected for handling S3 buckets
 */
@Service
public class BucketManager {
    @Autowired
    private BucketDao bucketDao;

    /**
     * Registers the bucket in system
     * @param bucket {@code Bucket} to save
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Bucket saveBucket(final Bucket bucket) {
        Assert.notNull(bucket, MessagesConstants.ERROR_NULL_PARAM);
        Assert.notNull(bucket.getAccessKeyId(), MessagesConstants.ERROR_NULL_PARAM);
        Assert.notNull(bucket.getBucketName(), MessagesConstants.ERROR_NULL_PARAM);
        Assert.notNull(bucket.getSecretAccessKey(), MessagesConstants.ERROR_NULL_PARAM);

        bucketDao.createBucket(bucket);
        return bucket;
    }


    /**
     * Loads a persisted {@code Bucket} record by it's ID
     *
     * @param bucketId {@code Long} a bucket ID
     * @return {@code Bucket} instance
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public Bucket loadBucket(final Long bucketId) {
        return bucketDao.loadBucketById(bucketId);
    }

    /**
     * Loads all {@code Bucket} records
     *
     * @return {@code List&lt;Bucket&gt;} instance
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Bucket> loadAllBucket() {
        return bucketDao.loadAllBucket();
    }
}
