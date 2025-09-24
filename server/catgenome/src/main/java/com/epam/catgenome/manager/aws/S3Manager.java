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

import com.epam.catgenome.exception.S3ReadingException;
import com.epam.catgenome.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Uri;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

public class S3Manager {

    private static volatile S3Manager instance;

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Manager.class);
    private static final String DELIMITER = "/";

    @Value("#{catgenome['path.style.access.enabled'] ?: false}")
    private boolean pathStyleAccessEnabled;

    private final S3Presigner presigner;

    @Autowired
    public static void setInstance(S3Manager s3Manager) {
        S3Manager.instance = s3Manager;
    }

    public S3Manager() {
        this.presigner = S3Presigner.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.AWS_GLOBAL)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccessEnabled)
                        .build())
                .build();
        instance = this;
    }

    public static String generateSignedUrl(String inputUrl) {
        return instance.generateSingedUrl(inputUrl);
    }

    public String generateSingedUrl(String inputUrl) {
        try {
            S3Uri s3Uri = parseS3Uri(inputUrl);
            String bucket = s3Uri.bucket().orElseThrow(() ->
                    new IllegalArgumentException("Invalid bucket in URI: " + inputUrl));
            String key = s3Uri.key().orElseThrow(() ->
                    new IllegalArgumentException("Invalid key in URI: " + inputUrl));

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(normalizePath(key))
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(r -> r
                    .getObjectRequest(getObjectRequest)
                    .signatureDuration(Duration.ofMillis(Utils.getTimeForS3URL().getTime())));

            return presignedRequest.url().toString();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new S3ReadingException(inputUrl, e);
        }
    }

    private String normalizePath(String path) {
        if (path.startsWith(DELIMITER)) {
            return path.substring(1);
        } else {
            return path;
        }
    }

    private S3Uri parseS3Uri(String uri) throws URISyntaxException {
        return S3Uri.builder().uri(new URI(uri)).build();
    }

    public static S3Manager singleton() {
        S3Manager s3Manager = instance;
        if (s3Manager == null) {
            synchronized (S3Manager.class) {
                s3Manager = instance;
                if (s3Manager == null) {
                    instance = new S3Manager();
                    s3Manager = instance;
                }
            }
        }
        return s3Manager;
    }
}
