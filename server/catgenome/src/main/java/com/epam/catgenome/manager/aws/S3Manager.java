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
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Class for working with AWS S3 buckets
 */
public class S3Manager {

    private static volatile S3Manager instance;

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Manager.class);
    private static final String DELIMITER = "/";

    @Value("#{catgenome['path.style.access.enabled'] ?: false}")
    private boolean pathStyleAccessEnabled;

    @Autowired
    public static void setInstance(S3Manager s3Manager) {
        instance = s3Manager;
    }

    public S3Manager() {
        instance = this;
    }

    public static String generateSignedUrl(String inputUrl) {
        return instance.generateSingedUrl(inputUrl);
    }
    public String generateSingedUrl(String inputUrl) {
        try (S3Presigner presigner = getClient()) {
            URI parsedUrl = new URI(inputUrl);
            return presigner.presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(Utils.getTimeForS3URL())
                            .getObjectRequest(GetObjectRequest.builder()
                                    .bucket(parsedUrl.getHost())
                                    .key(normalizePath(parsedUrl.getPath()))
                                    .build())
                            .build())
                    .url()
                    .toExternalForm();
        } catch (SdkException | URISyntaxException e) {
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

    /**
     * An {@link S3Presigner} rather than the {@code AmazonS3} this returned until Phase 8: on AWS
     * SDK v2 signing a URL is no longer something the S3 client itself does. Like the client it
     * replaces it is built per call and closed by the caller, and it resolves its region and
     * credentials from the default provider chain.
     */
    S3Presigner getClient() {
        return S3Presigner.builder()
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccessEnabled)
                        .build())
                .build();
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
