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

import com.epam.catgenome.util.IndexUtils;
import htsjdk.samtools.seekablestream.ISeekableStreamFactory;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.seekablestream.SeekableStreamFactory;
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.tribble.Feature;
import htsjdk.tribble.FeatureCodec;
import htsjdk.tribble.FeatureCodecHeader;
import htsjdk.tribble.TribbleException;
import htsjdk.tribble.util.ParsingUtils;
import htsjdk.tribble.util.TabixUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Copied from HTSJDK library. Added class TabixIndexCache for saving cache values.
 * Modified readIndex() method for caching index
 * @author Heng Li <hengli@broadinstitute.org>
 */
public class TabixReader {
    private EhCacheBasedIndexCache indexCache;
    private String mFn;
    private String mIdxFn;
    private BlockCompressedInputStream mFp;

    private int mPreset;
    private int mSc;
    private int mBc;
    private int mEc;
    private int mMeta;
    //private int mSkip; (not used)
    private String[] mSeq;

    private Map<String, Integer> mChr2tid;

    private static final int MAX_BIN = 37450;
    //private static int TAD_MIN_CHUNK_GAP = 32768; (not used)
    private static final int TAD_LIDX_SHIFT = 14;

    protected static class TPair64 implements Comparable<TPair64> {
        long u, v;

        public TPair64(final long longU, final long longV) {
            u = longU;
            v = longV;
        }

        public TPair64(final TPair64 p) {
            u = p.u;
            v = p.v;
        }

        public int compareTo(final TPair64 p) {
            return u == p.u ? 0 : ((u < p.u) ^ (u < 0) ^ (p.u < 0)) ? -1 : 1; // unsigned 64-bit comparison
        }
    }

    protected static class TIndex {
        HashMap<Integer, TPair64[]> b; // binning index
        long[] l; // linear index
    }

    protected TIndex[] mIndex;

    protected static class TabixIndexCache<T extends Feature, S> implements IndexCache {
        private TIndex[] mIndex;
        private String[] mSeq;
        private Map<String, Integer> mChr2tid;
        private int mPreset;
        private int mSc;
        private int mBc;
        private int mEc;
        private int mMeta;
        private FeatureCodecHeader header;
        private FeatureCodec <T, S> codec;

        public FeatureCodecHeader getHeader() {
            return header;
        }

        public void setHeader(FeatureCodecHeader header) {
            this.header = header;
        }

        public FeatureCodec<T, S> getCodec() {
            return codec;
        }

        public void setCodec(FeatureCodec<T, S> codec) {
            this.codec = codec;
        }
    }

    protected TabixIndexCache tabixIndexCache;

    private static class TIntv {
        int tid, beg, end;
    }

    private static boolean less64(final long u, final long v) { // unsigned 64-bit comparison
        return (u < v) ^ (u < 0) ^ (v < 0);
    }

    /**
     * @param fn File name of the data file
     */
    public TabixReader(final String fn) throws IOException {
        this(fn, null, SeekableStreamFactory.getInstance().getBufferedStream(
                SeekableStreamFactory.getInstance().getStreamFor(fn)), null);
    }

    /**
     * @param fn File name of the data file
     * @param idxFn Full path to the index file. Auto-generated if null
     */
    public TabixReader(final String fn, final String idxFn, EhCacheBasedIndexCache indexCache) throws IOException {
        this(fn, idxFn, SeekableStreamFactory.getInstance().getBufferedStream(
                SeekableStreamFactory.getInstance().getStreamFor(fn)), indexCache);
    }

    /**
     * @param fn File name of the data file  (used for error messages only)
     * @param stream Seekable stream from which the data is read
     */
    public TabixReader(final String fn, SeekableStream stream, EhCacheBasedIndexCache indexCache) throws IOException {
        this(fn, null, stream, indexCache);
    }

    /**
     * @param fn File name of the data file  (used for error messages only)
     * @param idxFn Full path to the index file. Auto-generated if null
     * @param stream Seekable stream from which the data is read
     */
    public TabixReader(final String fn, final String idxFn, SeekableStream stream,
                       EhCacheBasedIndexCache indexCache) throws IOException {
        mFn = fn;
        mFp = new BlockCompressedInputStream(stream);
        if (idxFn == null) {
            mIdxFn = ParsingUtils.appendToPath(fn, TabixUtils.STANDARD_INDEX_EXTENSION);
        } else {
            mIdxFn = idxFn;
        }
        this.indexCache = indexCache;
        readIndex();
    }

    /** return the source (filename/URL) of that reader */
    public String getSource() {
        return this.mFn;
    }

    private static int reg2bins(final int beg, final int intEnd, final int[] list) {
        int i = 0, k, end = intEnd;
        if (beg >= end) {
            return 0;
        }
        final int shift0 = 29;
        if (end >= 1 << shift0) {
            end = 1 << shift0;
        }
        --end;
        list[i++] = 0;
        final int shift1 = 26;
        for (k = 1 + (beg >> shift1); k <= 1 + (end >> shift1); ++k) {
            list[i++] = k;
        }
        final int shift2 = 23;
        final int startShift2 = 9;
        for (k = startShift2 + (beg >> shift2); k <= startShift2 + (end >> shift2); ++k) {
            list[i++] = k;
        }
        final int shift3 = 20;
        final int startShift3 = 73;
        for (k = startShift3 + (beg >> shift3); k <= startShift3 + (end >> shift3); ++k) {
            list[i++] = k;
        }
        final int shift4 = 17;
        final int startShift4 = 585;
        for (k = startShift4 + (beg >> shift4); k <= startShift4 + (end >> shift4); ++k) {
            list[i++] = k;
        }
        final int shift5 = 14;
        final int startShift5 = 4681;
        for (k = startShift5 + (beg >> shift5); k <= startShift5 + (end >> shift5); ++k) {
            list[i++] = k;
        }
        return i;
    }

    public static int readInt(final InputStream is) throws IOException {
        byte[] buf = new byte[4];
        is.read(buf);
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static long readLong(final InputStream is) throws IOException {
        byte[] buf = new byte[8];
        is.read(buf);
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    public static String readLine(final InputStream is) throws IOException {
        StringBuffer buf = new StringBuffer();
        int c = is.read();
        while (c >= 0 && c != '\n') {
            buf.append((char) c);
            c = is.read();
        }
        if (c < 0) {
            return null;
        }
        return buf.toString();
    }

    /**
     * Read the Tabix index from a file
     *
     * @param fp File pointer
     */
    private void readIndex(SeekableStream fp) throws IOException {
        if (fp == null) {
            return;
        }
        String indexFilePath = IndexUtils.getFirstPartForIndexPath(mIdxFn);

        // read the index cache
        if (indexCache != null && indexCache.contains(indexFilePath)) {
            tabixIndexCache = (TabixIndexCache) indexCache.getFromCache(indexFilePath);
            mIndex = tabixIndexCache.mIndex;
            mBc = tabixIndexCache.mBc;
            mChr2tid = tabixIndexCache.mChr2tid;
            mEc = tabixIndexCache.mEc;
            mMeta = tabixIndexCache.mMeta;
            mPreset = tabixIndexCache.mPreset;
            mSc = tabixIndexCache.mSc;
            mSeq = tabixIndexCache.mSeq;
        } else {
            //create index and save to cache
            BlockCompressedInputStream is = new BlockCompressedInputStream(fp);
            byte[] buf = new byte[4];

            is.read(buf, 0, 4); // read "TBI\1"
            mSeq = new String[readInt(is)]; // # sequences
            mChr2tid = new HashMap<String, Integer>();
            mPreset = readInt(is);
            mSc = readInt(is);
            mBc = readInt(is);
            mEc = readInt(is);
            mMeta = readInt(is);
            readInt(is); //unused
            // read sequence dictionary
            int l = readInt(is);
            int i;
            int j;
            int k;
            buf = new byte[l];
            is.read(buf);

            for (i = 0, j = 0, k = 0; i < buf.length; ++i) {
                if (buf[i] == 0) {
                    byte[] b = new byte[i - j];
                    System.arraycopy(buf, j, b, 0, b.length);
                    String s = new String(b);
                    mChr2tid.put(s, k);
                    mSeq[k++] = s;
                    j = i + 1;
                }
            }

            mIndex = new TIndex[mSeq.length];

            for (i = 0; i < mSeq.length; ++i) {
                // the binning index
                int nBin = readInt(is);
                mIndex[i] = new TIndex();
                mIndex[i].b = new HashMap<Integer, TPair64[]>(nBin);
                for (j = 0; j < nBin; ++j) {
                    int bin = readInt(is);
                    TPair64[] chunks = new TPair64[readInt(is)];
                    for (k = 0; k < chunks.length; ++k) {
                        long u = readLong(is);
                        long v = readLong(is);
                        chunks[k] = new TPair64(u, v); // in C, this is inefficient
                    }
                    mIndex[i].b.put(bin, chunks);
                }
                // the linear index
                mIndex[i].l = new long[readInt(is)];
                for (k = 0; k < mIndex[i].l.length; ++k) {
                    mIndex[i].l[k] = readLong(is);
                }
            }
            if (indexCache != null) {
                tabixIndexCache = new TabixIndexCache();
                tabixIndexCache.mIndex = mIndex;
                tabixIndexCache.mBc = mBc;
                tabixIndexCache.mChr2tid = mChr2tid;
                tabixIndexCache.mEc = mEc;
                tabixIndexCache.mMeta = mMeta;
                tabixIndexCache.mPreset = mPreset;
                tabixIndexCache.mSc = mSc;
                tabixIndexCache.mSeq = mSeq;

                indexCache.putInCache(tabixIndexCache, indexFilePath);
            }
            // close
            is.close();
        }
    }

    /**
     * Read the Tabix index from the default file.
     */
    private void readIndex() throws IOException {
        ISeekableStreamFactory ssf = SeekableStreamFactory.getInstance();
        final int bufferSize = 128000;
        SeekableStream bufferedStream = ssf.getBufferedStream(ssf.getStreamFor(mIdxFn), bufferSize);
        readIndex(bufferedStream);
    }

    /**
     * Read one line from the data file.
     */
    public String readLine() throws IOException {
        return readLine(mFp);
    }

    /** return chromosome ID or -1 if it is unknown */
    public int chr2tid(final String chr) {
        Integer tid = this.mChr2tid.get(chr);
        return tid == null ? -1 : tid;
    }

    /** return the chromosomes in that tabix file */
    public Set<String> getChromosomes() {
        return Collections.unmodifiableSet(this.mChr2tid.keySet());
    }

    /**
     * Parse a region in the format of "chr1", "chr1:100" or "chr1:100-1000"
     *
     * @param reg Region string
     * @return An array where the three elements are sequence_id,
     *         region_begin and region_end. On failure, sequence_id==-1.
     */
    public int[] parseReg(final String reg) { // FIXME: NOT working when the sequence name contains : or -.
        String chr;
        int colon, hyphen;
        int[] ret = new int[3];
        colon = reg.indexOf(':');
        hyphen = reg.indexOf('-');
        chr = colon >= 0 ? reg.substring(0, colon) : reg;
        ret[1] = colon >= 0 ? Integer.parseInt(reg.substring(colon + 1, hyphen >= 0 ? hyphen : reg.length())) - 1 : 0;
        final int maxIntInHex = 0x7fffffff;
        ret[2] = hyphen >= 0 ? Integer.parseInt(reg.substring(hyphen + 1)) : maxIntInHex;
        ret[0] = this.chr2tid(chr);
        return ret;
    }

    private TIntv getIntv(final String s) {
        TIntv intv = new TIntv();
        int col = 0;
        int beg = 0;
        int end = s.indexOf('\t', beg);
        while (end >= 0 || end == -1) {
            ++col;
            if (col == mSc) {
                intv.tid = chr2tid(end != -1 ? s.substring(beg, end) : s.substring(beg));
            } else if (col == mBc) {
                intv.end = Integer.parseInt(end != -1 ? s.substring(beg, end) : s.substring(beg));
                intv.beg = intv.end;
                final int magicNumber = 0x10000;
                if ((mPreset & magicNumber) != 0) {
                    ++intv.end;
                } else {
                    --intv.beg;
                }
                if (intv.beg < 0) {
                    intv.beg = 0;
                }
                if (intv.end < 1) {
                    intv.end = 1;
                }
            } else { // FIXME: SAM supports are not tested yet
                final int bitFlag = 0xffff;
                if ((mPreset & bitFlag) == 0) { // generic
                    if (col == mEc) {
                        intv.end = Integer.parseInt(end != -1 ? s.substring(beg, end) : s.substring(beg));
                    }
                } else if ((mPreset & bitFlag) == 1) { // SAM
                    if (col == 6) { // CIGAR
                        int l = 0, i, j;
                        String cigar = s.substring(beg, end);
                        for (i = 0, j = 0; i < cigar.length(); ++i) {
                            if (cigar.charAt(i) > '9') {
                                int op = cigar.charAt(i);
                                if (op == 'M' || op == 'D' || op == 'N') {
                                    l += Integer.parseInt(cigar.substring(j, i));
                                }
                                j = i + 1;
                            }
                        }
                        intv.end = intv.beg + l;
                    }
                } else if ((mPreset & bitFlag) == 2) { // VCF
                    String alt;
                    alt = end >= 0 ? s.substring(beg, end) : s.substring(beg);
                    if (col == 4) { // REF
                        if (!alt.isEmpty()) {
                            intv.end = intv.beg + alt.length();
                        }
                    } else if (col == 8) { // INFO
                        int eOff = -1, i = alt.indexOf("END=");
                        if (i == 0) {
                            eOff = 4;
                        } else if (i > 0) {
                            i = alt.indexOf(";END=");
                            if (i >= 0) {
                                eOff = i + 5;
                            }
                        }
                        if (eOff > 0) {
                            i = alt.indexOf(';', eOff);
                            intv.end = Integer.parseInt(i > eOff ? alt.substring(eOff, i) : alt.substring(eOff));
                        }
                    }
                }
            }
            if (end == -1) {
                break;
            }
            beg = end + 1;
            end = s.indexOf('\t', beg);
        }
        return intv;
    }

    public interface Iterator {
        /** return null when there is no more data to read */
        String next() throws IOException;
    }

    /** iterator returned instead of null when there is no more data */
    private static final Iterator EOF_ITERATOR = new Iterator()  {
        @Override
        public String next() throws IOException {
            return null;
        }
    };

    /** default implementation of Iterator */
    private final class IteratorImpl implements Iterator {
        private int i;
        //private int n_seeks;
        private int tid, beg, end;
        private TPair64[] off;
        private long currOff;
        private boolean iseof;

        private IteratorImpl(final int intTid, final int intBeg, final int intEnd, final TPair64[] tPairOff) {
            i = -1;
            //n_seeks = 0;
            currOff = 0;
            iseof = false;
            off = tPairOff;
            tid = intTid;
            beg = intBeg;
            end = intEnd;
        }

        @Override
        public String next() throws IOException {
            if (iseof) {
                return null;
            }
            for (;;) {
                if (currOff == 0 || !less64(currOff, off[i].v)) { // then jump to the next chunk
                    if (i == off.length - 1) {
                        break; // no more chunks
                    }
                    if (i >= 0) {
                        assert (currOff == off[i].v); // otherwise bug
                    }
                    if (i < 0 || off[i].v != off[i + 1].u) { // not adjacent chunks; then seek
                        mFp.seek(off[i + 1].u);
                        currOff = mFp.getFilePointer();
                        //++n_seeks;
                    }
                    ++i;
                }
                String s = readLine(mFp);
                if (s != null) {
                    TIntv intv;
                    char[] str = s.toCharArray();
                    currOff = mFp.getFilePointer();
                    if (str.length == 0 || str[0] == mMeta) {
                        continue;
                    }
                    intv = getIntv(s);
                    if (intv.tid != tid || intv.beg >= end) {
                        break; // no need to proceed
                    } else if (intv.end > beg && intv.beg < end) {
                        return s; // overlap; return
                    }
                } else {
                    break; // end of file
                }
            }
            iseof = true;
            return null;
        }
    }

    /**
     * Return
     * @param tid Sequence id
     * @param beg beginning of interval, genomic coords
     * @param end end of interval, genomic coords
     * @return an iterator over the lines within the specified interval
     */
    public Iterator query(final int tid, final int beg, final int end) {
        TPair64[] off, chunks;
        long minOff;
        if (tid < 0 || tid >= this.mIndex.length) {
            return EOF_ITERATOR;
        }
        TIndex idx = mIndex[tid];
        int[] bins = new int[MAX_BIN];
        int i, l, nOff, nBins = reg2bins(beg, end, bins);

        if (idx.l.length > 0) {
            minOff = (beg >> TAD_LIDX_SHIFT >= idx.l.length) ? idx.l[idx.l.length - 1] : idx.l[beg >> TAD_LIDX_SHIFT];
        } else {
            minOff = 0;
        }
        for (i = 0, nOff = 0; i < nBins; ++i) {
            chunks = idx.b.get(bins[i]);
            if (chunks != null) {
                nOff += chunks.length;
            }
        }
        if (nOff == 0) {
            return EOF_ITERATOR;
        }
        off = new TPair64[nOff];
        for (i = 0, nOff = 0; i < nBins; ++i) {
            chunks = idx.b.get(bins[i]);
            if (chunks != null) {
                for (int j = 0; j < chunks.length; ++j) {
                    if (less64(minOff, chunks[j].v)) {
                        off[nOff++] = new TPair64(chunks[j]);
                    }
                }
            }
        }
        Arrays.sort(off, 0, nOff);
        // resolve completely contained adjacent blocks
        for (i = 1, l = 0; i < nOff; ++i) {
            if (less64(off[l].v, off[i].v)) {
                ++l;
                off[l].u = off[i].u;
                off[l].v = off[i].v;
            }
        }
        nOff = l + 1;
        // resolve overlaps between adjacent blocks; this may happen due to the merge in indexing
        for (i = 1; i < nOff; ++i) {
            if (!less64(off[i - 1].v, off[i].u)) {
                off[i - 1].v = off[i].u;
            }
        }
        // merge adjacent blocks
        for (i = 1, l = 0; i < nOff; ++i) {
            if (off[l].v >> 16 == off[i].u >> 16) {
                off[l].v = off[i].v;
            } else {
                ++l;
                off[l].u = off[i].u;
                off[l].v = off[i].v;
            }
        }
        nOff = l + 1;
        // return
        TPair64[] ret = new TPair64[nOff];
        for (i = 0; i < nOff; ++i) {
            if (off[i] != null) {
                ret[i] = new TPair64(off[i].u, off[i].v); // in C, this is inefficient
            }
        }
        if (ret.length == 0 || (ret.length == 1 && ret[0] == null)) {
            return EOF_ITERATOR;
        }
        return new TabixReader.IteratorImpl(tid, beg, end, ret);
    }

    /**
     *
     * @see #parseReg(String)
     * @param reg A region string of the form acceptable by {@link #parseReg(String)}
     * @return
     */
    public Iterator query(final String reg) {
        int[] x = parseReg(reg);
        if (x[0] < 0) {
            return EOF_ITERATOR;
        }
        return query(x[0], x[1], x[2]);
    }

    /**
     *
     * @see #parseReg(String)
     * @param reg a chromosome
     * @param start start interval
     * @param end end interval
     * @return a tabix iterator
     */
    public Iterator query(final String reg, int start, int end) {
        int tid = this.chr2tid(reg);
        if (tid == -1) {
            return EOF_ITERATOR;
        }
        return query(tid, start, end);
    }
//
//    public static void main(String[] args) {
//        if (args.length < 1) {
//            System.out.println("Usage: java -cp .:sam.jar TabixReader <in.gz> [region]");
//            System.exit(1);
//        }
//        try {
//            TabixReader tr = new TabixReader(args[0], indexCache);
//            String s;
//            if (args.length == 1) { // no region is specified; print the whole file
//                while ((s = tr.readLine()) != null)
//                    System.out.println(s);
//            } else { // a region is specified; random access
//                TabixReader.Iterator iter = tr.query(args[1]); // get the iterator
//                while ((s = iter.next()) != null)
//                    System.out.println(s);
//            }
//        } catch (IOException e) {
//        }
//    }

    // ADDED BY JTR
    public void close() throws IOException {
        if (mFp != null) {
            try {
                mFp.close();
            } catch (IOException e) {
                throw new TribbleException("Error while closing TabixReader.");
            }
        }
    }

    @Override
    public String toString() {
        return "TabixReader: filename:"+getSource();
    }
}
