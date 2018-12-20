package com.epam.catgenome.util.aws;

import com.amazonaws.util.IOUtils;
import htsjdk.samtools.util.RuntimeIOException;

import java.io.IOException;
import java.io.InputStream;

/**
 * A custom Stream for parallel file reading.
 */
public class S3ObjectChunkInputStream extends InputStream {

    private static final int CHUNK_SIZE = 64 * 1024;
    private static final int INVERSE_MASK = 0xff;
    private static final int EOF_BYTE = -1;

    private final String uri;
    private long position;
    private final long to;

    private byte[] currentDataChunck;
    private int chunckIndex;

    public S3ObjectChunkInputStream(String uri, long from, long to) {
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
        try (InputStream s3DataStream = S3Client.getInstance().loadFromTo(uri, from, to)) {
            loadedDataBuffer = IOUtils.toByteArray(s3DataStream);
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

