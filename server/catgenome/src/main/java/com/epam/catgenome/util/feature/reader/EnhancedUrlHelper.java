/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import com.epam.catgenome.util.IOHelper;
import htsjdk.tribble.util.FTPHelper;
import htsjdk.tribble.util.HTTPHelper;
import htsjdk.tribble.util.URLHelper;

/**
 * URL Helper class that supports S3 singed url handling
 */
public class EnhancedUrlHelper implements URLHelper {

    private static final Pattern S3_PATTERN = Pattern.compile(".*s3.*\\.amazonaws\\.com");

    private URLHelper wrappedHelper;

    public EnhancedUrlHelper(URL url) {
        String protocol = url.getProtocol().toLowerCase();
        if (isSignedS3Url(url)) {
            this.wrappedHelper = new S3Helper(url);
        } else if (protocol.startsWith("http")) {
            this.wrappedHelper = new HTTPHelper(url);
        } else if (protocol.startsWith("ftp")) {
            this.wrappedHelper = new FTPHelper(url);
        } else {
            throw new IllegalArgumentException(
                    "Unable to create helper for url with protocol " + protocol);
        }
    }

    @Override public URL getUrl() {
        return this.wrappedHelper.getUrl();
    }

    @Override public long getContentLength() throws IOException {
        return this.wrappedHelper.getContentLength();
    }

    @Override public InputStream openInputStream() throws IOException {
        return this.wrappedHelper.openInputStream();
    }

    // Deprecated in htsjdk 2.x, un-deprecated in 5.0.0 - it is how a URLHelper reads a byte range,
    // and UrlSeekableStream reads through it.
    @Override public InputStream openInputStreamForRange(long start, long end)
            throws IOException {
        return this.wrappedHelper.openInputStreamForRange(start, end);
    }

    @Override public boolean exists() throws IOException {
        return this.wrappedHelper.exists();
    }

    /**
     * True for the URLs whose HEAD request may be refused with a 403 - that is, the ones this
     * class hands to {@link S3Helper}.
     *
     * <p>Exposed because the same URLs need the same tolerance outside the {@code URLHelper} SPI:
     * htsjdk's {@code SeekableHTTPStream} measures a resource with its own HEAD request and does
     * not consult a {@code URLHelper} at all, so {@code NgbSeekableStreamFactory} has to recognise
     * them and route them elsewhere.
     */
    public static boolean isSignedS3Url(final URL url) {
        return S3_PATTERN.matcher(url.getHost()).matches();
    }

    /**
     * As {@link #isSignedS3Url(URL)}, for a path that is not necessarily a URL at all: anything
     * that does not parse as one - a local file path, an {@code s3://} or {@code az://} URI - is
     * not a signed S3 URL either.
     */
    public static boolean isSignedS3Url(final String path) {
        try {
            return isSignedS3Url(new URL(path));
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * Inner helper class for handling S3 signed URLs. We sign URLs for GET requests so
     * HEAD request will return 403, this is considered to be OK in this case
     */
    public static class S3Helper extends HTTPHelper {

        public S3Helper(URL url) {
            super(url);
        }

        @Override public boolean exists() throws IOException {
            return urlExists();
        }

        /**
         * The inherited implementation measures the resource with a HEAD request and reports -1
         * when it is answered with anything but 200 - so for a URL signed for GET only it reports
         * no length, and a reader that trusts it sees an empty file. A GET probe is what htsjdk
         * itself used before 3.0.
         */
        @Override public long getContentLength() throws IOException {
            return IOHelper.getContentLength(getUrl());
        }

        private boolean urlExists() {
            HttpURLConnection con = null;
            try {
                URL url = getUrl();
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("HEAD");
                //we allow 403 code since AWS URL will return FORBIDDEN for signed urls HEAD request
                return (con.getResponseCode() == HttpURLConnection.HTTP_OK
                        || con.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN);
            } catch (IOException e) {
                // This is what we are testing for, so its not really an exception
                return false;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
    }
}
