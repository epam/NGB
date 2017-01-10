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

package com.epam.catgenome.controller;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.common.AbstractControllerTest;
import com.epam.catgenome.common.ResponseResult;
import com.epam.catgenome.entity.bucket.Bucket;

/**
 * Source:      BucketControllerTest.java
 * Created:     2/16/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Semen_Dmitriev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class BucketControllerTest extends AbstractControllerTest {
    private static final String SAVE_BUCKET = "/bucket/save";
    private static final String BUCKET_GET = "/bucket/%s/load";
    private static final String LOAD_ALL_BUCKET = "/bucket/loadAll";

    private static final String BUCKET_NAME = "Bucket1";
    private static final String ACCESS_KEY = "assess";
    private static final String SECRET_KEY = "secret";

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void registerBamTest() throws Exception {
        Bucket bucket = new Bucket();
        bucket.setSecretAccessKey(SECRET_KEY);
        bucket.setBucketName(BUCKET_NAME);
        bucket.setAccessKeyId(ACCESS_KEY);

        // save
        ResultActions actions = mvc()
                .perform(post(SAVE_BUCKET).content(getObjectMapper().writeValueAsString(bucket))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        final ResponseResult<Bucket> res = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                Bucket.class));

        Assert.assertNotNull(res.getPayload());
        final Long bucketId = res.getPayload().getId();
        Assert.assertNotNull(bucketId);

        assertEquals(res.getPayload().getBucketName(), bucket.getBucketName());
        assertEquals(res.getPayload().getAccessKeyId(), bucket.getAccessKeyId());
        assertEquals(res.getPayload().getSecretAccessKey(), bucket.getSecretAccessKey());


        // load by id
        actions = mvc()
                .perform(get(String.format(BUCKET_GET, bucketId))
                        .contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<Bucket> bucketRes = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                Bucket.class));

        Assert.assertNotNull(bucketRes.getPayload());
        Bucket testLoadBucket = bucketRes.getPayload();
        assertEquals(testLoadBucket.getBucketName(), bucket.getBucketName());
        assertEquals(testLoadBucket.getAccessKeyId(), bucket.getAccessKeyId());
        assertEquals(testLoadBucket.getSecretAccessKey(), bucket.getSecretAccessKey());

        // load all
        actions = mvc()
                .perform(get(LOAD_ALL_BUCKET))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(jsonPath(JPATH_PAYLOAD).exists())
                .andExpect(jsonPath(JPATH_PAYLOAD).isArray())
                .andExpect(jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<List<Bucket>> bucketAll = getObjectMapper()
                .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                        getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                getTypeFactory().constructParametrizedType(List.class, List.class, Bucket.class)));

        Assert.assertNotNull(bucketAll.getPayload());
        Assert.assertFalse(bucketAll.getPayload().isEmpty());
        assertEquals(bucketAll.getPayload().get(0).getBucketName(), bucket.getBucketName());
    }

}
