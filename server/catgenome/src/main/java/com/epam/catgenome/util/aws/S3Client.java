package com.epam.catgenome.util.aws;


import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.epam.catgenome.entity.BiologicalDataItemDownloadUrl;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.epam.catgenome.util.QueryUtils.buildContentDispositionHeader;

/**
 * Class provides configuration of AWS client and utility methods for S3
 */
public final class S3Client {

    private static final int CACHE_SIZE = 1000;
    private static final Long URL_EXPIRATION = 24 * 60 * 60 * 1000L;
    private static final Logger LOGGER = LoggerFactory.getLogger(S3Client.class);


    private AmazonS3 s3;
    private final AmazonS3 swiftStack;

    private static S3Client instance;

    private final LoadingCache<String, Long> fileSizes = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(
                    new CacheLoader<String, Long>() {
                        public Long load(String key) {
                            CloudType cloudType = getCloudType(key);
                            AmazonS3URI obj = new AmazonS3URI(replaceSchema(key));
                            return getAws(cloudType)
                                    .getObjectMetadata(obj.getBucket(), obj.getKey())
                                    .getContentLength();
                        }
                    });


    private S3Client(final String swsEndpoint, String swsRegion, boolean pathStyleAccess) {
        try {
            s3 = AmazonS3ClientBuilder.standard().build();
        } catch (SdkClientException e) {
            LOGGER.warn("Unable to create S3 client, S3 services will be unavailable.", e);
            s3 = null;
        }
        if (!StringUtils.isEmpty(swsEndpoint) && !StringUtils.isEmpty(swsRegion)) {
            AmazonS3ClientBuilder swsClientBuilder = AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(swsEndpoint, swsRegion))
                    .withCredentials(new AWSCredentialsProviderChain(new ProfileCredentialsProvider("sws")));
            swsClientBuilder.setPathStyleAccessEnabled(pathStyleAccess);
            swiftStack = swsClientBuilder.build();
        } else {
            swiftStack = null;
        }
    }

    public static synchronized S3Client configure(String swsEndpoint, String swsRegion, boolean pathStyleAccess) {
        instance = new S3Client(swsEndpoint, swsRegion, pathStyleAccess);
        return instance;
    }

    public static synchronized  S3Client getInstance() {
        if (instance == null) {
            throw new IllegalArgumentException("S3Client is not configured!");
        }
        return instance;
    }

    public static boolean isS3Source(String inputUrl) {
        return inputUrl.startsWith(CloudType.S3.protocol) || inputUrl.startsWith(CloudType.SWS.protocol);
    }

    private AmazonS3 getAws(CloudType cloudtype) {
        switch (cloudtype) {
            case S3:
            default:
                if (s3 == null) {
                    throw new IllegalArgumentException("S3 client is not configured!");
                }
                return s3;
            case SWS:
                if (swiftStack == null) {
                    throw new IllegalArgumentException("Swift Stack client is not configured!");
                }
                return swiftStack;
        }
    }

    /**
     * A method that returns true if a correct s3 URI was provided and false otherwise.
     *
     * @param uri The provided URI for the file.
     * @return a boolean value that shows whether the correct URI was provided
     */
    public boolean isFileExisting(String uri) {

        boolean exist = true;

        try {
            AmazonS3URI s3URI = new AmazonS3URI(replaceSchema(uri));
            getAws(getCloudType(uri)).getObjectMetadata(s3URI.getBucket(), s3URI.getKey());
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
    public long getFileSize(String amazonURI){
        return fileSizes.getUnchecked(amazonURI);
    }

    /**
     * A method that creates an InputStream on a specific range of the file.
     * InputStream classes wrapping order can be reversed.
     *
     * @param url    target file URI
     * @param offset range start position
     * @param end    range end position
     * @return an InputStream object on the specific range of the file.
     */
    public InputStream loadFromTo(String url, long offset, long end) {
        AmazonS3URI obj = new AmazonS3URI(replaceSchema(url));
        GetObjectRequest rangeObjectRequest = new GetObjectRequest(obj.getBucket(), obj.getKey());
        rangeObjectRequest.setRange(offset, end);
        S3Object s3Object = getAws(getCloudType(url)).getObject(rangeObjectRequest);
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
    public InputStream loadFrom(String obj,
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
    public InputStream loadFully(String obj) {
        return loadFrom(obj, 0);
    }

    public BiologicalDataItemDownloadUrl generatePresignedUrl(final String url) {
        final Date expires = new Date((new Date()).getTime() + URL_EXPIRATION);
        final GeneratePresignedUrlRequest request = generatePresignedUrlRequest(url, expires);
        final URL generatedUrl = getAws(getCloudType(url)).generatePresignedUrl(request);
        return BiologicalDataItemDownloadUrl.builder()
                .type(BiologicalDataItemResourceType.S3)
                .url(generatedUrl.toExternalForm())
                .expires(expires)
                .build();
    }

    private String replaceSchema(String url) {
        url = url.replace(CloudType.SWS.protocol, CloudType.S3.protocol);
        return url;
    }

    private CloudType getCloudType(String url) {
        return url.startsWith(CloudType.S3.protocol) ? CloudType.S3 : CloudType.SWS;
    }


    public enum CloudType {
        S3("s3://"),
        SWS("sws://");

        CloudType(String protocol) {
            this.protocol = protocol;
        }

        private String protocol;
    }

    private GeneratePresignedUrlRequest generatePresignedUrlRequest(final String url, final Date expires) {
        final AmazonS3URI s3URI = new AmazonS3URI(replaceSchema(url));
        final String filePath = s3URI.getKey();
        final GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(s3URI.getBucket(), filePath);
        request.setExpiration(expires);
        request.setResponseHeaders(new ResponseHeaderOverrides()
                .withContentDisposition(buildContentDispositionHeader(filePath)));
        return request;
    }
}
