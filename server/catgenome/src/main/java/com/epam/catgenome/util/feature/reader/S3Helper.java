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

    InputStream objectData = null;

    public InputStream openInputStream() throws IOException {

        try {
            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
            LOGGER.debug("Downloading an object");
            S3Object s3Object = s3Client.getObject(new GetObjectRequest((s3URI.getBucket()), s3URI.getKey()));
            objectData = s3Object.getObjectContent();
            LOGGER.debug("Content-Type: " + s3Object.getObjectMetadata().getContentType());// Process the objectData stream.

        } catch (AmazonServiceException ase) {
            LOGGER.debug("Caught an AmazonServiceException, which means your request made it " +
                    "to Amazon S3, but was rejected with an error response for some reason.");
            LOGGER.debug("Error Message:    " + ase.getMessage());
            LOGGER.debug("HTTP Status Code: " + ase.getStatusCode());
            LOGGER.debug("AWS Error Code:   " + ase.getErrorCode());
            LOGGER.debug("Error Type:       " + ase.getErrorType());
            LOGGER.debug("Request ID:       " + ase.getRequestId());

        } catch (AmazonClientException ace) {
            LOGGER.debug("Caught an AmazonClientException, which means the client encountered an internal error while trying to " +
                    "communicate with S3, such as not being able to access the network.");
            LOGGER.debug("Error Message: " + ace.getMessage());

        } finally {
            objectData.close();
        }

        return objectData;
    }

    @Deprecated
    public InputStream openInputStreamForRange(long start, long end) throws IOException {

        try {
            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
            LOGGER.debug("Downloading an object");
            GetObjectRequest rangeObjectRequest = new GetObjectRequest((s3URI.getBucket()), s3URI.getKey());
            rangeObjectRequest.setRange(start, end); // retrieving selected bytes.
            S3Object objectPortion = s3Client.getObject(rangeObjectRequest);

            objectData = objectPortion.getObjectContent();
        } catch (AmazonServiceException ase) {
            LOGGER.debug("Caught an AmazonServiceException, which means your request made it " +
                    "to Amazon S3, but was rejected with an error response for some reason.");
            LOGGER.debug("Error Message:    " + ase.getMessage());
            LOGGER.debug("HTTP Status Code: " + ase.getStatusCode());
            LOGGER.debug("AWS Error Code:   " + ase.getErrorCode());
            LOGGER.debug("Error Type:       " + ase.getErrorType());
            LOGGER.debug("Request ID:       " + ase.getRequestId());

        } catch (AmazonClientException ace) {
            LOGGER.debug("Caught an AmazonClientException, which means the client encountered an internal error while trying to " +
                    "communicate with S3, such as not being able to access the network.");
            LOGGER.debug("Error Message: " + ace.getMessage());

        } finally {
            objectData.close();
        }

        return objectData;

    }

}
