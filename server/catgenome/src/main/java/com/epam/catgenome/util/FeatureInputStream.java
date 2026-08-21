/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2021 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.catgenome.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads the bytes of a cloud object in {@link #CHUNK_SIZE}-sized ranged requests, so that a
 * sequential read of a large object costs one request per chunk rather than one per byte.
 *
 * <p>Subclasses supply {@link #getBytes(long, long)}; everything about where the next chunk starts
 * and when the object has been exhausted is decided here.
 */
public abstract class FeatureInputStream extends InputStream {

    protected static final int CHUNK_SIZE = 64 * 1024;
    private static final int INVERSE_MASK = 0xff;
    protected static final int EOF_BYTE = -1;
    protected final String uri;
    protected final long to;
    protected long position;
    protected byte[] currentDataChunck;
    protected int chunckIndex;

    public FeatureInputStream(String uri, long from, long to) {
        this.uri = uri;
        position = from;
        this.to = to;
        currentDataChunck = new byte[0];
    }

    @Override
    public int read() throws IOException {
        if (chunckEndReached()) {
            currentDataChunck = getNewBuffer();
            chunckIndex = 0;
            if (chunckEndReached()) {
                return EOF_BYTE;
            }
        }
        return getNextByte();
    }

    /**
     * Reads the object's bytes from {@code from} to {@code to}, <em>both ends inclusive</em> - the
     * range convention of an HTTP {@code Range} header, which is what both implementations turn it
     * into.
     */
    protected abstract byte[] getBytes(long from, long to);

    /**
     * The next chunk, or an empty array once {@link #to} has been passed - which is how
     * {@link #read()} recognises the end of the object.
     *
     * <p>The empty array matters: an earlier version returned {@code new byte[]{-1}} here and let
     * {@link #read()} hand that byte back through {@link #getNextByte()}, where {@code & 0xff}
     * turned it into 255. Nothing then ever saw an end of stream, so a read past the last byte
     * produced 255s indefinitely. htsjdk's {@code BlockCompressedInputStream} read four of them as
     * the trailing bgzip block's ISIZE field and refused every bgzip'd file in cloud storage with
     * "invalid uncompressedLength: -1".
     */
    private byte[] getNewBuffer() {
        final long destination = Math.min(to, position + CHUNK_SIZE);
        if (position > destination) {
            return new byte[0];
        }
        final byte[] bytes = getBytes(position, destination);
        position = destination + 1;
        return bytes;
    }

    protected int getNextByte() {
        return currentDataChunck[chunckIndex++] & INVERSE_MASK;
    }

    protected boolean chunckEndReached() {
        return currentDataChunck.length == chunckIndex;
    }

    @Override
    public void close() throws IOException {
        currentDataChunck = null;
    }
}