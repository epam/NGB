package com.epam.catgenome.util;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.epam.catgenome.manager.bam.BamHelper;
import org.apache.commons.io.IOUtils;

import com.epam.catgenome.exception.IndexException;
import htsjdk.samtools.seekablestream.ISeekableStreamFactory;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.seekablestream.SeekableStreamFactory;
import htsjdk.samtools.util.AbstractIterator;
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedStreamConstants;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.LocationAware;
import htsjdk.samtools.util.RuntimeIOException;
import htsjdk.samtools.util.Tuple;
import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import htsjdk.tribble.CloseableTribbleIterator;
import htsjdk.tribble.Feature;
import htsjdk.tribble.FeatureCodec;
import htsjdk.tribble.FeatureCodecHeader;
import htsjdk.tribble.TribbleException;
import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.IndexCreator;
import htsjdk.tribble.index.IndexFactory;
import htsjdk.tribble.index.tabix.TabixFormat;
import htsjdk.tribble.index.tabix.TabixIndex;
import htsjdk.tribble.index.tabix.TabixIndexCreator;
import htsjdk.tribble.readers.AsciiLineReader;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.tribble.readers.LineReader;
import htsjdk.tribble.readers.PositionalBufferedStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epam.catgenome.util.NgbFileUtils.isGzCompressed;

/**
 * An utility class for creating Tabix (tbi) indexes for tab delimited BGZIP-compressed files (VCF,
 * BED, GFF). All calls to this class should be removed, after creating Tabix indexes will be fixed
 * in HTSJDK library.
 */
public final class IndexUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexUtils.class);
    private static String patternString = "(.*?)\\?";
    private static Pattern pattern = Pattern.compile(patternString);

    private IndexUtils() {
        //no operations
    }

    /**
     * Try to find index for given file path. This method checks if the directory with this
     * file also contains index for it
     * @param filePath file path for checking
     * @return index file path for the file otherwise null
     * */
    public static String checkExistingIndex(String filePath) {
        List<String> possibleIndexPathes = new ArrayList<>();
        if(BamUtil.isBam(filePath)) {
            String fileExtension = NgbFileUtils.getFileExtension(filePath);
            String indexExtension = BamHelper.BAI_EXTENSIONS.get(fileExtension);
            possibleIndexPathes.add(filePath + indexExtension);
            possibleIndexPathes.add(filePath.replace(fileExtension, indexExtension));
        } else {
            if (isGzCompressed(filePath)) {
                possibleIndexPathes.add(filePath + NgbFileUtils.TBI_EXTENSION);
                possibleIndexPathes.add(filePath.replace(NgbFileUtils.GZ_EXTENSION, NgbFileUtils.TBI_EXTENSION));
            } else {
                possibleIndexPathes.add(filePath + NgbFileUtils.IDX_EXTENSION);
            }
        }

        for (String possibleIndexPath : possibleIndexPathes) {
            File indexFile = new File(possibleIndexPath);
            if (indexFile.exists() && indexFile.canRead()) {
                return indexFile.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * If file isn't compressed HTSJDK method is used, for compressed files our methods are
     * used, which create the tabix index correctly.
     *
     * @param inputFile
     * @param codec
     * @param tabixFormat
     * @param <F>
     * @param <S>
     * @return
     */
    public static <F extends Feature, S> TabixIndex createTabixIndex(final File inputFile,
            final FeatureCodec<F, S> codec, final TabixFormat tabixFormat) {
        if (AbstractFeatureReader.hasBlockCompressedExtension(inputFile)) {
            final TabixIndexCreator indexCreator = new TabixIndexCreator(null, tabixFormat);
            return (TabixIndex) createIndex(inputFile, new FeatureIterator<>(inputFile, codec),
                    indexCreator);
        } else {
            return IndexFactory.createTabixIndex(inputFile, codec, tabixFormat, null);
        }
    }

    private static Index createIndex(final File inputFile, final FeatureIterator iterator,
            final IndexCreator creator) {
        Feature lastFeature = null;
        Feature currentFeature;
        final Map<String, Feature> visitedChromos = new HashMap<>(40);
        while (iterator.hasNext()) {
            final long position = iterator.getPosition();
            currentFeature = iterator.next();

            checkSorted(inputFile, lastFeature, currentFeature, visitedChromos);

            creator.addFeature(currentFeature, position);
            lastFeature = currentFeature;
        }

        iterator.close();
        return creator.finalizeIndex(iterator.getPosition());
    }

    public static void checkSorted(final File inputFile, final Feature lastFeature,
            final Feature currentFeature, Map<String, Feature> visitedChromos) {
        // if the last currentFeature is after the current currentFeature, exception out
        if (lastFeature != null && currentFeature.getStart() < lastFeature.getStart() && lastFeature
                .getContig().equals(currentFeature.getContig())) {
            throw new TribbleException.MalformedFeatureFile(
                    "Input file is not sorted by start position. \n"
                            + "We saw a record with a start of " + currentFeature.getContig() + ":"
                            + currentFeature.getStart() + " after a record with a start of "
                            + lastFeature.getContig() + ":" + lastFeature.getStart(),
                    inputFile.getAbsolutePath());
        }

        //should only visit chromosomes once
        final String curChr = currentFeature.getContig();
        final String lastChr = lastFeature != null ? lastFeature.getContig() : null;
        if (!curChr.equals(lastChr)) {
            if (visitedChromos.containsKey(curChr)) {
                String msg = "Input file must have contiguous chromosomes.";
                throw new TribbleException.MalformedFeatureFile(msg,
                        inputFile.getAbsolutePath());
            } else {
                visitedChromos.put(curChr, currentFeature);
            }
        }

    }

    /**
     * Iterator for reading features from a file, given a {@code FeatureCodec}. A modification of HTSJDK's private
     * class to allow low-level file operations
     */
    public static final class FeatureIterator<F extends Feature, S>
            implements CloseableTribbleIterator<Feature> {
        // the stream we use to get features
        private final S source;
        // the next feature
        private Feature nextFeature;
        // our codec
        private final FeatureCodec<F, S> codec;
        private final File inputFile;

        // we also need cache our position
        private long cachedPosition;

        /**
         * @param inputFile The file from which to read. Stream for reading is opened on construction.
         * @param codec
         */
        public FeatureIterator(final File inputFile, final FeatureCodec<F, S> codec) {
            this.codec = codec;
            this.inputFile = inputFile;
            final FeatureCodecHeader header = readHeader();
            source =
                    (S) makeIndexableSourceFromStream(initStream(inputFile, header.getHeaderEnd()));
            readNextFeature();
        }

        public FeatureIterator(PositionalBufferedStream stream, final FeatureCodec<F, S> codec) throws IOException {
            this.codec = codec;
            this.inputFile = null;
            initStream(stream, readHeader(stream).getHeaderEnd());
            source = (S) codec.makeIndexableSourceFromStream(stream);
            readNextFeature();
        }

        public LocationAware makeIndexableSourceFromStream(InputStream bufferedInputStream) {
            final BlockCompressedInputStream pbs;
            if (bufferedInputStream instanceof BlockCompressedInputStream) {
                pbs = (BlockCompressedInputStream) bufferedInputStream;
            } else {
                throw new IllegalArgumentException();
            }
            return new TabixLineReaderIterator(new TabixLineReader(pbs));
        }

        /**
         * Some codecs,  e.g. VCF files,  need the header to decode features.  This is a rather poor design,
         * the internal header is set as a side-affect of reading it, but we have to live with it for now.
         */
        private FeatureCodecHeader readHeader() {
            try {
                final S headerSource = this.codec.makeSourceFromStream(initStream(inputFile, 0));
                final FeatureCodecHeader header = this.codec.readHeader(headerSource);
                codec.close(headerSource);
                return header;
            } catch (final IOException e) {
                throw new IndexException(e.getMessage(), e);
            }
        }

        private FeatureCodecHeader readHeader(PositionalBufferedStream stream) {
            try {
                final S sourceFromStream = this.codec.makeSourceFromStream(stream);
                return this.codec.readHeader(sourceFromStream);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw new TribbleException.InvalidHeader(e.getMessage());
            }
        }

        private InputStream initStream(final File inputFile, final long skip) {
            try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                InputStream is;
                // if this looks like a block compressed file and it in fact is, we will use it
                // otherwise we will use the file as is
                if (AbstractFeatureReader.hasBlockCompressedExtension(inputFile)) {
                    // make a buffered stream to test that this is in fact a valid block compressed file
                    final int bufferSize = BlockCompressedStreamConstants.MAX_COMPRESSED_BLOCK_SIZE;
                    final BufferedInputStream bufferedStream =
                            new BufferedInputStream(fileStream, bufferSize);

                    if (!BlockCompressedInputStream.isValidFile(bufferedStream)) {
                        throw new TribbleException.MalformedFeatureFile(
                                "Input file is not in valid block compressed format.",
                                inputFile.getAbsolutePath());
                    }

                    final ISeekableStreamFactory ssf = SeekableStreamFactory.getInstance();
                    final SeekableStream seekableStream =
                            ssf.getStreamFor(inputFile.getAbsolutePath());
                    is = new BlockCompressedInputStream(seekableStream);
                } else {
                    throw new IllegalArgumentException("Only compressed files are supported.");
                }
                if (skip > 0) {
                    IOUtils.skip(is, skip);
                }
                return is;
            } catch (final FileNotFoundException e) {
                throw new IndexException(e.getMessage(), e);
            } catch (final IOException e) {
                throw new TribbleException.MalformedFeatureFile("Error initializing stream",
                        inputFile.getAbsolutePath(), e);
            }
        }

        private void initStream(PositionalBufferedStream stream, final long skip) throws IOException {
            if (skip > 0) {
                IOUtils.skip(stream, skip);
            }
        }

        @Override public boolean hasNext() {
            return nextFeature != null;
        }

        @Override public Feature next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            final Feature ret = nextFeature;
            readNextFeature();
            return ret;
        }

        /**
         * @throws UnsupportedOperationException
         */
        @Override public void remove() {
            throw new UnsupportedOperationException("We cannot remove");
        }


        /**
         * @return the file position from the underlying reader
         */
        public long getPosition() {
            return hasNext() ? cachedPosition : ((LocationAware) source).getPosition();
        }

        @Override public Iterator<Feature> iterator() {
            return this;
        }

        @Override public void close() {
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
                throw new TribbleException.MalformedFeatureFile(
                        "Unable to read a line from the file", inputFile.getAbsolutePath(), e);
            }
        }
    }

    public static class TabixLineReader implements LineReader, LocationAware {

        private long lastPosition = -1;
        private BlockCompressedInputStream is;
        private boolean closed = false;

        public TabixLineReader(final BlockCompressedInputStream is) {
            this.is = is;
            try {
                is.available();
            } catch (IOException e) {
                throw new IndexException(e.getMessage(), e);
            }
        }


        /**
         * @return The position of the InputStream
         */
        @Override public long getPosition() {
            if (is == null) {
                throw new TribbleException(
                        "getPosition() called but no default stream was provided to the class on creation");
            }
            if (lastPosition < 0) {
                return is.getFilePointer();
            } else {
                return lastPosition;
            }
        }

        @Override public final String readLine() throws IOException {
            if (is == null) {
                throw new TribbleException(
                        "readLine() called without an explicit stream argument but no default"
                                + " stream was provided to the class on creation");
            }
            return is.readLine();
        }

        @Override public void close() {
            if (is != null && !closed) {
                try {
                    lastPosition = is.getFilePointer();
                    is.close();
                    closed = true;
                } catch (IOException e) {
                    throw new IndexException(e.getMessage(), e);
                }
            }
        }

    }

    public static class TabixLineReaderIterator implements LocationAware, LineIterator, Closeable {
        private final TabixLineReader lineReader;
        private final TabixLineReaderIterator.TupleIterator i;

        public TabixLineReaderIterator(final TabixLineReader lineReader) {
            this.lineReader = lineReader;
            this.i = new TabixLineReaderIterator.TupleIterator();
        }

        @Override public void close() throws IOException {
            CloserUtil.close(lineReader);
        }

        @Override public boolean hasNext() {
            return i.hasNext();
        }

        @Override public String next() {
            Tuple<String, Long> current = i.next();
            return current.a;
        }

        @Override public void remove() {
            i.remove();
        }

        /**
         * Returns the byte position at the end of the most-recently-read line (a.k.a.,
         * he beginning of the next line) from {@link #next()} in
         * the underlying {@link AsciiLineReader}.
         */
        @Override public long getPosition() {
            return i.getPosition();
        }

        @Override public String peek() {
            return i.peek().a;
        }

        /**
         * This is stored internally since it iterates over {@link htsjdk.samtools.util.Tuple},
         * not {@link String} (and the outer
         * class can't do both).
         */
        private final class TupleIterator extends AbstractIterator<Tuple<String, Long>>
                implements LocationAware {

            private TupleIterator() {
                super.hasNext();
            }

            @Override protected Tuple<String, Long> advance() {
                final String line;
                final long position = lineReader.getPosition();
                try {
                    line = lineReader.readLine();
                } catch (IOException e) {
                    throw new RuntimeIOException(e);
                }
                return line == null ? null : new Tuple<>(line, position);
            }

            /**
             * Returns the byte position at the beginning of the next line.
             */
            @Override public long getPosition() {
                final Tuple<String, Long> peek = super.peek();
                // Be careful: peek will be null at the end of the stream.
                return peek != null ? peek.b : lineReader.getPosition();
            }
        }
    }

    /**
     * Returns first part of URL for S3 created links (if "?" include in path) or else full path
     */
    public static String getFirstPartForIndexPath(String indexPath) {
        Matcher matcher = pattern.matcher(indexPath);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return indexPath;
        }
    }
}
