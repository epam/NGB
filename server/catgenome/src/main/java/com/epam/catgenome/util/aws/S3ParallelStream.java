package com.epam.catgenome.util.aws;

import com.amazonaws.services.s3.AmazonS3URI;
import htsjdk.samtools.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * A custom Stream for parallel file reading.
 */
public class S3ParallelStream extends InputStream {

    private static final int EOF_BYTE = -1;
    private final Log log = Log.getInstance(S3ParallelStream.class);

    private final ParallelPartsLoader taskProducer;

    private volatile byte[] currentDataChunck;
    private volatile int chunckIndex;

    public S3ParallelStream(AmazonS3URI uri,
                            long from,
                            long to,
                            S3InputStreamFactory factory) {

        taskProducer = new ParallelPartsLoader(uri, from, to, factory);
        currentDataChunck = new byte[0];
    }

    @Override
    public int read() throws IOException {
        if (chunckEndReached()) {
            currentDataChunck = taskProducer.fetchNextPart();
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

    private boolean chunckEndReached() {
        return currentDataChunck.length == chunckIndex;
    }

    @Override
    public void close() throws IOException {
        taskProducer.cancelLoading();
        log.debug("Loading is stopped.");
    }
}
