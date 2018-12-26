/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.catgenome.manager.aws;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.Test;
import org.mockito.Mockito;

public class S3ManagerTest {

    private static final String TEST_URL = "s3://bucket/file.bam";
    private static final String TEST_SIGNED_URL =
            "https://bucket.s3.eu-central-1.amazonaws.com/file.bam?X-Amz-Algorithm";

    @Test
    public void testGenerateUrl() throws MalformedURLException {
        S3Manager s3Manager = Mockito.spy(S3Manager.class);
        AmazonS3 mockClient = Mockito.mock(AmazonS3.class);
        Mockito.doReturn(new URL(TEST_SIGNED_URL))
                .when(mockClient)
                .generatePresignedUrl(
                        Mockito.eq("bucket"),
                        Mockito.eq("file.bam"),
                        Mockito.any(Date.class));
        Mockito.doReturn(mockClient).when(s3Manager).getClient();
        String result = s3Manager.generateSingedUrl(TEST_URL);
        assertEquals(TEST_SIGNED_URL, result);
    }


}
