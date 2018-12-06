package com.epam.catgenome.util.aws;

import com.amazonaws.services.s3.AmazonS3URI;
import htsjdk.samtools.util.Log;
import htsjdk.samtools.util.RuntimeIOException;

import java.io.IOException;
import java.io.InputStream;

/**
 * A custom Stream for parallel file reading.
 */
public class S3ObjectChunkInputStream extends InputStream {

    private static final int CHUNK_SIZE = 32 * 1024;

    private static final int EOF_BYTE = -1;
    private final Log log = Log.getInstance(S3ObjectChunkInputStream.class);
    private final AmazonS3URI uri;
    private long position;
    private final long to;

    private byte[] currentDataChunck;
    private int chunckIndex;

    public S3ObjectChunkInputStream(AmazonS3URI uri, long from, long to) {
        this.uri = uri;
        position = from;
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

    private int getNextByte() {
        return currentDataChunck[chunckIndex++] & (0xff);
    }

    private byte[] getNewBuffer() {
        long destination = Math.min(to, position + CHUNK_SIZE);
        byte[] bytes = getBytes(position, destination, 3);
        position = destination;
        return bytes;
    }

    private byte[] getBytes(long from, long to, int remainingAttempts) {

        if (remainingAttempts == 0) {
            log.error("Ran out of connection retries to ", uri.toString());
            return new byte[0];
        }

        byte[] loadedDataBuffer;
        try (InputStream s3DataStream = S3Client.loadFromTo(uri, from, to)) {
            loadedDataBuffer = loadDataFromStream(s3DataStream, Math.toIntExact(to - from));
        } catch (IOException e) {
            log.warn("Reconnected on position: ", from, e);
            return getBytes(from, to,remainingAttempts - 1);
        }

        return loadedDataBuffer;
    }

    private byte[] loadDataFromStream(InputStream s3DataStream, int size) throws IOException {
        byte[] loadedDataBuffer = new byte[size];

        for (int dataLoaded = 0; dataLoaded < size; dataLoaded++) {
            int byteRead = s3DataStream.read();

            if (byteRead == EOF_BYTE) {
                throw new RuntimeIOException("Data stream ends ahead.");
            }

            loadedDataBuffer[dataLoaded] = (byte) byteRead;
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

