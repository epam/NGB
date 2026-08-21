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

import com.epam.catgenome.util.Utils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public class S3ManagerTest {

    private static final String TEST_URL = "s3://bucket/file.bam";
    private static final String TEST_SIGNED_URL =
            "https://bucket.s3.eu-central-1.amazonaws.com/file.bam?X-Amz-Algorithm";

    /**
     * On AWS SDK v1 this stubbed {@code AmazonS3.generatePresignedUrl(bucket, key, expiry)} and
     * matched the bucket and the key in the call itself. v2 splits that into a presigner and a
     * request object, so the same two values are asserted on the captured request instead - along
     * with the lifetime, which v1 had no way to read back.
     */
    @Test
    public void testGenerateUrl() throws MalformedURLException {
        S3Manager s3Manager = Mockito.spy(S3Manager.class);
        S3Presigner mockPresigner = Mockito.mock(S3Presigner.class);
        PresignedGetObjectRequest presigned = Mockito.mock(PresignedGetObjectRequest.class);
        Mockito.doReturn(new URL(TEST_SIGNED_URL)).when(presigned).url();
        Mockito.doReturn(presigned)
                .when(mockPresigner)
                .presignGetObject(Mockito.any(GetObjectPresignRequest.class));
        Mockito.doReturn(mockPresigner).when(s3Manager).getClient();

        String result = s3Manager.generateSingedUrl(TEST_URL);

        assertEquals(TEST_SIGNED_URL, result);
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        Mockito.verify(mockPresigner).presignGetObject(captor.capture());
        assertEquals("bucket", captor.getValue().getObjectRequest().bucket());
        assertEquals("file.bam", captor.getValue().getObjectRequest().key());
        assertEquals(Utils.getTimeForS3URL(), captor.getValue().signatureDuration());
    }


}
