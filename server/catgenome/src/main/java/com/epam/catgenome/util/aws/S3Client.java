package com.epam.catgenome.util.aws;

import com.epam.catgenome.entity.BiologicalDataItemDownloadUrl;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Uri;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.epam.catgenome.util.QueryUtils.buildContentDispositionHeader;

public final class S3Client {

    private static final int CACHE_SIZE = 1000;
    private static final Long URL_EXPIRATION = 24 * 60 * 60 * 1000L;
    private static final Logger LOGGER = LoggerFactory.getLogger(S3Client.class);

    private software.amazon.awssdk.services.s3.S3Client s3Client;
    private final software.amazon.awssdk.services.s3.S3Client swiftStackClient;
    private final S3Presigner presigner;

    private static S3Client instance;

    private final LoadingCache<String, Long> fileSizes = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(
                    new CacheLoader<String, Long>() {
                        public Long load(String key) {
                            CloudType cloudType = getCloudType(key);
                            S3Uri s3Uri = parseS3Uri(key);
                            HeadObjectRequest request = HeadObjectRequest.builder()
                                    .bucket(s3Uri.bucket().orElseThrow(() -> new IllegalArgumentException("Invalid bucket")))
                                    .key(s3Uri.key().orElseThrow(() -> new IllegalArgumentException("Invalid key")))
                                    .build();
                            HeadObjectResponse response = getAws(cloudType).headObject(request);
                            return response.contentLength();
                        }
                    });

    private S3Client(final String swsEndpoint, String swsRegion, boolean pathStyleAccess) {
        try {
            s3Client = software.amazon.awssdk.services.s3.S3Client.builder()
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .region(Region.AWS_GLOBAL)
                    .build();
        } catch (SdkException e) {
            LOGGER.warn("Unable to create S3 client, S3 services will be unavailable.", e);
            s3Client = null;
        }

        if (!StringUtils.isEmpty(swsEndpoint) && !StringUtils.isEmpty(swsRegion)) {
            swiftStackClient = software.amazon.awssdk.services.s3.S3Client.builder()
                    .endpointOverride(URI.create(swsEndpoint))
                    .region(Region.of(swsRegion))
                    .credentialsProvider(ProfileCredentialsProvider.create("sws"))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(pathStyleAccess)
                            .build())
                    .build();
        } else {
            swiftStackClient = null;
        }

        presigner = S3Presigner.builder().region(Region.AWS_GLOBAL).build();
    }

    public static synchronized S3Client configure(String swsEndpoint, String swsRegion, boolean pathStyleAccess) {
        instance = new S3Client(swsEndpoint, swsRegion, pathStyleAccess);
        return instance;
    }

    public static synchronized S3Client getInstance() {
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
                if (s3Client == null) {
                    throw new IllegalArgumentException("S3 client is not configured!");
                }
                return s3Client;
            case SWS:
                if (swiftStackClient == null) {
                    throw new IllegalArgumentException("Swift Stack client is not configured!");
                }
                return swiftStackClient;
        }
    }

    public boolean isFileExisting(String uri) {
        boolean exist = true;
        try {
            S3Uri s3Uri = parseS3Uri(uri);
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(s3Uri.bucket().orElseThrow(() -> new IllegalArgumentException("Invalid bucket")))
                    .key(s3Uri.key().orElseThrow(() -> new IllegalArgumentException("Invalid key")))
                    .build();
            getAws(getCloudType(uri)).headObject(request);
        } catch (S3Exception e) {
            if (e.statusCode() == HttpStatus.SC_FORBIDDEN || e.statusCode() == HttpStatus.SC_NOT_FOUND) {
                exist = false;
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error checking file existence", e);
        }
        return exist;
    }

    public long getFileSize(String amazonURI) {
        return fileSizes.getUnchecked(amazonURI);
    }

    public InputStream loadFromTo(String url, long offset, long end) {
        S3Uri s3Uri = parseS3Uri(url);
        GetObjectRequest rangeObjectRequest = GetObjectRequest.builder()
                .bucket(s3Uri.bucket().orElseThrow(() -> new IllegalArgumentException("Invalid bucket")))
                .key(s3Uri.key().orElseThrow(() -> new IllegalArgumentException("Invalid key")))
                .range("bytes=" + offset + "-" + end)
                .build();
        return new BufferedInputStream(getAws(getCloudType(url)).getObject(rangeObjectRequest, ResponseTransformer.toInputStream()));
    }

    public InputStream loadFrom(String obj, long offset) {
        long contentLength = S3Client.getInstance().getFileSize(obj);
        return loadFromTo(obj, offset, contentLength);
    }

    public InputStream loadFully(String obj) {
        return loadFrom(obj, 0);
    }

    public BiologicalDataItemDownloadUrl generatePresignedUrl(final String url) {
        final Instant expiration = Instant.now().plusMillis(URL_EXPIRATION);
        S3Uri s3Uri = parseS3Uri(url);
        String filePath = s3Uri.key().orElseThrow(() -> new IllegalArgumentException("Invalid key"));

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Uri.bucket().orElseThrow(() -> new IllegalArgumentException("Invalid bucket")))
                .key(filePath)
                .responseContentDisposition(buildContentDispositionHeader(filePath))
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(r -> r
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofMillis(URL_EXPIRATION)));

        return BiologicalDataItemDownloadUrl.builder()
                .type(BiologicalDataItemResourceType.S3)
                .url(presignedRequest.url().toString())
                .expires(Date.from(expiration))
                .size(getFileSize(url))
                .build();
    }

    private String replaceSchema(String url) {
        return url.replace(CloudType.SWS.protocol, CloudType.S3.protocol);
    }

    private CloudType getCloudType(String url) {
        return url.startsWith(CloudType.S3.protocol) ? CloudType.S3 : CloudType.SWS;
    }

    private S3Uri parseS3Uri(String uri) {
        try {
            return S3Uri.builder().uri(URI.create(replaceSchema(uri))).build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid S3 URI: " + uri, e);
        }
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
