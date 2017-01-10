/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.util;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import htsjdk.samtools.Defaults;
import htsjdk.samtools.seekablestream.ISeekableStreamFactory;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.seekablestream.SeekableStreamFactory;
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedStreamConstants;
import htsjdk.samtools.util.LocationAware;
import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.CloseableTribbleIterator;
import htsjdk.tribble.Feature;
import htsjdk.tribble.FeatureCodec;
import htsjdk.tribble.FeatureCodecHeader;
import htsjdk.tribble.TribbleException;
import htsjdk.tribble.readers.PositionalBufferedStream;

/**
 * Source:      FeatureIterator
 * Created:     15.09.16, 18:02
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *<p>
 * A modification of HTSJDK's private class to allow low-level file operations
 *</p>
 */
public class FeatureIterator<T extends Feature, S> implements CloseableTribbleIterator<Feature> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureIterator.class);
    // the stream we use to get features
    private final S source;
    // the next feature
    private Feature nextFeature;
    // our codec
    private final FeatureCodec<T, S> codec;
    private final File inputFile;

    // we also need cache our position
    private long cachedPosition;

    /**
     * @param inputFile The file from which to read. Stream for reading is opened on construction.
     * @param codec feature codec, of features, that iterator should handle
     */
    public FeatureIterator(final File inputFile, final FeatureCodec<T, S> codec) {
        this.codec = codec;
        this.inputFile = inputFile;
        final FeatureCodecHeader header = readHeader();
        source = (S) codec.makeIndexableSourceFromStream(initStream(inputFile, header.getHeaderEnd()));
        readNextFeature();
    }

    /**
     * @param stream PositionalBufferedStream from file from which to read.
     * @param codec feature codec, of features, that iterator should handle
     */
    public FeatureIterator(PositionalBufferedStream stream, final FeatureCodec<T, S> codec) {
        this.codec = codec;
        this.inputFile = null;
        readHeader(stream);
        source = (S) codec.makeIndexableSourceFromStream(stream);
        readNextFeature();
    }

    /**
     * Some codecs,  e.g. VCF files,  need the header to decode features.  This is a rather poor design,
     * the internal header is set as a side-affect of reading it, but we have to live with it for now.
     */
    private FeatureCodecHeader readHeader() {
        try {
            final S sourceFromStream = this.codec.makeSourceFromStream(initStream(inputFile, 0));
            final FeatureCodecHeader header = this.codec.readHeader(sourceFromStream);
            codec.close(sourceFromStream);
            return header;
        } catch (IOException e) {
            LOGGER.error(MessageHelper.getMessage(MessagesConstants.ERROR_FILE_HEADER_READING), e);
            throw new TribbleException.InvalidHeader(e.getMessage());
        }
    }

    private FeatureCodecHeader readHeader(PositionalBufferedStream stream) {
        try {
            final S sourceFromStream = this.codec.makeSourceFromStream(stream);
            return this.codec.readHeader(sourceFromStream);
        } catch (IOException e) {
            LOGGER.error(MessageHelper.getMessage(MessagesConstants.ERROR_FILE_HEADER_READING), e);
            throw new TribbleException.InvalidHeader(e.getMessage());
        }
    }

    private PositionalBufferedStream initStream(final File inputFile, final long skip) {
        try (FileInputStream fileStream = new FileInputStream(inputFile)) {
            final InputStream is = createStream(fileStream);

            final PositionalBufferedStream pbs = new PositionalBufferedStream(is);

            if (skip > 0) {
                long skipped = pbs.skip(skip);
                if (skipped == 0) {
                    throw new EOFException();
                }
            }
            return pbs;
        } catch (final FileNotFoundException e) {
            LOGGER.error("Error while init stream", e);
            throw new TribbleException.FeatureFileDoesntExist(
                    "Unable to open the input file, most likely the file doesn't exist.", inputFile.getAbsolutePath());
        } catch (final IOException e) {
            throw new TribbleException.MalformedFeatureFile("Error initializing stream",
                    inputFile.getAbsolutePath(), e);
        }
    }

    @Override
    public boolean hasNext() {
        return nextFeature != null;
    }

    @Override
    public Feature next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No next element");
        }
        final Feature ret = nextFeature;
        readNextFeature();
        return ret;
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException("We cannot remove");
    }


    /**
     * @return the file position from the underlying reader
     */
    public long getPosition() {
        return hasNext() ? cachedPosition : ((LocationAware) source).getPosition();
    }

    @Override
    public Iterator<Feature> iterator() {
        return this;
    }

    @Override
    public void close() {
        codec.close(source);
    }

    /**
     * Read the next feature from the stream
     *
     * @throws TribbleException.MalformedFeatureFile
     */
    private void readNextFeature() {
        cachedPosition = ((LocationAware) source).getPosition();
        try {
            nextFeature = null;
            while (nextFeature == null && !codec.isDone(source)) {
                nextFeature = codec.decodeLoc(source);
            }
        } catch (final IOException e) {
            throw new TribbleException.MalformedFeatureFile("Unable to read a line from the file",
                    inputFile.getAbsolutePath(), e);
        }
    }

    private InputStream createStream(final FileInputStream fileStream) throws IOException {
        // if this looks like a block compressed file and it in fact is, we will use it
        // otherwise we will use the file as is
        if (!AbstractFeatureReader.hasBlockCompressedExtension(inputFile)) {
            return fileStream;
        }

        // make a buffered stream to test that this is in fact a valid block compressed file
        final int bufferSize = Math.max(Defaults.BUFFER_SIZE,
                BlockCompressedStreamConstants.MAX_COMPRESSED_BLOCK_SIZE);
        final BufferedInputStream bufferedStream = new BufferedInputStream(fileStream, bufferSize);

        if (!BlockCompressedInputStream.isValidFile(bufferedStream)) {
            throw new TribbleException.MalformedFeatureFile(
                    "Input file is not in valid block compressed format.", inputFile.getAbsolutePath());
        }

        final ISeekableStreamFactory ssf = SeekableStreamFactory.getInstance();
        // if we got here, the file is valid, make a SeekableStream for the BlockCompressedInputStream
        // to read from
        final SeekableStream seekableStream =
                ssf.getBufferedStream(ssf.getStreamFor(inputFile.getAbsolutePath()));
        return new BlockCompressedInputStream(seekableStream);
    }

}
