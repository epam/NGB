package com.epam.catgenome.util.aws;


import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Class provides configuration of AWS client and utility methods for S3
 */
public final class S3Client {

    private static final int CACHE_SIZE = 1000;

    private final AmazonS3 s3;
    private final AmazonS3 swiftStack;

    private static S3Client instance;

    private final LoadingCache<AmazonS3URI, Long> fileSizes = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(
                    new CacheLoader<AmazonS3URI, Long>() {
                        public Long load(AmazonS3URI key) {
                            return getAws(key)
                                    .getObjectMetadata(key.getBucket(), key.getKey())
                                    .getContentLength();
                        }
                    });


    private S3Client(final String swsEndpoint) {
        s3 = AmazonS3ClientBuilder.standard().enableForceGlobalBucketAccess().build();
        if (!StringUtils.isEmpty(swsEndpoint)) {
            swiftStack = AmazonS3ClientBuilder.standard().enableForceGlobalBucketAccess()
                    .withCredentials(
                            new AWSCredentialsProviderChain(
                                    new EnvironmentVariableCredentialsProvider(),
                                    new SystemPropertiesCredentialsProvider(),
                                    new EC2ContainerCredentialsProviderWrapper(),
                                    new ProfileCredentialsProvider("sws"))
                    ).build();
            swiftStack.setEndpoint(swsEndpoint);
        } else {
            swiftStack = null;
        }
    }

    public static synchronized S3Client configure(String swsEndpoint) {
        instance = new S3Client(swsEndpoint);
        return instance;
    }

    public static synchronized  S3Client getInstance() {
        if (instance == null) {
            throw new IllegalArgumentException("S3Client is not configured!");
        }
        return instance;
    }

    private AmazonS3 getAws(AmazonS3URI object) {
        if (object.toString().startsWith("sws")) {
            if (swiftStack == null) {
                throw new IllegalArgumentException("Swift Stack client is not configured!");
            }
            return swiftStack;
        }
        return s3;
    }

    /**
     * A method that returns true if a correct s3 URI was provided and false otherwise.
     *
     * @param uri The provided URI for the file.
     * @return a boolean value that shows whether the correct URI was provided
     */
    private boolean isFileExisting(AmazonS3URI uri) {

        boolean exist = true;

        try {
            getAws(uri).getObjectMetadata(uri.getBucket(), uri.getKey());
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == HttpStatus.SC_FORBIDDEN
                    || e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                exist = false;
            } else {
                throw e;
            }
        }
        return exist;
    }

    /**
     * A method that returns true if a correct s3 URI was provided and false otherwise.
     *
     * @param uri The provided URI for the file.
     * @return a boolean value that shows whether the correct URI was provided
     */
    public boolean isFileExisting(String uri) {
        return isFileExisting(new AmazonS3URI(uri));
    }

    /**
     * A method that returns the file size.
     *
     * @param amazonURI An s3 URI
     * @return long value of the file size in bytes
     */
    public long getFileSize(AmazonS3URI amazonURI){
        return fileSizes.getUnchecked(amazonURI);
    }

    /**
     * A method that creates an InputStream on a specific range of the file.
     * InputStream classes wrapping order can be reversed.
     *
     * @param obj    target file URI
     * @param offset range start position
     * @param end    range end position
     * @return an InputStream object on the specific range of the file.
     */
    public InputStream loadFromTo(AmazonS3URI obj, long offset, long end) {
        GetObjectRequest rangeObjectRequest = new GetObjectRequest(obj.getBucket(), obj.getKey());
        rangeObjectRequest.setRange(offset, end);
        S3Object s3Object = getAws(obj).getObject(rangeObjectRequest);
        S3ObjectInputStream objectStream = s3Object.getObjectContent();
        return new BufferedInputStream(objectStream);
    }

    /**
     * A method that creates an InputStream on a range
     * from a specific position to the end of the file.
     *
     * @param obj    target file URI
     * @param offset range start position
     * @return an InputStream object on the specific range of the file.
     */
    @SuppressWarnings("WeakerAccess")
    public InputStream loadFrom(AmazonS3URI obj,
                                       @SuppressWarnings("SameParameterValue") long offset) {
        long contentLength = S3Client.getInstance().getFileSize(obj);
        return loadFromTo(obj, offset, contentLength);
    }

    /**
     * A method that creates an InputStream on a specific file URI.
     *
     * @param obj target file URI
     * @return an InputStream object on the file URI.
     */
    @SuppressWarnings("WeakerAccess")
    public InputStream loadFully(AmazonS3URI obj) {
        return loadFrom(obj, 0);
    }

}
