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


package com.epam.catgenome.util.aws;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.amazonaws.AmazonClientException;
import com.epam.catgenome.exception.S3ReadingException;
import com.epam.catgenome.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for work with AWS S3 buckets
 * became unnecessary in new S3 implementation
 */
public class S3Manager {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Manager.class);
    private static final String DELIMITER = "/";

    public String generateSingedUrl(String inputUrl) {
        try  {
            LOGGER.debug("Input url is:" + inputUrl);
            URI parsedUrl = new URI(inputUrl);
            URL url = S3Client.getAws().generatePresignedUrl(parsedUrl.getHost(),
                    normalizePath(parsedUrl.getPath()), Utils.getTimeForS3URL());
            String urlInExternalForm = url.toExternalForm();
            LOGGER.debug("Creating signed url here:" + urlInExternalForm);
            return urlInExternalForm;
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
}
