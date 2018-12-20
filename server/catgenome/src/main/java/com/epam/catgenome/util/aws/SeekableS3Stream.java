package com.epam.catgenome.util.aws;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.utils.CountingInputStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper class for S3ObjectChunkInputStream that supports @<code>seek()</code> method.
 * If position skip requested by seek() is small, just skips bytes. Otherwise, recreates stream.
 */
public class SeekableS3Stream extends SeekableStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeekableS3Stream.class);

    private final String s3Source;
    private CountingInputStream currentDataStream;
    private final long contentLength;
    private long offset;

    SeekableS3Stream(String source) {
        this.s3Source = source;
        contentLength = S3Client.getInstance().getFileSize(s3Source);
        recreateInnerStream();
    }

    private void recreateInnerStream() {
        if (null != currentDataStream) {
            try {
                currentDataStream.close();
            } catch (IOException e) {
                throw new RuntimeIOException(e.getMessage() + "failed to close the data stream", e);
            }
        }

        this.currentDataStream = new CountingWithSkipInputStream(
                new S3ObjectChunkInputStream(s3Source, offset, length() - 1));
        LOGGER.debug("A new data stream was launched on offset = {}", offset);
    }

    @Override
    public long length() {
        return contentLength;
    }

    @Override
    public long position() throws IOException {
        return offset + currentDataStream.getBytesRead();
    }

    /**
     * A method that jumps to a specific position in file.
     *
     * @param targetPosition target position in file.
     */
    @Override
    public void seek(long targetPosition) throws IOException {
        LOGGER.debug("Seeking from {} to {}", position(), targetPosition);
        this.offset = targetPosition;
        recreateInnerStream();
    }

    @Override
    public int read() throws IOException {
        return currentDataStream.read();
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return currentDataStream.read(buffer, offset, length);
    }

    @Override
    public void close() throws IOException {
        currentDataStream.close();
    }

    @Override
    public boolean eof() throws IOException {
        return position() == length();
    }

    @Override
    public String getSource() {
        return s3Source;
    }

    /**
     * We should count data skipped because we want to count all data loaded.
     */
    private static class CountingWithSkipInputStream extends CountingInputStream {
        CountingWithSkipInputStream(InputStream in) {
            super(in);
        }

        @Override
        public long skip(long n) throws IOException {
            long bytesSkipped = in.skip(n);
            count(bytesSkipped);

            return bytesSkipped;
        }
    }
}

