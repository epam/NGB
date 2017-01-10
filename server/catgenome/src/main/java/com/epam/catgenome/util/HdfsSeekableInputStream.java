/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.fs.FSDataInputStream;

import htsjdk.samtools.seekablestream.SeekableStream;

/**
 * Source:      HdfsSeekableInputStream.java
 * Created:     4/29/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 * <p>
 * A {@link SeekableStream} extension, designed to read data from HDFS
 * </p>
 */
public class HdfsSeekableInputStream extends SeekableStream {

    private FSDataInputStream stream;

    /**
     * @param stream a HDFS {@link InputStream}
     * @throws IOException
     */
    public HdfsSeekableInputStream(InputStream stream) throws IOException {
        this.stream = new FSDataInputStream(stream);
    }

    @Override
    public long length() {
        return 0;
    }

    @Override
    public long position() throws IOException {
        return stream.getPos();

    }

    @Override
    public void seek(long position) throws IOException {
        stream.seek(position);
    }

    @Override
    public int read() throws IOException {
        return stream.read();
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return stream.read(buffer, offset, length);
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    @Override
    public boolean eof() throws IOException {
        return false;
    }

    @Override
    public String getSource() {
        return null;
    }
}
