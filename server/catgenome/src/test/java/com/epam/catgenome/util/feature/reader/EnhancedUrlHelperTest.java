/*
 * MIT License
 *
 * Copyright (c) 2026 EPAM Systems
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

package com.epam.catgenome.util.feature.reader;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Which URLs get the pre-signed-URL tolerance, and which do not.
 *
 * <p>The question this answers is whether the tolerance follows the *signature* or the *host*. Until
 * migration Phase 9 it followed the host, so a pre-signed URL from any S3-compatible store that is
 * not AWS - MinIO, Ceph RGW, SwiftStack - was routed to stock htsjdk, which measures a resource with
 * a HEAD that the signature does not cover, and the file read back as empty.
 */
public class EnhancedUrlHelperTest {

    private static final String SIGV4_QUERY = "X-Amz-Algorithm=AWS4-HMAC-SHA256"
            + "&X-Amz-Date=20260821T101112Z"
            + "&X-Amz-SignedHeaders=host"
            + "&X-Amz-Expires=86400"
            + "&X-Amz-Credential=AKIAEXAMPLE%2F20260821%2Fus-east-1%2Fs3%2Faws4_request"
            + "&X-Amz-Signature=8a1b2c3d4e5f60718293a4b5c6d7e8f90112233445566778899aabbccddeeff00";

    @Test
    public void testToleratesPresignedUrlsFromAnyS3CompatibleStore() throws MalformedURLException {
        assertSigned("https://ngb-oss-builds.s3.amazonaws.com/data/test.vcf.gz?" + SIGV4_QUERY);
        assertSigned("https://ngb-cloud.s3.eu-central-1.amazonaws.com/data/test.vcf.gz?" + SIGV4_QUERY);
        // the three stores the host pattern used to exclude
        assertSigned("http://minio:9000/ngb-cloud/data/test.vcf.gz?" + SIGV4_QUERY);
        assertSigned("https://rgw.ceph.internal/ngb-cloud/data/test.vcf.gz?" + SIGV4_QUERY);
        assertSigned("https://swiftstack.example.org:8443/v1/ngb-cloud/data/test.vcf.gz?" + SIGV4_QUERY);
    }

    @Test
    public void testTolerancePersistsForUnsignedAwsUrls() throws MalformedURLException {
        // not a defect being fixed, just behaviour deliberately kept: see headMayBeRefused
        assertSigned("https://ngb-oss-builds.s3.amazonaws.com/public/data/tests/example.gff");
        assertSigned("https://ngb-oss-builds.s3-eu-west-1.amazonaws.com/public/data/tests/example.gff");
    }

    @Test
    public void testPlainHttpUrlsAreLeftToStockHtsjdk() throws MalformedURLException {
        assertNotSigned("https://ftp.ensembl.org/pub/release-111/gff3/homo_sapiens.gff3.gz");
        assertNotSigned("https://example.org/data/test.vcf.gz?token=abc&region=X%3A1-100");
        // a query parameter whose *value* mentions a signature is not a signature
        assertNotSigned("https://example.org/data/test.vcf.gz?redirect=X-Amz-Signature%3Dnope");
    }

    @Test
    public void testCloudSchemesAndLocalPathsAreNotUrlsWorthProbing() {
        // these are handled by S3SeekableStreamFactory / AzureSeekableStreamFactory before the
        // question is ever asked, and must not throw when it is
        assertFalse(EnhancedUrlHelper.headMayBeRefused("s3://ngb-cloud/data/test.vcf.gz"));
        assertFalse(EnhancedUrlHelper.headMayBeRefused("az://ngb-cloud/data/test.vcf.gz"));
        assertFalse(EnhancedUrlHelper.headMayBeRefused("/opt/ngb/contents/test.vcf.gz"));
        assertFalse(EnhancedUrlHelper.headMayBeRefused("test.vcf.gz"));
    }

    private void assertSigned(final String url) throws MalformedURLException {
        assertTrue(url, EnhancedUrlHelper.headMayBeRefused(url));
        assertTrue(url, EnhancedUrlHelper.headMayBeRefused(new URL(url)));
    }

    private void assertNotSigned(final String url) throws MalformedURLException {
        assertFalse(url, EnhancedUrlHelper.headMayBeRefused(url));
        assertFalse(url, EnhancedUrlHelper.headMayBeRefused(new URL(url)));
    }
}
