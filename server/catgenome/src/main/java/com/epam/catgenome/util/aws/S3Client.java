package com.epam.catgenome.util.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import htsjdk.samtools.util.Log;
import org.apache.http.HttpStatus;

/**
 * Class provides configuration of AWS client and utility methods for S3
 */
public class S3Client {

    private static final Log LOG = Log.getInstance(S3Client.class);

    private static final int TIMEOUT = 10_000;
    private static final int MAX_RETRY = 10;
    private final AWSCredentialsProviderChain providerChain;
    private final AmazonS3 aws;

    public S3Client() {
        providerChain = new DefaultAWSCredentialsProviderChain();
        aws = configureAWS();
    }

    /**
     * A method that returns true if there are valid credentials set and false otherwise.
     * @return a boolean value that shows whether there are valid credentials set.
     */
    boolean credentialsExist() {
        try {
            providerChain.getCredentials();
        } catch (AmazonClientException e) {
            LOG.warn(e, "Credentials not found, anonymous credentials will be used!");
            return false;
        }
        return true;
    }

    /**
     * A method that returns true if a correct s3 URI was provided and false otherwise.
     *
     * @param uri The provided URI for the file.
     * @return a boolean value that shows whether the correct URI was provided
     */
    public boolean isFileExisting(AmazonS3URI uri) {

        boolean exist = true;

        try {
            aws.getObjectMetadata(uri.getBucket(), uri.getKey());
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
     * A method that returns the file size.
     *
     * @param amazonURI An s3 URI
     * @return long value of the file size in bytes
     */
    long getFileSize(AmazonS3URI amazonURI){
        return aws
                .getObjectMetadata(amazonURI.getBucket(), amazonURI.getKey())
                .getContentLength();
    }

    private AmazonS3 configureAWS() {
        ClientConfiguration configuration = new ClientConfiguration()
                .withMaxConnections(Configuration.getNumberOfConnections())
                .withMaxErrorRetry(MAX_RETRY)
                .withConnectionTimeout(TIMEOUT)
                .withSocketTimeout(TIMEOUT)
                .withTcpKeepAlive(true);

        if (credentialsExist()) {
            return new AmazonS3Client(providerChain, configuration);
        } else {
            return new AmazonS3Client(new AnonymousAWSCredentials(), configuration);
        }
    }

    AmazonS3 getAws() {
        return aws;
    }
}
