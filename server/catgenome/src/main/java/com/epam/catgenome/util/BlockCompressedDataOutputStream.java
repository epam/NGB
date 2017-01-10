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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import htsjdk.samtools.util.BlockCompressedOutputStream;

/**
 * Source:      BlockCompressedDataOutputStream.java
 * Created:     4/4/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A custom implementation of block-compressed data output stream, allowing same functionality as HTSJDK's
 * {@link BlockCompressedOutputStream} plus adding {@link java.io.DataOutputStream} functionality
 * </p>
 */
public class BlockCompressedDataOutputStream extends BlockCompressedOutputStream {

    private static final int SHIFT24 = 24;
    private static final int SHIFT56 = 56;
    private static final int SHIFT48 = 48;
    private static final int SHIFT40 = 40;
    private static final int SHIFT32 = 32;
    private static final int BYTE_MASK = 0xFF;

    private static final int BYTE_SIZE = 8;
    private static final int TWO_BYTE_SIZE = 16;

    private static final int OFFSET_0 = 0;
    private static final int OFFSET_1 = 1;
    private static final int OFFSET_2 = 2;
    private static final int OFFSET_3 = 3;
    private static final int OFFSET_4 = 4;
    private static final int OFFSET_5 = 5;
    private static final int OFFSET_6 = 6;
    private static final int OFFSET_7 = 7;

    private static final int BUFFER_SIZE = 8;
    private static final int LONG_SIZE = 8;

    private byte[] writeBuffer = new byte[BUFFER_SIZE];

    public BlockCompressedDataOutputStream(String filename) {
        super(filename);
    }

    public BlockCompressedDataOutputStream(File file) {
        super(file);
    }

    public BlockCompressedDataOutputStream(String filename, int compressionLevel) {
        super(filename, compressionLevel);
    }

    public BlockCompressedDataOutputStream(File file, int compressionLevel) {
        super(file, compressionLevel);
    }

    public BlockCompressedDataOutputStream(OutputStream os, File file) {
        super(os, file);
    }

    public BlockCompressedDataOutputStream(OutputStream os, File file, int compressionLevel) {
        super(os, file, compressionLevel);
    }

    /**
     * Writes a boolean value to stream
     * @param v a value to write
     * @throws IOException if writing failed
     */
    public final void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    /**
     * Writes a byte value to stream
     * @param v a value to write
     * @throws IOException if writing failed
     */
    public final void writeByte(int v) throws IOException {
        write(v);
    }

    /**
     * Writes a short value to stream
     * @param v a value to write
     * @throws IOException if writing failed
     */
    public final void writeShort(int v) throws IOException {
        write((v >>> BYTE_SIZE) & BYTE_MASK);
        write(v & BYTE_MASK);
    }

    /**
     * Writes a char value to stream
     * @param v a value to write
     * @throws IOException if writing failed
     */
    public final void writeChar(int v) throws IOException {
        write((v >>> BUFFER_SIZE) & BYTE_MASK);
        write(v & BYTE_MASK);

    }

    /**
     * Writes an int value to stream
     * @param v a value to write
     * @throws IOException if writing failed
     */
    public final void writeInt(int v) throws IOException {
        write((v >>> SHIFT24) & BYTE_MASK);
        write((v >>> TWO_BYTE_SIZE) & BYTE_MASK);
        write((v >>> BYTE_SIZE) & BYTE_MASK);
        write(v & BYTE_MASK);

    }

    /**
     * Writes a long value to stream
     * @param v a value to write
     * @throws IOException if writing failed
     */
    public final void writeLong(long v) throws IOException {
        writeBuffer[OFFSET_0] = (byte) (v >>> SHIFT56);
        writeBuffer[OFFSET_1] = (byte) (v >>> SHIFT48);
        writeBuffer[OFFSET_2] = (byte) (v >>> SHIFT40);
        writeBuffer[OFFSET_3] = (byte) (v >>> SHIFT32);
        writeBuffer[OFFSET_4] = (byte) (v >>> SHIFT24);
        writeBuffer[OFFSET_5] = (byte) (v >>> TWO_BYTE_SIZE);
        writeBuffer[OFFSET_6] = (byte) (v >>> BYTE_SIZE);
        writeBuffer[OFFSET_7] = (byte) v;
        write(writeBuffer, 0, LONG_SIZE);
    }

    /**
     * Writes a float value to stream
     * @param v a value to write
     * @throws IOException if writing failed
     */
    public final void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    /**
     * Writes a double value to stream
     * @param v a value to write
     * @throws IOException if writing failed
     */
    public final void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    /**
     * Writes a String value to stream
     * @param s a string to write
     * @throws IOException if writing failed
     */
    public final void writeChars(String s) throws IOException {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            int v = s.charAt(i);
            write((v >>> BYTE_SIZE) & BYTE_MASK);
            write(v & BYTE_MASK);
        }
    }

    /**
     * Writes bytes value to stream
     * @param s a value to write
     * @throws IOException if writing failed
     */
    public final void writeBytes(String s) throws IOException {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            write((byte) s.charAt(i));
        }
    }


}
