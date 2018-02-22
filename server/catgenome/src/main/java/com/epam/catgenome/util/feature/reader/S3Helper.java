package com.epam.catgenome.util.feature.reader;

import com.amazonaws.AmazonClientException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import net.sf.ehcache.pool.sizeof.annotations.IgnoreSizeOf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.InputStream;


/**
 * Helper class for handling S3 URLs.
 */
public class S3Helper {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Helper.class);


    public S3Helper(AmazonS3URI s3URI) {
        this.s3URI = s3URI;
    }

    AmazonS3URI s3URI;

    @IgnoreSizeOf
    private InputStream objectData;

    private static S3Object s3Object;


    public InputStream openInputStream() throws AmazonClientException {

        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        LOGGER.debug("Downloading an object");
        s3Object = s3Client.getObject(new GetObjectRequest((s3URI.getBucket()), s3URI.getKey()));

        objectData = s3Object.getObjectContent();
        LOGGER.debug("Content-Type: " + s3Object.getObjectMetadata().getContentType());// Process the objectData stream

        return objectData;
    }


    @Deprecated
    public InputStream openInputStreamForRange(long start, long end) throws AmazonClientException {

        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        LOGGER.debug("Downloading an object");
        GetObjectRequest rangeObjectRequest = new GetObjectRequest((s3URI.getBucket()), s3URI.getKey());
        rangeObjectRequest.setRange(start, end); // retrieving selected bytes.
        S3Object s3Object = s3Client.getObject(rangeObjectRequest);

        objectData = s3Object.getObjectContent();

        return objectData;
    }

    public static long getContentLength() {
        return s3Object.getObjectMetadata().getContentLength();
    }

}
