package com.epam.catgenome.util.feature.reader;

import htsjdk.samtools.seekablestream.ISeekableStreamFactory;
import htsjdk.samtools.seekablestream.SeekableBufferedStream;
import htsjdk.samtools.seekablestream.SeekableStream;

import java.io.IOException;
import java.net.URL;

public class S3SeekableStreamFactory implements ISeekableStreamFactory {

    private static ISeekableStreamFactory currentFactory = null;

    private S3SeekableStreamFactory() {
    }

    public static void setInstance() {
        currentFactory = new S3SeekableStreamFactory();
    }

    public static ISeekableStreamFactory getInstance() {
        return currentFactory;
    }

    @Override
    public SeekableStream getStreamFor(URL url) throws IOException {
        return getStreamFor(url.toExternalForm());
    }

    @Override
    public SeekableStream getStreamFor(String path) throws IOException {
        return new SeekableS3Stream(path);
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
}
