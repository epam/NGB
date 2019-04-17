/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018 EPAM Systems
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

import com.epam.catgenome.util.aws.S3Client;
import com.epam.catgenome.util.aws.S3SeekableStreamFactory;
import com.epam.catgenome.util.azure.AzureBlobClient;
import com.epam.catgenome.util.azure.AzureSeekableStremFactory;
import htsjdk.samtools.seekablestream.ISeekableStreamFactory;
import htsjdk.samtools.seekablestream.SeekableBufferedStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.seekablestream.SeekableStreamFactory;

import java.io.IOException;
import java.net.URL;

public final class NgbSeekableStreamFactory implements ISeekableStreamFactory {

    public static ISeekableStreamFactory getInstance() {
        return INSTANCE;
    }

    private static final ISeekableStreamFactory INSTANCE;

    static {
        INSTANCE = new NgbSeekableStreamFactory();
        SeekableStreamFactory.setInstance(INSTANCE);
    }

    private final ISeekableStreamFactory localSeekableStreamFactory;

    private NgbSeekableStreamFactory() {
        localSeekableStreamFactory = SeekableStreamFactory.getInstance();
    }

    @Override
    public SeekableStream getStreamFor(URL url) throws IOException {
        if(S3Client.isS3Source(url.toString())) {
            return S3SeekableStreamFactory.getInstance().getStreamFor(url);
        } else if (AzureBlobClient.isAzSource(url.toString())) {
            return AzureSeekableStremFactory.getInstance().getStreamFor(url);
        } else {
            return localSeekableStreamFactory.getStreamFor(url);
        }
    }

    @Override
    public SeekableStream getStreamFor(String path) throws IOException {
        if(S3Client.isS3Source(path)) {
            return S3SeekableStreamFactory.getInstance().getStreamFor(path);
        } else if (AzureBlobClient.isAzSource(path)) {
            return AzureSeekableStremFactory.getInstance().getStreamFor(path);
        } else {
            return localSeekableStreamFactory.getStreamFor(path);
        }
    }

    @Override
    public SeekableStream getBufferedStream(SeekableStream stream) {
        return getBufferedStream(stream, SeekableBufferedStream.DEFAULT_BUFFER_SIZE);
    }

    @Override
    public SeekableStream getBufferedStream(SeekableStream stream, int bufferSize) {
        if (bufferSize == 0){
            return stream;
        } else {
            return new SeekableBufferedStream(stream, bufferSize);
        }
    }

}
