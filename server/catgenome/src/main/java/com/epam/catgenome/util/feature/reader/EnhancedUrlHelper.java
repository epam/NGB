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

package com.epam.catgenome.util.feature.reader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Pattern;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import htsjdk.tribble.util.FTPHelper;
import htsjdk.tribble.util.HTTPHelper;
import htsjdk.tribble.util.URLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * URL Helper class that supports S3 singed url handling
 */
public class EnhancedUrlHelper implements URLHelper {

    private static final Pattern S3_PATTERN = Pattern.compile(".*s3.*\\.amazonaws\\.com");

    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedUrlHelper.class);
    private URLHelper wrappedHelper;

    public EnhancedUrlHelper(URL url) {
        String protocol = url.getProtocol().toLowerCase();
        if (S3_PATTERN.matcher(url.getHost()).matches()) {
            LOGGER.debug("Creating S3Helper here. Url:" + url);
            this.wrappedHelper = new S3Helper(url);
        } else if (protocol.startsWith("http")) {
            this.wrappedHelper = new HTTPHelper(url);
        } else if (protocol.startsWith("ftp")) {
            this.wrappedHelper = new FTPHelper(url);
        } else {
            throw new IllegalArgumentException(
                    "Unable to create helper for url with protocol " + protocol);
        }
    }

    @Override
    public URL getUrl() {
        return this.wrappedHelper.getUrl();
    }

    @Override
    public long getContentLength() throws IOException {
        return this.wrappedHelper.getContentLength();
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return this.wrappedHelper.openInputStream();
    }

    @Override
    @Deprecated
    public InputStream openInputStreamForRange(long start, long end)
            throws IOException {
        return this.wrappedHelper.openInputStreamForRange(start, end);
    }

    @Override
    public boolean exists() throws IOException {
        return this.wrappedHelper.exists();
    }

    /**
     * Inner helper class for handling S3 signed URLs. We sign URLs for GET requests so
     * HEAD request will return 403, this is considered to be OK in this case
     */
    public static class S3Helper implements URLHelper {

        public S3Helper(URL inputUrl) {
            this.inputUrl = inputUrl;
        }

        URL inputUrl = null;

        AmazonS3URI s3URI = new AmazonS3URI(inputUrl.toString());

        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

        InputStream objectData = null;

        private String bucketName = s3URI.getBucket();

        private String key = s3URI.getKey();


        @Override
        public URL getUrl() {
            return inputUrl;
        }


        @Override
        public InputStream openInputStream() throws IOException {

            try {
                System.out.println("Downloading an object");
                S3Object s3Object = s3Client.getObject(new GetObjectRequest(bucketName, key));
                objectData = s3Object.getObjectContent();
                System.out.println("Content-Type: " + s3Object.getObjectMetadata().getContentType());// Process the objectData stream.

            } catch (AmazonServiceException ase) {
                System.out.println("Caught an AmazonServiceException, which means your request made it " +
                        "to Amazon S3, but was rejected with an error response for some reason.");
                System.out.println("Error Message:    " + ase.getMessage());
                System.out.println("HTTP Status Code: " + ase.getStatusCode());
                System.out.println("AWS Error Code:   " + ase.getErrorCode());
                System.out.println("Error Type:       " + ase.getErrorType());
                System.out.println("Request ID:       " + ase.getRequestId());

            } catch (AmazonClientException ace) {
                System.out.println("Caught an AmazonClientException, which means the client encountered an internal error while trying to " +
                        "communicate with S3, such as not being able to access the network.");
                System.out.println("Error Message: " + ace.getMessage());

            } finally {
                objectData.close();
            }

            return objectData;
        }

        @Deprecated
        public InputStream openInputStreamForRange(long start, long end) throws IOException {
            InputStream objectData = null;
            try {
                System.out.println("Downloading an object");
                GetObjectRequest rangeObjectRequest = new GetObjectRequest(bucketName, key);
                rangeObjectRequest.setRange(start, end); // retrieving selected bytes.
                S3Object objectPortion = s3Client.getObject(rangeObjectRequest);

                objectData = objectPortion.getObjectContent();
            } catch (AmazonServiceException ase) {
                    System.out.println("Caught an AmazonServiceException, which means your request made it " +
                            "to Amazon S3, but was rejected with an error response for some reason.");
                    System.out.println("Error Message:    " + ase.getMessage());
                    System.out.println("HTTP Status Code: " + ase.getStatusCode());
                    System.out.println("AWS Error Code:   " + ase.getErrorCode());
                    System.out.println("Error Type:       " + ase.getErrorType());
                    System.out.println("Request ID:       " + ase.getRequestId());

            } catch (AmazonClientException ace) {
                    System.out.println("Caught an AmazonClientException, which means the client encountered an internal error while trying to " +
                            "communicate with S3, such as not being able to access the network.");
                    System.out.println("Error Message: " + ace.getMessage());

                } finally {
                    objectData.close();
                }

                return objectData;

            }

        @Override
        public boolean exists() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getContentLength() throws IOException {
            throw new UnsupportedOperationException();
        }
        public AmazonS3 getS3Client() {
            return s3Client;
        }

        public URL getInputUrl() {
            return inputUrl;
        }

        public AmazonS3URI getS3URI() {
            return s3URI;
        }

    }

}
