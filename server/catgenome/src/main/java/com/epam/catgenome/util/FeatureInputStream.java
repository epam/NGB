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