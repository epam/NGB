/*
 * MIT License
 *
 * Copyright (c) 2019 EPAM Systems
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

package com.epam.catgenome.util.azure;

import com.epam.catgenome.entity.BiologicalDataItemDownloadUrl;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.microsoft.azure.storage.blob.BlobRange;
import com.microsoft.azure.storage.blob.BlobSASPermission;
import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.PipelineOptions;
import com.microsoft.azure.storage.blob.SASProtocol;
import com.microsoft.azure.storage.blob.SASQueryParameters;
import com.microsoft.azure.storage.blob.ServiceSASSignatureValues;
import com.microsoft.azure.storage.blob.ServiceURL;
import com.microsoft.azure.storage.blob.SharedKeyCredentials;
import com.microsoft.azure.storage.blob.StorageException;
import com.microsoft.azure.storage.blob.StorageURL;
import com.microsoft.azure.storage.blob.models.BlobGetPropertiesHeaders;
import com.microsoft.rest.v2.util.FlowableUtil;
import io.reactivex.Flowable;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;

import static com.epam.catgenome.util.QueryUtils.buildContentDispositionHeader;

public class AzureBlobClient {

    private static final String BLOB_URL_FORMAT = "https://%s.blob.core.windows.net";
    private static final String BLOB_CONTENT_TYPE = "blob";
    private static final String AZ_SCHEME = "az://";
    private static final String AZ_BLOB_DELIMITER = "/";
    private static final int NOT_FOUND_ERROR_CODE = 404;
    private static final Long URL_EXPIRATION = 24 * 60 * 60 * 1000L;

    private ServiceURL blobService;
    private SharedKeyCredentials credentials;

    private static AzureBlobClient instance;

    public AzureBlobClient() {
        //just to make test context work
    }

    public AzureBlobClient(final String storageAccount,
                           final String storageKey) {
        try {
            final SharedKeyCredentials credentials = new SharedKeyCredentials(storageAccount, storageKey);
            this.blobService = new ServiceURL(
                    url(String.format(BLOB_URL_FORMAT, storageAccount)),
                    StorageURL.createPipeline(credentials, new PipelineOptions()));
            this.credentials = credentials;
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid credentials for Azure storage account: " + storageAccount);
        }
    }

    @PostConstruct
    public void init() {
        this.instance = this;
    }

    public InputStream loadFromTo(final String uri, final long offset, final long end) {
        final BlockBlobURL blockBlobURL = getBlobURL(uri);
        final BlobRange range = new BlobRange().withOffset(offset).withCount(end - offset + 1);
        final Flowable<ByteBuffer> body = blockBlobURL.download(range, null, false, null)
                .blockingGet()
                .body(null);
        return FlowableUtil.collectBytesInArray(body)
                .map(ByteArrayInputStream::new)
                .blockingGet();
    }

    public boolean blobExists(String uri) {
        try {
            return getFileSize(uri) != 0;
        } catch (StorageException e) {
            if (e.statusCode() == NOT_FOUND_ERROR_CODE) {
                return false;
            }
            throw e;
        }
    }

    public long getFileSize(String uri){
        final BlockBlobURL blockBlobURL = getBlobURL(uri);
        final BlobGetPropertiesHeaders headers = blockBlobURL.getProperties().blockingGet().headers();
        return headers.contentLength();
    }

    private BlockBlobURL getBlobURL(final String uri) {
        final AzureBlobItem azureBlob = validateUri(uri);
        return blobService.createContainerURL(azureBlob.container)
                .createBlockBlobURL(azureBlob.blobPath);
    }

    private AzureBlobItem validateUri(final String uri) {
        if (StringUtils.isBlank(uri)) {
            throw new IllegalArgumentException("Azure blob path is not specified.");
        }
        if (!uri.startsWith(AZ_SCHEME)) {
            throw new IllegalArgumentException(
                    String.format("Azure blob path must start with %s scheme. Actual value: %s.", AZ_SCHEME, uri));
        }
        return parseUri(uri);
    }

    @SneakyThrows
    private AzureBlobItem parseUri(final String azureBlobUri) {
        final URI uri = new URI(azureBlobUri);
        final String container = uri.getHost();
        final String blobPath = uri.getPath().substring(1);
        if (StringUtils.isBlank(blobPath) || blobPath.endsWith(AZ_BLOB_DELIMITER)) {
            throw new IllegalArgumentException("Azure blob math must point to a file. Actual path: " + blobPath);
        }
        return new AzureBlobItem(container, blobPath);
    }

    @SneakyThrows
    private URL url(final String blobUrl) {
        return new URL(blobUrl);
    }

    public InputStream loadFrom(String obj, long offset) {
        long contentLength = getFileSize(obj);
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


    public static boolean isAzSource(String inputUrl) {
        return inputUrl.startsWith(AZ_SCHEME);
    }

    public static AzureBlobClient getClient() {
        return instance;
    }

    public BiologicalDataItemDownloadUrl generatePresignedUrl(final String path) {
        final Date expires = new Date((new Date()).getTime() + URL_EXPIRATION);
        final String downloadUrl = buildBlobDownloadUrl(path);
        return BiologicalDataItemDownloadUrl.builder()
                .url(downloadUrl)
                .expires(expires)
                .type(BiologicalDataItemResourceType.AZ)
                .build();
    }

    @Data
    private static final class AzureBlobItem {
        private final String container;
        private final String blobPath;
    }

    private String buildBlobDownloadUrl(final String blobPath) {
        final AzureBlobItem azureBlobItem = parseUri(blobPath);
        final String sasToken = generateSASToken(azureBlobItem.getContainer(), azureBlobItem.getBlobPath());
        return String.format(BLOB_URL_FORMAT + "/%s/%s%s", credentials.getAccountName(),
                azureBlobItem.getContainer(), azureBlobItem.getBlobPath(), sasToken);
    }

    private String generateSASToken(final String containerName,
                                    final String blobName) {
        final ServiceSASSignatureValues values = new ServiceSASSignatureValues()
                .withProtocol(SASProtocol.HTTPS_ONLY)
                .withExpiryTime(OffsetDateTime.now().plus(Duration.ofDays(1)))
                .withContainerName(containerName)
                .withBlobName(blobName)
                .withContentType(BLOB_CONTENT_TYPE)
                .withContentDisposition(buildContentDispositionHeader(blobName))
                .withPermissions(buildPermissions().toString());
        final SASQueryParameters params = values.generateSASQueryParameters(credentials);
        return params.encode();
    }

    private BlobSASPermission buildPermissions() {
        return new BlobSASPermission()
                .withRead(true)
                .withAdd(false)
                .withWrite(false);
    }
}
