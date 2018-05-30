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

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.epam.catgenome.exception.S3ReadingException;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

/**
 * Class for working with AWS S3 buckets
 */
@Service
public class S3Manager {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Manager.class);
    private static final String S3_SCHEME = "s3://";
    private static final String DELIMITER = "/";
    //in minutes
    private static final int S3_LINK_EXPIRATION = 60;

    @Value("#{catgenome['path.style.access.enabled'] ?: false}")
    private boolean pathStyleAccessEnabled;


    public String processUrl(String inputUrl) {
        if (!inputUrl.startsWith(S3_SCHEME)) {
            return inputUrl;
        }
        return generateSingedUrl(inputUrl);
    }

    public String generateSingedUrl(String inputUrl) {
        try {
            AmazonS3 s3Client = getClient();
            URI parsedUrl = new URI(inputUrl);
            URL url = s3Client.generatePresignedUrl(parsedUrl.getHost(),
                    normalizePath(parsedUrl.getPath()), getTimeForS3URL());
            return url.toExternalForm();
        } catch (AmazonClientException | URISyntaxException e) {
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

    AmazonS3 getClient() {
        return AmazonS3ClientBuilder.standard().withPathStyleAccessEnabled(pathStyleAccessEnabled).build();
    }

    /**
     * Makes time for S3 URL access
     * @return a {@link Date} object, representing time for S3 URL access
     */
    public Date getTimeForS3URL() {
        return DateUtils.addMinutes(new Date(), S3_LINK_EXPIRATION);
    }

}
