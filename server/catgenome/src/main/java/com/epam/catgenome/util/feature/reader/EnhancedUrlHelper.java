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
import java.net.URL;
import java.util.regex.Pattern;

import htsjdk.tribble.util.FTPHelper;
import htsjdk.tribble.util.HTTPHelper;
import htsjdk.tribble.util.URLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * URL Helper class that supports S3 singed url handling
 */
public class EnhancedUrlHelper implements URLHelper {

    private static final Pattern S3_PATTERN = Pattern.compile(".*s3.*\\.amazonaws\\.com");

    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedUrlHelper.class);
    private URLHelper wrappedHelper;

    public EnhancedUrlHelper(URL url) {
        String protocol = url.getProtocol().toLowerCase();
        if (S3_PATTERN.matcher(url.getHost()).matches()) {
            LOGGER.debug("Creating S3Helper here. Url:" + url);
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

    @Override
    @Deprecated public InputStream openInputStreamForRange(long start, long end)
            throws IOException {
        return this.wrappedHelper.openInputStreamForRange(start, end);
    }

    @Override public boolean exists() throws IOException {
        return this.wrappedHelper.exists();
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
