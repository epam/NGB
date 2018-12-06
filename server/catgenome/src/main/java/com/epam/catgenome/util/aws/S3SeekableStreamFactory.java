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

package com.epam.catgenome.util.aws;

import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.util.IOUtils;
import htsjdk.samtools.seekablestream.ISeekableStreamFactory;
import htsjdk.samtools.seekablestream.SeekableBufferedStream;
import htsjdk.samtools.seekablestream.SeekableMemoryStream;
import htsjdk.samtools.seekablestream.SeekableStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class S3SeekableStreamFactory implements ISeekableStreamFactory {

    private static S3SeekableStreamFactory currentFactory = new S3SeekableStreamFactory();

    private S3SeekableStreamFactory() {
    }

    public static S3SeekableStreamFactory getInstance() {
        return currentFactory;
    }

    @Override
    public SeekableStream getStreamFor(URL url){
        return getStreamFor(url.toExternalForm());
    }

    @Override
    public SeekableStream getStreamFor(String path) {
        return new SeekableS3Stream(new AmazonS3URI(path));
    }

    @Override
    public SeekableStream getBufferedStream(SeekableStream stream) {
        return getBufferedStream(stream, SeekableBufferedStream.DEFAULT_BUFFER_SIZE);
    }

    @Override
    public SeekableStream getBufferedStream(SeekableStream stream, int bufferSize) {
        if (bufferSize == 0) return stream;
        else return new SeekableBufferedStream(stream, bufferSize);
    }

    public SeekableStream getInMemorySeekableStream(String url) throws IOException {
        AmazonS3URI indexURI = new AmazonS3URI(url);
        InputStream stream = S3Client.loadFully(indexURI);
        long fileSize = S3Client.getFileSize(indexURI);
        byte[] buffer = IOUtils.toByteArray(stream);

        if (fileSize != buffer.length) {
            throw new IOException("Failed to fully download index " + indexURI);
        }

        return new SeekableMemoryStream(buffer, indexURI.toString());
    }
}
