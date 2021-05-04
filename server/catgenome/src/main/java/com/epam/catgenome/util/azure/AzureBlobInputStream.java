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

package com.epam.catgenome.util.azure;

import com.amazonaws.util.IOUtils;
import com.epam.catgenome.util.FeatureInputStream;
import htsjdk.samtools.util.RuntimeIOException;

import java.io.IOException;
import java.io.InputStream;

public class AzureBlobInputStream extends FeatureInputStream {

    private final AzureBlobClient client;

    public AzureBlobInputStream(String uri, long from, long to, final AzureBlobClient client) {
        super(uri, from, to);
        this.client = client;
    }

    @Override
    public int read() throws IOException {
        if (chunckEndReached()) {
            currentDataChunck = getNewBuffer();
            chunckIndex = 0;
        }
        return getNextByte();
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
}
