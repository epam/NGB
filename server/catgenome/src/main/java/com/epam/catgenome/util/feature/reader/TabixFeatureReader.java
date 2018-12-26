package com.epam.catgenome.util.feature.reader;

/*
 * The MIT License
 *
 * Copyright (c) 2013 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
import com.epam.catgenome.util.IOHelper;
import com.epam.catgenome.util.IndexUtils;
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.RuntimeIOException;
import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.CloseableTribbleIterator;
import htsjdk.tribble.Feature;
import htsjdk.tribble.TribbleException;
import htsjdk.tribble.readers.LineReader;
import htsjdk.tribble.readers.LineReaderUtil;
import htsjdk.tribble.readers.PositionalBufferedStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Copied from HTSJDK library. Modified readHeader() method for caching header
 *
 * @author Jim Robinson
 * @since 2/11/12
 */
public class TabixFeatureReader<T extends Feature, S> extends AbstractFeatureReader<T, S> {

    TabixReader tabixReader;
    List<String> sequenceNames;
    String indexFile;

    /**
     *
     * @param featureFile - path to a feature file. Can be a local file, http url, or ftp url
     * @param codec
     * @throws IOException
     */
    public TabixFeatureReader(final String featureFile, final AsciiFeatureCodec codec) throws IOException {
        super(featureFile, codec);
        tabixReader = new TabixReader(featureFile);
        sequenceNames = new ArrayList<>(tabixReader.getChromosomes());
        readHeader();
    }

    /**
     *
     *
     * @param featureFile - path to a feature file. Can be a local file, http url, or ftp url
     * @param indexFile - path to the index file.
     * @param codec
     * @throws IOException
     */
    public TabixFeatureReader(final String featureFile, final String indexFile,
                              final AsciiFeatureCodec codec, EhCacheBasedIndexCache indexCache) throws IOException {
        super(featureFile, codec);

        this.indexCache = indexCache;
        this.indexFile = indexFile;
        tabixReader = new TabixReader(featureFile, indexFile, indexCache);
        sequenceNames = new ArrayList<>(tabixReader.getChromosomes());

        readHeader();
    }

    /**
     * read the header
     *
     * @return a Object, representing the file header, if available
     * @throws IOException throws an IOException if we can't open the file
     */
    private void readHeader() throws IOException {
        S source = null;
        try {
            String indexFilePath;
            if (indexFile == null) {
                indexFilePath = path;
            } else {
                indexFilePath = IndexUtils.getFirstPartForIndexPath(indexFile);
            }
            if (indexCache != null && indexCache.contains(indexFilePath)) {
                TabixReader.TabixIndexCache tabixIndexCache =
                        (TabixReader.TabixIndexCache) indexCache.getFromCache(indexFilePath);
                header = tabixIndexCache.getHeader();

                if (header == null) {
                    InputStream is = IOHelper.openStream(path);
                    source = codec.makeSourceFromStream(new PositionalBufferedStream(
                            new BlockCompressedInputStream(is)));
                    header = codec.readHeader(source);
                    tabixIndexCache.setHeader(header);
                    tabixIndexCache.setCodec(codec);
                    indexCache.putInCache(tabixIndexCache, indexFilePath);
                }
                codec = tabixIndexCache.getCodec();
            } else {
                source = codec.makeSourceFromStream(new PositionalBufferedStream(
                        new BlockCompressedInputStream(IOHelper.openStream(path))));
                header = codec.readHeader(source);
            }
        } catch (IOException e) {
            throw new TribbleException.MalformedFeatureFile("Unable to parse header with error: "
                    + e.getMessage(), path, e);
        } finally {
            if (source != null) {
                codec.close(source);
            }
        }
    }

    @Override
    public boolean hasIndex(){
        return true;
    }

    public List<String> getSequenceNames() {
        return sequenceNames;
    }

    /**
     * Return iterator over all features overlapping the given interval
     *
     * @param chr
     * @param start
     * @param end
     * @return
     * @throws IOException
     */
    public CloseableTribbleIterator<T> query(final String chr, final int start, final int end) throws IOException {
        final List<String> mp = getSequenceNames();
        if (mp == null) {
            throw new TribbleException.TabixReaderFailure("Unable to find sequence named " + chr +
                    " in the tabix index. ", path);
        }
        if (!mp.contains(chr)) {
            return new EmptyIterator<T>();
        }
        final TabixIteratorLineReader lineReader = new TabixIteratorLineReader(
                tabixReader.query(tabixReader.chr2tid(chr), start - 1, end));
        return new FeatureIterator<T>(lineReader, start - 1, end);
    }

    public CloseableTribbleIterator<T> iterator() throws IOException {
        final InputStream is = new BlockCompressedInputStream(IOHelper.openStream(path));
        final PositionalBufferedStream stream = new PositionalBufferedStream(is);
        final LineReader reader = LineReaderUtil.fromBufferedStream(stream,
                LineReaderUtil.LineReaderOption.SYNCHRONOUS);
        return new FeatureIterator<T>(reader, 0, Integer.MAX_VALUE);
    }

    public void close() throws IOException {
        tabixReader.close();
    }

    class FeatureIterator<T extends Feature> implements CloseableTribbleIterator<T> {
        private T currentRecord;
        private LineReader lineReader;
        private int start;
        private int end;

        FeatureIterator(final LineReader lineReader, final int start, final int end) throws IOException {
            this.lineReader = lineReader;
            this.start = start;
            this.end = end;
            readNextRecord();
        }

        /**
         * Advance to the next record in the query interval.
         *
         * @throws IOException
         */
        protected void readNextRecord() throws IOException {
            currentRecord = null;
            String nextLine;
            while (currentRecord == null) {
                nextLine = lineReader.readLine();
                if (nextLine == null) {
                    return;
                }
                try {
                    final Feature f = ((AsciiFeatureCodec)codec).decode(nextLine);
                    if (f == null) {
                        continue;   // Skip
                    }
                    if (f.getStart() > end) {
                        return;    // Done
                    }
                    if (f.getEnd() <= start) {
                        continue;   // Skip
                    }
                    currentRecord = (T) f;
                } catch (TribbleException e) {
                    e.setSource(path);
                    throw e;
                } catch (NumberFormatException e) {
                    String error = "Error parsing line: " + nextLine;
                    throw new TribbleException.MalformedFeatureFile(error, path, e);
                }
            }
        }

        public boolean hasNext() {
            return currentRecord != null;
        }

        public T next() {
            T ret = currentRecord;
            try {
                readNextRecord();
            } catch (IOException e) {
                throw new RuntimeIOException("Unable to read the next record, the last record was at " +
                        ret.getContig() + ":" + ret.getStart() + "-" + ret.getEnd(), e);
            }
            return ret;
        }

        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported in Iterators");
        }

        public void close() {
            lineReader.close();
        }

        public Iterator<T> iterator() {
            return this;
        }
    }
}
