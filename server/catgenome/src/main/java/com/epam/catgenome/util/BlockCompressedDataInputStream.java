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

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.util.Assert;

import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.BlockCompressedInputStream;

/**
 * Source:      BlockCompressedDataInputStream.java
 * Created:     4/1/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A custom implementation of block-compressed DataInputStream, allowing same functionality as HTSJDK's
 * {@link BlockCompressedInputStream} plus adding {@link DataInputStream} functionality
 * </p>
 */
public class BlockCompressedDataInputStream extends BlockCompressedInputStream {

    private static final int SHIFT24 = 24;
    private static final int SHIFT56 = 56;
    private static final int SHIFT48 = 48;
    private static final int SHIFT40 = 40;
    private static final int SHIFT32 = 32;
    private static final int BYTE_MASK = 255;

    private static final int BYTE_SIZE = 8;
    private static final int TWO_BYTE_SIZE = 16;
    private static final int LONG_SIZE = 8;
    private static final int OFFSET_0 = 0;
    private static final int OFFSET_1 = 1;
    private static final int OFFSET_2 = 2;
    private static final int OFFSET_3 = 3;
    private static final int OFFSET_4 = 4;
    private static final int OFFSET_5 = 5;
    private static final int OFFSET_6 = 6;
    private static final int OFFSET_7 = 7;
    /**
     * Create from {@link InputStream}
     * @param stream an {@link InputStream} to build from
     */
    public BlockCompressedDataInputStream(InputStream stream) {
        super(stream);
    }

    /**
     * Create from {@link InputStream}
     * @param stream an {@link InputStream} to build from
     * @param allowBuffering flag, determining if buffering is allowed
     */
    public BlockCompressedDataInputStream(InputStream stream, boolean allowBuffering) {
        super(stream, allowBuffering);
    }

    /**
     * Create from {@link File} object
     * @param file {@link File} object to create from
     * @throws IOException
     */
    public BlockCompressedDataInputStream(File file) throws IOException {
        super(file);
    }

    /**
     * Create from {@link URL}
     * @param url an {@link URL} to build from
     */
    public BlockCompressedDataInputStream(URL url) {
        super(url);
    }

    /**
     * Create from {@link SeekableStream}
     * @param strm an {@link SeekableStream} to build from
     */
    public BlockCompressedDataInputStream(SeekableStream strm) {
        super(strm);
    }

    /**
     * Reads an unsigned byte value from stream
     * @return an unsigned byte value
     * @throws IOException if no value was read
     */
    public int readUnsignedByte() throws IOException {
        int ch = read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    /**
     * Reads a short value from stream
     * @return a short value
     * @throws IOException if no value was read
     */
    public short readShort() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (short) ((ch1 << BYTE_SIZE) + (ch2));
    }

    /**
     * Reads an unsigned byte value from stream
     * @return an unsigned byte value
     * @throws IOException if no value was read
     */
    public int readUnsignedShort() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (ch1 << BYTE_SIZE) + (ch2);
    }

    /**
     * Reads a char value from stream
     * @return a char value
     * @throws IOException if no value was read
     */
    public char readChar() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (char) ((ch1 << BYTE_SIZE) + (ch2));
    }

    /**
     * Reads an int value from stream
     * @return an int value
     * @throws IOException if no value was read
     */
    public final int readInt() throws IOException {
        int ch1 = read();
        int ch2 = read();
        int ch3 = read();
        int ch4 = read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return (ch1 << SHIFT24) + (ch2 << TWO_BYTE_SIZE) + (ch3 << BYTE_SIZE) + ch4;
    }

    /**
     * Reads a long value from stream
     * @return an unsigned byte value
     * @throws IOException if no value was read
     */
    public final long readLong() throws IOException {
        final byte[] readBuffer = new byte[LONG_SIZE];
        final int countByte = read(readBuffer);
        Assert.isTrue(countByte == readBuffer.length);
        return (((long) readBuffer[OFFSET_0] << SHIFT56) +
                ((long) (readBuffer[OFFSET_1] & BYTE_MASK) << SHIFT48) +
                ((long) (readBuffer[OFFSET_2] & BYTE_MASK) << SHIFT40) +
                ((long) (readBuffer[OFFSET_3] & BYTE_MASK) << SHIFT32) +
                ((long) (readBuffer[OFFSET_4] & BYTE_MASK) << SHIFT24) +
                ((readBuffer[OFFSET_5] & BYTE_MASK) << TWO_BYTE_SIZE) +
                ((readBuffer[OFFSET_6] & BYTE_MASK) << BYTE_SIZE) +
                readBuffer[OFFSET_7]) & BYTE_MASK;
    }

    /**
     * Reads a float value from stream
     * @return an float value
     * @throws IOException if no value was read
     */
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * Reads a double value from stream
     * @return an double value
     * @throws IOException if no value was read
     */
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }
}
