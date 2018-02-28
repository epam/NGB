package com.epam.catgenome.util.aws;

import com.amazonaws.services.s3.AmazonS3URI;
import htsjdk.samtools.seekablestream.ISeekableStreamFactory;
import htsjdk.samtools.seekablestream.SeekableBufferedStream;
import htsjdk.samtools.seekablestream.SeekableStream;

import java.net.URL;

public class S3SeekableStreamFactory implements ISeekableStreamFactory {

    private static ISeekableStreamFactory currentFactory = new S3SeekableStreamFactory();

    private S3SeekableStreamFactory() {
    }

    public static ISeekableStreamFactory getInstance() {
        return currentFactory;
    }

    @Override
    public SeekableStream getStreamFor(URL url){
        return getStreamFor(url.toExternalForm());
    }

    @Override
    public SeekableStream getStreamFor(String path) {
        return new SeekableS3Stream(new AmazonS3URI(path), new S3Client());
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
