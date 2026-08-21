package com.epam.catgenome.util.aws;


import com.epam.catgenome.entity.BiologicalDataItemDownloadUrl;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Uri;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.epam.catgenome.util.QueryUtils.buildContentDispositionHeader;

/**
 * Class provides configuration of AWS client and utility methods for S3
 *
 * <p>Moved from AWS SDK v1 to v2 in migration Phase 8. Three consequences are worth knowing about
 * before editing this class:
 *
 * <ul>
 *   <li>the SDK's own client type is {@code software.amazon.awssdk.services.s3.S3Client} - the same
 *       simple name as this class - so it is spelled out in full below instead of imported;</li>
 *   <li>presigning is no longer something the client does. v1's
 *       {@code AmazonS3.generatePresignedUrl} became a separate {@link S3Presigner}, which carries
 *       its own copy of the region, credentials and endpoint configuration, hence the parallel
 *       pair of fields per cloud type;</li>
 *   <li>URI parsing hangs off a client. v1's {@code AmazonS3URI} was a standalone parser; v2's
 *       equivalent is {@code client.utilities().parseUri}, which accepts the same
 *       "s3://bucket/key" form and, like {@code AmazonS3URI}, returns bucket and key
 *       percent-decoded.</li>
 * </ul>
 */
public final class S3Client {

    private static final int CACHE_SIZE = 1000;
    private static final Duration URL_EXPIRATION = Duration.ofDays(1);
    private static final String SWS_PROFILE = "sws";
    private static final String SCHEME_SEPARATOR = "://";
    private static final Logger LOGGER = LoggerFactory.getLogger(S3Client.class);


    private software.amazon.awssdk.services.s3.S3Client s3;
    private S3Presigner s3Presigner;
    private final software.amazon.awssdk.services.s3.S3Client swiftStack;
    private final S3Presigner swiftStackPresigner;

    private static S3Client instance;

    private final LoadingCache<String, Long> fileSizes = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(
                    new CacheLoader<String, Long>() {
                        public Long load(String url) {
                            return fetchFileSize(url);
                        }
                    });


    private S3Client(final String swsEndpoint, String swsRegion, boolean pathStyleAccess) {
        try {
            s3 = software.amazon.awssdk.services.s3.S3Client.create();
            s3Presigner = S3Presigner.create();
        } catch (SdkException e) {
            // Both resolve the region from the provider chain and throw SdkClientException when it
            // finds nothing, exactly as v1's AmazonS3ClientBuilder did; SdkException is its
            // supertype and is caught deliberately, because every NGB startup builds this bean
            // (Application.s3Client) and a machine with no AWS configuration at all has to come up
            // with S3 unavailable rather than fail.
            LOGGER.warn("Unable to create S3 client, S3 services will be unavailable.", e);
            s3 = null;
            s3Presigner = null;
        }
        if (!StringUtils.isEmpty(swsEndpoint) && !StringUtils.isEmpty(swsRegion)) {
            final URI endpoint = endpointUri(swsEndpoint);
            final Region region = Region.of(swsRegion);
            final ProfileCredentialsProvider credentials = ProfileCredentialsProvider.create(SWS_PROFILE);
            final S3Configuration serviceConfiguration = S3Configuration.builder()
                    .pathStyleAccessEnabled(pathStyleAccess)
                    .build();
            swiftStack = software.amazon.awssdk.services.s3.S3Client.builder()
                    .endpointOverride(endpoint)
                    .region(region)
                    .credentialsProvider(credentials)
                    .serviceConfiguration(serviceConfiguration)
                    .build();
            swiftStackPresigner = S3Presigner.builder()
                    .endpointOverride(endpoint)
                    .region(region)
                    .credentialsProvider(credentials)
                    .serviceConfiguration(serviceConfiguration)
                    .build();
        } else {
            swiftStack = null;
            swiftStackPresigner = null;
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

    private software.amazon.awssdk.services.s3.S3Client getAws(CloudType cloudtype) {
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

    private S3Presigner getPresigner(CloudType cloudtype) {
        switch (cloudtype) {
            case S3:
            default:
                if (s3Presigner == null) {
                    throw new IllegalArgumentException("S3 client is not configured!");
                }
                return s3Presigner;
            case SWS:
                if (swiftStackPresigner == null) {
                    throw new IllegalArgumentException("Swift Stack client is not configured!");
                }
                return swiftStackPresigner;
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
            fetchFileSize(uri);
        } catch (S3Exception e) {
            // NoSuchKeyException (404) is an S3Exception too, so both branches are covered by one
            // catch, as they were under v1's AmazonS3Exception.
            if (e.statusCode() == HttpStatusCode.FORBIDDEN
                    || e.statusCode() == HttpStatusCode.NOT_FOUND) {
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
        final CloudType cloudType = getCloudType(url);
        final S3Uri obj = parseUri(url, cloudType);
        final GetObjectRequest rangeObjectRequest = GetObjectRequest.builder()
                .bucket(bucketOf(obj))
                .key(keyOf(obj))
                // v1's GetObjectRequest.setRange(offset, end) wrote this header itself; v2 takes the
                // header value. Both ends are inclusive.
                .range("bytes=" + offset + "-" + end)
                .build();
        return new BufferedInputStream(getAws(cloudType).getObject(rangeObjectRequest));
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
        long contentLength = getInstance().getFileSize(obj);
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
        final CloudType cloudType = getCloudType(url);
        final S3Uri s3URI = parseUri(url, cloudType);
        final String filePath = keyOf(s3URI);
        final PresignedGetObjectRequest presigned = getPresigner(cloudType).presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(URL_EXPIRATION)
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(bucketOf(s3URI))
                                .key(filePath)
                                .responseContentDisposition(buildContentDispositionHeader(filePath))
                                .build())
                        .build());
        return BiologicalDataItemDownloadUrl.builder()
                .type(BiologicalDataItemResourceType.S3)
                .url(presigned.url().toExternalForm())
                .expires(Date.from(presigned.expiration()))
                .size(getFileSize(url))
                .build();
    }

    private long fetchFileSize(final String url) {
        final CloudType cloudType = getCloudType(url);
        final S3Uri obj = parseUri(url, cloudType);
        return getAws(cloudType).headObject(HeadObjectRequest.builder()
                .bucket(bucketOf(obj))
                .key(keyOf(obj))
                .build())
                .contentLength();
    }

    private S3Uri parseUri(final String url, final CloudType cloudType) {
        return getAws(cloudType).utilities().parseUri(URI.create(replaceSchema(url)));
    }

    private static String bucketOf(final S3Uri uri) {
        return uri.bucket().orElseThrow(() -> new IllegalArgumentException("No bucket in URI " + uri.uri()));
    }

    private static String keyOf(final S3Uri uri) {
        return uri.key().orElseThrow(() -> new IllegalArgumentException("No file key in URI " + uri.uri()));
    }

    /**
     * v1's {@code AwsClientBuilder.EndpointConfiguration} took a bare host and defaulted the
     * protocol to https; v2's {@code endpointOverride} takes a URI and rejects one without a
     * scheme. Existing {@code swift.stack.endpoint.url} settings keep working either way.
     */
    private static URI endpointUri(final String endpoint) {
        return URI.create(endpoint.contains(SCHEME_SEPARATOR) ? endpoint : "https" + SCHEME_SEPARATOR + endpoint);
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
}
