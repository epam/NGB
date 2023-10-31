/*
 * MIT License
 *
 * Copyright (c) 2017-2022 EPAM Systems
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
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.Interval;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.manager.GeneInfo;
import com.epam.catgenome.manager.bam.BamHelper;
import com.epam.catgenome.manager.gene.GeneUtils;
import htsjdk.samtools.Defaults;
import htsjdk.samtools.util.AbstractIterator;
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedStreamConstants;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.LocationAware;
import htsjdk.samtools.util.RuntimeIOException;
import htsjdk.samtools.util.Tuple;
import htsjdk.tribble.index.interval.IntervalIndexCreator;
import htsjdk.tribble.index.interval.IntervalTreeIndex;
import htsjdk.tribble.util.TabixUtils;
import htsjdk.variant.vcf.VCFCodec;
import org.apache.commons.io.IOUtils;

import com.epam.catgenome.exception.IndexException;
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
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epam.catgenome.util.NgbFileUtils.isGzCompressed;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * An utility class for creating Tabix (tbi) indexes for tab delimited BGZIP-compressed files (VCF,
 * BED, GFF). All calls to this class should be removed, after creating Tabix indexes will be fixed
 * in HTSJDK library.
 */
public final class IndexUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexUtils.class);
    private static final String FIRST_PART_PATH_REGEX = "(.*?)\\?";
    private static final Pattern FIRST_PART_PATH_PATTERN = Pattern.compile(FIRST_PART_PATH_REGEX);
    private static final String LINE_DELIMITER = "|";
    private static final String LINE_DELIMITER_PATTERN = "\\|";
    private static final String TERM_SPLIT_TOKEN = " ";

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
     */
    public static <F extends Feature, S> TabixIndex createTabixIndex(final FeatureFile featureFile,
            final FeatureCodec<F, S> codec, final TabixFormat tabixFormat) {

        try {
            final TabixIndexCreator indexCreator = new TabixIndexCreator(null, tabixFormat);
            return (TabixIndex) createIndex(featureFile.getPath(),
                    new FeatureIterator<>(featureFile.getPath(), codec), indexCreator);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static IntervalTreeIndex createIntervalIndex(VcfFile vcfFile, VCFCodec codec) {
        try {
            final IntervalIndexCreator indexCreator = new IntervalIndexCreator(new File(vcfFile.getPath()));
            return (IntervalTreeIndex)createIndex(vcfFile.getPath(),
                    new FeatureIterator<>(vcfFile.getPath(), codec), indexCreator);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    private static Index createIndex(final String filePath, final FeatureIterator iterator,
            final IndexCreator creator) {
        Feature lastFeature = null;
        Feature currentFeature;
        final Map<String, Feature> visitedChromos = new HashMap<>(40);
        while (iterator.hasNext()) {
            final long position = iterator.getPosition();
            currentFeature = iterator.next();

            checkSorted(filePath, lastFeature, currentFeature, visitedChromos);

            creator.addFeature(currentFeature, position);
            lastFeature = currentFeature;
        }

        iterator.close();
        return creator.finalizeIndex(iterator.getPosition());
    }

    public static void checkSorted(final String inputFile, final Feature lastFeature,
            final Feature currentFeature, Map<String, Feature> visitedChromos) {
        // if the last currentFeature is after the current currentFeature, exception out
        if (lastFeature != null && currentFeature.getStart() < lastFeature.getStart() && lastFeature
                .getContig().equals(currentFeature.getContig())) {
            throw new TribbleException.MalformedFeatureFile(
                    "Input file is not sorted by start position. \n"
                            + "We saw a record with a start of " + currentFeature.getContig() + ":"
                            + currentFeature.getStart() + " after a record with a start of "
                            + lastFeature.getContig() + ":" + lastFeature.getStart(),
                    inputFile);
        }

        //should only visit chromosomes once
        final String curChr = currentFeature.getContig();
        final String lastChr = lastFeature != null ? lastFeature.getContig() : null;
        if (!curChr.equals(lastChr)) {
            if (visitedChromos.containsKey(curChr)) {
                String msg = "Input file must have contiguous chromosomes.";
                throw new TribbleException.MalformedFeatureFile(msg,
                        inputFile);
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

        public FeatureIterator(String featureFilePath, final FeatureCodec<F, S> codec) throws IOException {
            this.codec = codec;
            this.inputFile = null;
            InputStream headerStream;
            if (AbstractFeatureReader.hasBlockCompressedExtension(featureFilePath)) {
                headerStream = new BlockCompressedInputStream(IOHelper.openStream(featureFilePath));
            } else {
                headerStream = IOHelper.openStream(featureFilePath);
            }

            try (PositionalBufferedStream buffHeaderStream = new PositionalBufferedStream(headerStream)) {
                FeatureCodecHeader header = readHeader(buffHeaderStream);
                source = (S) makeIndexableSourceFromStream(initStream(featureFilePath, header.getHeaderEnd()));
                readNextFeature();
            }
        }

        public LocationAware makeIndexableSourceFromStream(InputStream bufferedInputStream) {
            final BlockCompressedInputStream pbs;
            if (bufferedInputStream instanceof BlockCompressedInputStream) {
                pbs = (BlockCompressedInputStream) bufferedInputStream;
            } else {
                return codec.makeIndexableSourceFromStream(bufferedInputStream);
            }
            return new TabixLineReaderIterator(new TabixLineReader(pbs));
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

        private InputStream initStream(final String inputFile, final long skip) {
            try (InputStream fileStream = IOHelper.openStream(inputFile)) {
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
                                inputFile);
                    }

                    is = new BlockCompressedInputStream(IOHelper.openStream(inputFile));
                } else {
                    is = IOHelper.openStream(inputFile);
                }
                if (skip > 0) {
                    IOUtils.skip(is, skip);
                }
                return is;
            } catch (final FileNotFoundException e) {
                throw new IndexException(e.getMessage(), e);
            } catch (final IOException e) {
                throw new TribbleException.MalformedFeatureFile("Error initializing stream",
                        inputFile, e);
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
        Matcher matcher = FIRST_PART_PATH_PATTERN.matcher(indexPath);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return indexPath;
        }
    }

    /**
     * Load in index from the specified file.   The type of index (LinearIndex or IntervalTreeIndex) is determined
     * at run time by reading the type flag in the file.
     *
     * @param indexResource from which to load the index
     */
    @SuppressWarnings("all")
    public static Index loadIndex(final String indexResource) {
        // Must be buffered, because getIndexType uses mark and reset
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(
                indexFileInputStream(IOHelper.openStream(indexResource), Utils.getFileExtension(indexResource)),
                Defaults.NON_ZERO_BUFFER_SIZE)){
            final Class<Index> indexClass = IndexFactory.IndexType.getIndexType(bufferedInputStream).getIndexType();
            final Constructor<Index> ctor = indexClass.getConstructor(InputStream.class);
            return ctor.newInstance(bufferedInputStream);
        } catch (final IOException ex) {
            throw new TribbleException.UnableToReadIndexFile("Unable to read index file", indexResource, ex);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void deleteIndexDocument(final String field, final long value, final String indexDirectory)
            throws IOException {
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            final Term term = new Term(field, String.valueOf(value));
            writer.deleteDocuments(term);
        }
    }

    public static void addIntIntervalFilter(final String fieldName,
                                            final Interval<Integer> interval,
                                            final BooleanQuery.Builder builder) {
        if (interval.getFrom() != null || interval.getTo() != null) {
            builder.add(IntPoint.newRangeQuery(fieldName,
                    interval.getFrom() == null ? Integer.MIN_VALUE : interval.getFrom(),
                    interval.getTo() == null ? Integer.MAX_VALUE : interval.getTo()),
                    BooleanClause.Occur.MUST);
        }
    }

    public static void addFloatIntervalFilter(final String fieldName,
                                              final Interval<Float> interval,
                                              final BooleanQuery.Builder builder) {
        if (interval.getFrom() != null || interval.getTo() != null) {
            builder.add(FloatPoint.newRangeQuery(fieldName,
                    interval.getFrom() == null ? Float.MIN_VALUE : interval.getFrom(),
                    interval.getTo() == null ? Float.MAX_VALUE : interval.getTo()),
                    BooleanClause.Occur.MUST);
        }
    }

    /**
     * Fetch gene IDs of genes, affected by variation. The variation is specified by it's start and end indexes
     *
     * @param intervalMap represents a batch loaded genes form gene file
     * @param start       a start index of the variation
     * @param end         an end index of the variation
     * @param chromosome  a {@code Chromosome}
     * @return a {@code Set} of IDs of genes, affected by the variation
     */
    public static Set<GeneInfo> fetchGeneIdsFromBatch(final NggbIntervalTreeMap<List<Gene>> intervalMap,
                                                      final int start,
                                                      final int end,
                                                      final Chromosome chromosome) {
        final Set<GeneInfo> geneIds = getGeneIds(intervalMap, chromosome, start, start);
        if (end > start) {
            geneIds.addAll(getGeneIds(intervalMap, chromosome, end, end));
        }
        return geneIds;
    }

    public static Set<GeneInfo> getGeneIds(final NggbIntervalTreeMap<List<Gene>> intervalMap,
                                           final Chromosome chromosome,
                                           final int start,
                                           final int end) {
        final Collection<Gene> genes = intervalMap.getOverlapping(
                new htsjdk.samtools.util.Interval(chromosome.getName(), start, end))
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        final boolean isExon = genes.stream().anyMatch(GeneUtils::isExon);
        return genes.stream()
                .filter(GeneUtils::isGene)
                .map(g -> new GeneInfo(g.getGroupId(), g.getFeatureName(), isExon))
                .collect(Collectors.toSet());
    }

    public static String serialize(final List<String> tokens) {
        return join(tokens, LINE_DELIMITER);
    }

    public static List<String> deserialize(final String encoded) {
        return Arrays.asList(encoded.split(LINE_DELIMITER_PATTERN));
    }

    public static Query getByTermQuery(final String term, final String fieldName) throws ParseException {
        final StandardAnalyzer analyzer = new StandardAnalyzer();
        final QueryParser queryParser = new QueryParser(fieldName, analyzer);
        queryParser.setDefaultOperator(QueryParser.Operator.OR);
        return queryParser.parse(term);
    }

    public static Query getByTermsQuery(final List<String> ids, final String fieldName) throws ParseException {
        return getByTermQuery(join(ids, TERM_SPLIT_TOKEN), fieldName);
    }

    public static Query getByPrefixQuery(final String prefix, final String fieldName) {
        return new PrefixQuery(new Term(fieldName, prefix));
    }

    public static Query getByPhraseQuery(final String phrase, final String fieldName) {
        final BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (String term : phrase.split(TERM_SPLIT_TOKEN)) {
            builder.add(new WildcardQuery(new Term(fieldName, "*" + term.toLowerCase() + "*")),
                    BooleanClause.Occur.MUST);
        }
        return builder.build();
    }

    public static Query getByOptionsQuery(final List<String> options, final String fieldName) {
        final BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (String option : options) {
            builder.add(buildTermQuery(option, fieldName), BooleanClause.Occur.SHOULD);
        }
        return builder.build();
    }

    public static Query getByRangeQuery(final Interval<Float> interval, final String fieldName) {
        return FloatPoint.newRangeQuery(fieldName, interval.getFrom(), interval.getTo());
    }

    public static Query buildTermQuery(final String term, final String fieldName) {
        return new TermQuery(new Term(fieldName, term));
    }

    public static List<String> getFieldValues(final String fieldName, final String indexDirectory) throws IOException {
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            final List<String> fieldValues = new ArrayList<>();
            final Terms terms = MultiFields.getTerms(indexReader, fieldName);
            final TermsEnum termsEnum = terms.iterator();
            while (termsEnum.next() != null) {
                String fieldValue = termsEnum.term().utf8ToString();
                fieldValues.add(fieldValue);
            }
            return fieldValues;
        }
    }


    public static String getField(final Document doc, final String fieldName) {
        return doc.getField(fieldName).stringValue();
    }

    private static InputStream indexFileInputStream(final InputStream indexStream, String extension)
            throws IOException {
        if (extension.equals("gz")) {
            return new GZIPInputStream(indexStream);
        } else if (extension.equals(TabixUtils.STANDARD_INDEX_EXTENSION)) {
            return new BlockCompressedInputStream(indexStream);
        } else {
            return indexStream;
        }
    }
}
