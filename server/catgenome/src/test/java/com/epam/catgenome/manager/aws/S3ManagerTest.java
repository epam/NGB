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

import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class S3ManagerTest {

    private static final String TEST_URL = "s3://bucket/file.bam";
    private static final String TEST_SIGNED_URL =
            "https://bucket.s3.eu-central-1.amazonaws.com/file.bam?X-Amz-Algorithm";

    @Test
    public void testGenerateUrl() throws MalformedURLException {
        S3Manager s3Manager = Mockito.spy(S3Manager.class);
        S3Presigner mockPresigner = Mockito.mock(S3Presigner.class);

        // Use reflection to set the private presigner field
        try {
            java.lang.reflect.Field field = S3Manager.class.getDeclaredField("presigner");
            field.setAccessible(true);
            field.set(s3Manager, mockPresigner);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set presigner field", e);
        }

        PresignedGetObjectRequest mockPresignedRequest = Mockito.mock(PresignedGetObjectRequest.class);
        Mockito.when(mockPresignedRequest.url()).thenReturn(new URL(TEST_SIGNED_URL));

        Mockito.doReturn(mockPresignedRequest)
                .when(mockPresigner)
                .presignGetObject(Mockito.any(Consumer.class));

        String result = s3Manager.generateSingedUrl(TEST_URL);
        assertEquals(TEST_SIGNED_URL, result);
    }
}
