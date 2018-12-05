package com.epam.catgenome.util.aws;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import com.amazonaws.services.s3.AmazonS3URI;
import htsjdk.samtools.util.Log;
import htsjdk.samtools.util.RuntimeIOException;

/**
 * A class for loading a specific part of target file.
 */
class PartReader implements Callable<Optional<byte[]>> {

    private static final Log LOG = Log.getInstance(PartReader.class);

    private static final int CHECK_INTERVAL = 512;
    public static final int END_BYTE = -1;

    private final AtomicBoolean canceledFlag;
    private final AmazonS3URI uri;
    private final long from;
    private final long to;

    private final String threadName;

    PartReader(AmazonS3URI uri, long from, long to, AtomicBoolean canceledFlag) {
        this.canceledFlag = canceledFlag;
        this.uri = uri;
        this.from = from;
        this.to = to;

        this.threadName = "[" + from + " : " + to + "](" + uri.toString() + ")";
    }

    @Override public Optional<byte[]> call() throws InterruptedException {
        LOG.debug("Launched ", threadName, "on ", uri.toString());
        Thread.currentThread().setName(threadName);

        return getByteArray(Configuration.getCustomRetryCount());
    }

    private Optional<byte[]> getByteArray(int remainingAttempts) throws InterruptedException {

        if (remainingAttempts == 0) {
            LOG.error("Ran out of connection retries to ", uri.toString(), " ", threadName);
            return Optional.empty();
        }

        byte[] loadedDataBuffer;
        try (InputStream s3DataStream = S3InputStreamFactory.loadFromTo(uri, from, to)) {
            loadedDataBuffer = loadDataFromStream(s3DataStream);
        } catch (RuntimeIOException e) {
            canceledFlag.set(true);
            throw e;
        } catch (IOException e) {
            LOG.warn("Reconnected ", threadName, e);
            return getByteArray(remainingAttempts - 1);
        }

        return Optional.of(loadedDataBuffer);
    }

    private byte[] loadDataFromStream(InputStream s3DataStream)
            throws IOException, InterruptedException {
        int bufferSize = Math.toIntExact(to - from);
        byte[] loadedDataBuffer = new byte[bufferSize];

        for (int dataLoaded = 0; dataLoaded < bufferSize; dataLoaded++) {
            int byteRead = s3DataStream.read();

            if (byteRead == END_BYTE) {
                throw new RuntimeIOException("Data stream ends ahead.");
            }

            if (dataLoaded % CHECK_INTERVAL == 0 && canceledFlag.get()) {
                LOG.debug("Loading canceled on. ", uri.toString(), " ", threadName);
                throw new InterruptedException("Loading canceled!");

            }
            loadedDataBuffer[dataLoaded] = (byte) byteRead;
        }

        return loadedDataBuffer;
    }
}
