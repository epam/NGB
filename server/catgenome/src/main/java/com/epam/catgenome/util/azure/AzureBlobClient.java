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

import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.epam.catgenome.entity.BiologicalDataItemDownloadUrl;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;

abstract class AbstractCredentialConfigurer implements AzureCredentialConfiguration {
    abstract BlobServiceClientBuilder configureCredential(BlobServiceClientBuilder builder);
}

@Getter
@Builder
class AccessKeyCredentialConfigurer extends AbstractCredentialConfigurer implements AzureCredentialConfiguration {

    private final String storageAccount;
    private final String storageKey;

    @Override
    BlobServiceClientBuilder configureCredential(BlobServiceClientBuilder builder) {
        return builder.credential(new StorageSharedKeyCredential(storageAccount, storageKey));
    }
}

@Getter
@Builder
class ServicePrincipalCredentialConfigurer extends AbstractCredentialConfigurer implements AzureCredentialConfiguration {

    private final String storageAccount;
    private final String clientId;
    private final String clientSecret;
    private final String tenantId;

    @Override
    BlobServiceClientBuilder configureCredential(BlobServiceClientBuilder builder) {
        return builder.credential( new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build());
    }
}

@Getter
@Builder
class DefaultCredentialConfigurer extends AbstractCredentialConfigurer implements AzureCredentialConfiguration {

    private final String storageAccount;
    private final String managedIdentityId;
    private final String tenantId;

    @Override
    BlobServiceClientBuilder configureCredential(BlobServiceClientBuilder builder) {

        DefaultAzureCredentialBuilder cb = new DefaultAzureCredentialBuilder();

        if(StringUtils.isNotEmpty(managedIdentityId)) {
            cb = cb.managedIdentityClientId(managedIdentityId);
        }

        if(StringUtils.isNotEmpty(tenantId)) {
            cb = cb.tenantId(tenantId);
        }

        return builder.credential(cb.build());
    }
}

public class AzureBlobClient {

    private static final String BLOB_URL_FORMAT = "https://%s.blob.core.windows.net";
    private static final String AZ_SCHEME = "az://";
    private static final String AZ_BLOB_DELIMITER = "/";
    private static final Long URL_EXPIRATION = 24 * 60 * 60 * 1000L;

    private AzureCredentialConfiguration credentialConfig;
    private BlobServiceClient blobService;

    private static AzureBlobClient instance;

    public AzureBlobClient() {
        //just to make test context work
    }

    public AzureBlobClient(@NonNull AbstractCredentialConfigurer credentialConfigurer) {
        this.credentialConfig = credentialConfigurer;
        this.blobService = credentialConfigurer.configureCredential(new BlobServiceClientBuilder()
                        .endpoint(String.format(BLOB_URL_FORMAT, credentialConfigurer.getStorageAccount())))
                .buildClient();
    }

    @PostConstruct
    public void init() {
        instance = this;
    }

    public InputStream loadFromTo(final String uri, final long offset, final long end) {
        final BlobClient client = getBlobURL(uri);
        final BlobRange blobRange = new BlobRange(offset, end - offset + 1);
        return client.openInputStream(blobRange, null);
    }

    public boolean blobExists(String uri) {
        return getBlobURL(uri).exists();
    }

    public long getFileSize(String uri){
        return getBlobURL(uri).getProperties().getBlobSize();
    }

    private BlobClient getBlobURL(final String uri) {
        final AzureBlobItem azureBlob = validateUri(uri);
        BlobContainerClient blobContainerClient = blobService.getBlobContainerClient(azureBlob.container);
        return blobContainerClient.getBlobClient(azureBlob.blobPath);
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
                .size(getFileSize(path))
                .build();
    }

    @Data
    private static final class AzureBlobItem {
        private final String container;
        private final String blobPath;
    }

    private String buildBlobDownloadUrl(final String blobPath) {
        final AzureBlobItem azureBlobItem = parseUri(blobPath);
        final BlobSasPermission permissions = new BlobSasPermission()
                .setReadPermission(true)
                .setAddPermission(false)
                .setWritePermission(false);
        final BlobServiceSasSignatureValues blobServiceSasSignatureValues =
                new BlobServiceSasSignatureValues(OffsetDateTime.now().plus(Duration.ofDays(1)), permissions);
        final String sasToken = getBlobURL(blobPath).generateSas(blobServiceSasSignatureValues);
        return String.format(BLOB_URL_FORMAT + "/%s/%s?%s", credentialConfig.getStorageAccount(),
                azureBlobItem.getContainer(), azureBlobItem.getBlobPath(), sasToken);
    }
}
