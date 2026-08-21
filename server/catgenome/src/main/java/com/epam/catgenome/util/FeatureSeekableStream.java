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
import org.apache.commons.io.input.CountingInputStream;

import java.io.IOException;
import java.io.InputStream;

public abstract class FeatureSeekableStream extends SeekableStream {

    protected final String cloudUri;
    protected long contentLength;
    protected CountingInputStream currentDataStream;
    protected long offset;

    public FeatureSeekableStream(String cloudUri) {
        this.cloudUri = cloudUri;
    }

    @Override
    public long length() {
        return contentLength;
    }

    @Override
    public long position() throws IOException {
        return offset + currentDataStream.getByteCount();
    }

    @Override
    public int read() throws IOException {
        return currentDataStream.read();
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return currentDataStream.read(buffer, offset, length);
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
     * We should count data skipped because we want to count all data loaded.
     *
     * <p>Until Java 21 migration Phase 7 the counting came from
     * {@code org.apache.commons.compress.utils.CountingInputStream}, which counted reads but not
     * skips, so this subclass added the skip accounting. That class was removed in commons-compress
     * 1.26 (the version htsjdk 5 brings), and its commons-io replacement counts skipped bytes
     * itself - so nothing is left to override. The subclass survives only because
     * {@code util/aws/S3SeekableStream} and {@code util/azure/AzureBlobSeekableStream} construct it
     * by name; it can collapse into its parent when those are next touched.
     */
    public static class CountingWithSkipInputStream extends CountingInputStream {
        public CountingWithSkipInputStream(InputStream in) {
            super(in);
        }
    }
}