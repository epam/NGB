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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

/**
 * Source:      BlockCompressedDataInputStreamTest
 * Created:     05.10.16, 14:01
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
public class BlockCompressedDataStreamsTest {
    private static final int TEST_CHAR_ID = 13;
    private static final int TEST_COMPRESSION_LEVEL = 9;

    @Test
    public void testOutputStream() throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (BlockCompressedDataOutputStream outputStream = new BlockCompressedDataOutputStream(
                byteArrayOutputStream, null)) {
            doWrite(outputStream);
        }

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        try (BlockCompressedDataInputStream inputStream = new BlockCompressedDataInputStream(byteArrayInputStream)) {
            doRead(inputStream);
        }

        File tmpFile = File.createTempFile("testFile", "");
        try (BlockCompressedDataOutputStream outputStream = new BlockCompressedDataOutputStream(tmpFile)) {
            doWrite(outputStream);
        }
        try (BlockCompressedDataInputStream inputStream = new BlockCompressedDataInputStream(tmpFile)) {
            doRead(inputStream);
        }

        tmpFile = File.createTempFile("testFile", "");
        try (BlockCompressedDataOutputStream outputStream = new BlockCompressedDataOutputStream(tmpFile,
                TEST_COMPRESSION_LEVEL)) {
            doWrite(outputStream);
        }
        try (BlockCompressedDataInputStream inputStream = new BlockCompressedDataInputStream(tmpFile)) {
            doRead(inputStream);
        }
    }

    private void doWrite(BlockCompressedDataOutputStream outputStream) throws IOException {
        outputStream.writeFloat(RandomUtils.nextFloat());
        outputStream.writeDouble(RandomUtils.nextDouble());
        outputStream.writeLong(RandomUtils.nextLong());
        outputStream.writeInt(RandomUtils.nextInt());
        outputStream.writeShort(0);
        outputStream.writeBoolean(RandomUtils.nextBoolean());
        outputStream.writeBytes("test");
        outputStream.writeByte(RandomUtils.nextInt());
        outputStream.writeChar(TEST_CHAR_ID);
        outputStream.writeChars("test");
        outputStream.flush();
    }

    private void doRead(BlockCompressedDataInputStream inputStream) throws IOException {
        try {
            inputStream.readFloat();
            inputStream.readDouble();
            inputStream.readLong();
            inputStream.readInt();
            inputStream.readShort();
            inputStream.readUnsignedByte();
            inputStream.readLine();
            inputStream.readUnsignedShort();
            inputStream.readChar();
            inputStream.readLine();
        } catch (EOFException e) {
            return;
        }
    }
}