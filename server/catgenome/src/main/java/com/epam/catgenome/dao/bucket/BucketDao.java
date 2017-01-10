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

package com.epam.catgenome.dao.bucket;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.bucket.Bucket;

/**
 * {@code BucketDao} is a DAO component, that handles database interaction with bucket metadata.
 */
public class BucketDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;

    private String bucketName;
    private String createBucketQuery;
    private String loadBucketByIdQuery;
    private String loadAllBucketQuery;

    /**
     * Creates a new ID for a {@code Bucket} instance
     *
     * @return {@code Long} new {@code Bucket} ID
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long createBucketFileId() {
        return daoHelper.createId(bucketName);
    }

    /**
     * Persists {@code Bucket} record to the database
     * @param bucket a {@code Bucket} instance to be persisted
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createBucket(Bucket bucket) {
        bucket.setId(createBucketFileId());
        getNamedParameterJdbcTemplate().update(createBucketQuery, BucketParameters.getParameters(bucket));
    }

    /**
     * Loads all {@code Bucket} records saved in the database
     * @return a {@code List} of {@code Bucket} instances
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Bucket> loadAllBucket() {
        return getNamedParameterJdbcTemplate().query(loadAllBucketQuery,
                BucketParameters.getRowMapper());
    }

    /**
     * Loads a persisted {@code Bucket} record, specified by it's ID
     * @param bucketId a Bucket ID
     * @return {@code Bucket} instance
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Bucket loadBucketById(final Long bucketId) {
        List<Bucket> buckets = getJdbcTemplate().query(loadBucketByIdQuery, BucketParameters.getPasswordRowMapper(),
                bucketId);
        return buckets.isEmpty() ? null : buckets.get(0);
    }

    enum BucketParameters {
        BUCKET_ID,
        BUCKET_NAME,
        ACCESS_KEY_ID,
        SECRET_ACCESS_KEY;

        static MapSqlParameterSource getParameters(Bucket bucket) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(BUCKET_ID.name(), bucket.getId());
            params.addValue(BUCKET_NAME.name(), bucket.getBucketName());
            params.addValue(ACCESS_KEY_ID.name(), bucket.getAccessKeyId());
            params.addValue(SECRET_ACCESS_KEY.name(), bucket.getSecretAccessKey());

            return params;
        }

        static RowMapper<Bucket> getRowMapper() {
            return (rs, rowNum) -> {
                Bucket bucket = new Bucket();

                bucket.setId(rs.getLong(BUCKET_ID.name()));
                bucket.setBucketName(rs.getString(BUCKET_NAME.name()));
                return bucket;
            };
        }

        static RowMapper<Bucket> getPasswordRowMapper() {
            return (rs, rowNum) -> {
                Bucket bucket = new Bucket();

                bucket.setId(rs.getLong(BUCKET_ID.name()));
                bucket.setBucketName(rs.getString(BUCKET_NAME.name()));
                bucket.setAccessKeyId(rs.getString(ACCESS_KEY_ID.name()));
                bucket.setSecretAccessKey(rs.getString(SECRET_ACCESS_KEY.name()));

                return bucket;
            };
        }
    }


    @Required
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Required
    public void setCreateBucketQuery(String createBucketQuery) {
        this.createBucketQuery = createBucketQuery;
    }

    @Required
    public void setLoadBucketByIdQuery(String loadBucketByIdQuery) {
        this.loadBucketByIdQuery = loadBucketByIdQuery;
    }

    @Required
    public void setLoadAllBucketQuery(String loadAllBucketQuery) {
        this.loadAllBucketQuery = loadAllBucketQuery;
    }
}
