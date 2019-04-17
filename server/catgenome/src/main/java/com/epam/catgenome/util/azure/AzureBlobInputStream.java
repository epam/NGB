/*
 * MIT License
 *
 * Copyright (c) 2019 EPAM Systems
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

package com.epam.catgenome.util.azure;

import com.amazonaws.util.IOUtils;
import htsjdk.samtools.util.RuntimeIOException;

import java.io.IOException;
import java.io.InputStream;

public class AzureBlobInputStream extends InputStream {
    private static final int CHUNK_SIZE = 64 * 1024;
    private static final int INVERSE_MASK = 0xff;
    private static final int EOF_BYTE = -1;

    private final String uri;
    private long position;
    private final long to;
    private final AzureBlobClient client;

    private byte[] currentDataChunck;
    private int chunckIndex;

    public AzureBlobInputStream(String uri, long from, long to, final AzureBlobClient client) {
        this.uri = uri;
        this.position = from;
        this.to = to;
        this.client = client;
        this.currentDataChunck = new byte[0];
    }

    @Override
    public int read() throws IOException {
        if (chunckEndReached()) {
            currentDataChunck = getNewBuffer();
            chunckIndex = 0;
        }
        return getNextByte();
    }

    private int getNextByte() {
        return currentDataChunck[chunckIndex++] & INVERSE_MASK;
    }

    private byte[] getNewBuffer() {
        long destination = Math.min(to, position + CHUNK_SIZE);
        byte[] bytes;
        if (position < destination) {
            bytes = getBytes(position, destination);
        } else {
            bytes = new byte[]{EOF_BYTE};
        }
        position = destination + 1;
        return bytes;
    }

    private byte[] getBytes(long from, long to) {

        byte[] loadedDataBuffer;
        try (InputStream dataStream = client.loadFromTo(uri, from, to)) {
            loadedDataBuffer = IOUtils.toByteArray(dataStream);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }

        return loadedDataBuffer;
    }

    private boolean chunckEndReached() {
        return currentDataChunck.length == chunckIndex;
    }


    @Override
    public void close() throws IOException {
        currentDataChunck = null;
    }
}
