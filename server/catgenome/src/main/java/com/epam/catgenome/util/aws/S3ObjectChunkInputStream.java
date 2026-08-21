package com.epam.catgenome.util.aws;

import org.apache.commons.io.IOUtils;
import com.epam.catgenome.util.FeatureInputStream;
import htsjdk.samtools.util.RuntimeIOException;

import java.io.IOException;
import java.io.InputStream;

/**
 * A custom Stream for parallel file reading.
 */
public class S3ObjectChunkInputStream extends FeatureInputStream {

    public S3ObjectChunkInputStream(String uri, long from, long to) {
        super(uri, from, to);
    }

    @Override
    protected byte[] getBytes(long from, long to) {
        byte[] loadedDataBuffer;
        try (InputStream s3DataStream = S3Client.getInstance().loadFromTo(uri, from, to)) {
            loadedDataBuffer = IOUtils.toByteArray(s3DataStream);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        return loadedDataBuffer;
    }
}

