/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

import htsjdk.samtools.seekablestream.SeekableStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link SeekableStream} whose reads are served from a plain {@link InputStream} that subclasses
 * replace whenever someone seeks - so that a sequential read costs one request rather than one per
 * seek. {@link #position()} is that inner stream's start offset plus however much of it has been
 * consumed.
 *
 * <p>The consumed count used to come from a decorator: {@code CountingInputStream}, from
 * commons-compress until migration Phase 7, then from commons-io. Phase 8 counts here instead, in
 * the two {@code read} methods. That removes a wrapper from every cloud read, and it removes the
 * only reason commons-io had to stay pinned at 2.15.1 - 2.16.0 deprecates its
 * {@code CountingInputStream} in favour of {@code BoundedInputStream}, which does not count skipped
 * bytes. Nothing is lost by counting here: skipping is not something this class delegates, so a
 * {@code skip} on it goes through {@link InputStream#skip(long)}, which reads into a throwaway
 * buffer through {@link #read(byte[], int, int)} and is therefore still counted.
 */
public abstract class FeatureSeekableStream extends SeekableStream {

    protected final String cloudUri;
    protected long contentLength;
    protected long offset;

    private InputStream currentDataStream;
    private long streamBytesRead;

    public FeatureSeekableStream(String cloudUri) {
        this.cloudUri = cloudUri;
    }

    @Override
    public long length() {
        return contentLength;
    }

    @Override
    public long position() throws IOException {
        return offset + streamBytesRead;
    }

    @Override
    public int read() throws IOException {
        final int data = currentDataStream.read();
        if (data != -1) {
            streamBytesRead++;
        }
        return data;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        final int read = currentDataStream.read(buffer, offset, length);
        if (read != -1) {
            streamBytesRead += read;
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        currentDataStream.close();
    }

    @Override
    public boolean eof() throws IOException {
        return position() == length();
    }

    @Override
    public String getSource() {
        return cloudUri;
    }

    /**
     * Closes the stream reads are currently served from, if there is one. Subclasses call this
     * before opening the replacement, so that only one request is ever open at a time.
     */
    protected void closeDataStream() throws IOException {
        if (currentDataStream != null) {
            currentDataStream.close();
        }
    }

    /**
     * Serves subsequent reads from {@code data} and restarts the count that {@link #position()}
     * adds to {@link #offset}, which the caller is expected to have set already.
     */
    protected void serveFrom(final InputStream data) {
        currentDataStream = data;
        streamBytesRead = 0;
    }
}