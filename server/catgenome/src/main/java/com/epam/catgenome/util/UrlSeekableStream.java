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

package com.epam.catgenome.util;

import htsjdk.tribble.util.URLHelper;

import java.io.IOException;
import java.io.InputStream;

/**
 * A seekable stream over an http(s) resource, reading it through a tribble {@link URLHelper} - so
 * whatever tolerances that helper has apply to the data as well as to the existence check.
 *
 * <p>It exists because htsjdk's own {@code SeekableHTTPStream} has none. Its constructor measures
 * the resource with {@code HttpUtils.getHeaderField}, which sends a HEAD request since htsjdk 3.0,
 * and on failure records a length of zero without complaining; {@code read} then returns end of
 * stream before it ever issues a request. A pre-signed S3 URL is signed for GET only and answers
 * HEAD with 403, so through that stream it is indistinguishable from an empty file - the reader
 * gets no bytes and reports the file as malformed. htsjdk offers no way to change the request
 * method, and the field is private, so the whole stream has to be replaced for those URLs;
 * {@link com.epam.catgenome.util.feature.reader.EnhancedUrlHelper#headMayBeRefused(String)} picks
 * them out and {@link NgbSeekableStreamFactory} routes them here. Every other http URL stays on
 * stock htsjdk.
 *
 * <p>The reading strategy is {@code S3SeekableStream}'s: one ranged request open from the current
 * offset to the end of the resource, replaced whenever someone seeks, so that sequential reads
 * cost one request in total rather than one each.
 */
public class UrlSeekableStream extends FeatureSeekableStream {

    private final URLHelper helper;

    public UrlSeekableStream(final URLHelper helper) throws IOException {
        super(helper.getUrl().toExternalForm());
        this.helper = helper;
        this.contentLength = helper.getContentLength();
        if (contentLength < 0) {
            throw new IOException("Unable to determine the length of " + cloudUri
                    + ", it cannot be read as a track");
        }
        recreateInnerStream();
    }

    @Override
    public void seek(final long targetPosition) throws IOException {
        this.offset = targetPosition;
        recreateInnerStream();
    }

    private void recreateInnerStream() throws IOException {
        closeDataStream();
        // A request for the range past the last byte is answered with 416 rather than with nothing,
        // and htsjdk does seek to the end of a file - of a BAM index, for one - so that range is
        // never asked for.
        final InputStream data = offset >= contentLength
                ? nullInputStream()
                : helper.openInputStreamForRange(offset, contentLength - 1);
        serveFrom(data);
    }
}
