package com.epam.catgenome.util.feature.reader;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;


/**
 * Inner helper class for handling S3 signed URLs. We sign URLs for GET requests so
 * HEAD request will return 403, this is considered to be OK in this case
 */
public class S3Helper {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Helper.class);

    public S3Helper(AmazonS3URI s3URI) {
        this.s3URI = s3URI;
    }

    AmazonS3URI s3URI;


    public InputStream openInputStream() throws AmazonClientException {

        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        LOGGER.debug("Downloading an object");
        S3Object s3Object = s3Client.getObject(new GetObjectRequest((s3URI.getBucket()), s3URI.getKey()));
        InputStream objectData = s3Object.getObjectContent();
        LOGGER.debug("Content-Type: " + s3Object.getObjectMetadata().getContentType());// Process the objectData stream

        return objectData;
    }


    @Deprecated
    public InputStream openInputStreamForRange(long start, long end) throws AmazonClientException {

        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        LOGGER.debug("Downloading an object");
        GetObjectRequest rangeObjectRequest = new GetObjectRequest((s3URI.getBucket()), s3URI.getKey());
        rangeObjectRequest.setRange(start, end); // retrieving selected bytes.
        S3Object objectPortion = s3Client.getObject(rangeObjectRequest);

        InputStream objectData = objectPortion.getObjectContent();

        return objectData;
    }

}
