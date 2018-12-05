package com.epam.catgenome.util.aws;

import com.amazonaws.services.s3.AmazonS3URI;
import htsjdk.samtools.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.IOException;
import java.io.InputStream;

/**
 * A custom Stream for parallel file reading.
 */
public class S3ParallelStream extends InputStream {

    private static final int EOF_BYTE = -1;
    private final Log log = Log.getInstance(S3ParallelStream.class);
    private final AmazonS3URI uri;
    private final long from;
    private final long to;
    long curPosition;
    int downlPartSize = Configuration.getMinDownloadPartSize();


    private volatile byte[] currentDataChunck;
    private volatile int chunckIndex;

    public S3ParallelStream(AmazonS3URI uri,
                            long from,
                            long to) {
        this.uri = uri;
        this.from = from;
        curPosition = from;
        this.to = to;
        currentDataChunck = new byte[0];
    }

    @Override
    public int read() throws IOException {
        if (chunckEndReached()) {
            currentDataChunck = getNewBuffer();
            chunckIndex = 0;
        }
        return getNextByte();
    }

    private int getNextByte() throws IOException {
        if (currentDataChunck == ParallelPartsLoader.EOF) {
            close();
            return EOF_BYTE;
        }
        int nextByte = currentDataChunck[chunckIndex] & (0xff);
        chunckIndex++;
        return nextByte;
    }

    private byte[] getNewBuffer() {
        long destPosition = Math.min(to, curPosition + downlPartSize);
        try {
            PartReader partReader = new PartReader(uri, curPosition, destPosition, new AtomicBoolean(false));
            curPosition = destPosition;
            return partReader.call().get();
        } catch (InterruptedException e) {
            return new byte[0];
        }
    }

    private boolean chunckEndReached() {
        return currentDataChunck.length == chunckIndex;
    }


    @Override
    public void close() throws IOException {
        log.debug("Loading is stopped.");
    }
}

