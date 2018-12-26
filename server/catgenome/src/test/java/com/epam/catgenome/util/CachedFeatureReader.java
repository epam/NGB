package com.epam.catgenome.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import htsjdk.tribble.CloseableTribbleIterator;
import htsjdk.tribble.Feature;
import htsjdk.tribble.FeatureCodec;

/**
 * An implementation of HTSJDK's {@link AbstractFeatureReader}, that is backed by a list of {@link Feature} entities.
 * <p>For test usage only
 */
public class CachedFeatureReader<T extends Feature, S> extends AbstractFeatureReader<T, S> {
    private boolean closed = false;
    private List<T> cachedFeatures;

    public CachedFeatureReader(List<T> cachedFeatures, FeatureCodec<T, S> codec) {
        super("", codec);
        this.cachedFeatures = cachedFeatures;
    }

    @Override
    public CloseableTribbleIterator<T> query(String chr, int start, int end) throws IOException {
        if (closed) {
            throw new IOException("Reader already closed");
        }

        return new CachedFeatureIterator<T>(cachedFeatures.stream()
                                                .filter(f -> f.getContig().equals(chr) &&
                                                             (f.getStart() <= end && f.getEnd() >= start))
                                                .collect(Collectors.toList()));
    }

    @Override
    public CloseableTribbleIterator<T> iterator() throws IOException {
        if (closed) {
            throw new IOException("Reader already closed");
        }

        return new CachedFeatureIterator<>(cachedFeatures);
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }

    @Override
    public List<String> getSequenceNames() {
        return null;
    }

    @Override
    public Object getHeader() {
        return null;
    }

    public void reOpen() {
        closed = false;
    }

    private final class CachedFeatureIterator<T extends Feature> implements CloseableTribbleIterator<T> {
        private Iterator<T> cachedListIterator;

        private CachedFeatureIterator(List<T> cachedFeatures) {
            this.cachedListIterator = cachedFeatures.iterator();
        }

        @Override
        public void close() {
        }

        @Override
        public Iterator<T> iterator() {
            return cachedListIterator;
        }

        @Override
        public boolean hasNext() {
            return cachedListIterator.hasNext();
        }

        @Override
        public T next() {
            return cachedListIterator.next();
        }
    }
}
