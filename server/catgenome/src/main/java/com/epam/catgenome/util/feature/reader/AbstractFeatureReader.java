package com.epam.catgenome.util.feature.reader;

/*
 * Copyright (c) 2007-2010 by The Broad Institute, Inc. and the Massachusetts Institute of Technology.
 * All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL), Version 2.1 which
 * is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR WARRANTIES OF
 * ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING, WITHOUT LIMITATION, WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT
 * OR OTHER DEFECTS, WHETHER OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR
 * RESPECTIVE TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES OF
 * ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES, ECONOMIC
 * DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER THE BROAD OR MIT SHALL
 * BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT SHALL KNOW OF THE POSSIBILITY OF THE
 * FOREGOING.
 */

import htsjdk.tribble.*;
import htsjdk.tribble.index.Index;
import htsjdk.tribble.util.ParsingUtils;
import htsjdk.tribble.util.TabixUtils;

import java.io.File;
import java.io.IOException;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Copied from HTSJDK library. Added: indexCache in getFeatureReader() method
 *
 * jrobinso
 * <p/>
 * the feature reader class, which uses indices and codecs to read in Tribble file formats.
 */
public abstract class AbstractFeatureReader<T extends Feature, S> implements FeatureReader<T> {
    // the logging destination for this source
    //private final static Logger log = Logger.getLogger("BasicFeatureSource");

    // the path to underlying data source
    String path;

    // the query source, codec, and header
    // protected final QuerySource querySource;
    protected FeatureCodec<T, S> codec;
    protected FeatureCodecHeader header;
    EhCacheBasedIndexCache indexCache;

    private static AbstractFeatureReader.ComponentMethods methods = new AbstractFeatureReader.ComponentMethods();

    public static final Set<String> BLOCK_COMPRESSED_EXTENSIONS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(".gz", ".gzip", ".bgz", ".bgzf")));

    /**
     * Calls {@link #getFeatureReader(String, FeatureCodec, boolean, EhCacheBasedIndexCache)} with {@code requireIndex} = true
     */
    public static <F extends Feature, S> AbstractFeatureReader<F, S> getFeatureReader(
            final String featureFile, final FeatureCodec<F, S> codec,
            EhCacheBasedIndexCache indexCache) throws TribbleException {
        return getFeatureReader(featureFile, codec, true, indexCache);
    }

    /**
     * {@link #getFeatureReader(String, String, FeatureCodec, boolean, EhCacheBasedIndexCache)} with {@code null} for indexResource
     * @throws TribbleException
     */
    public static <F extends Feature, S> AbstractFeatureReader<F, S> getFeatureReader(
            final String featureResource, final FeatureCodec<F, S> codec,
            final boolean requireIndex, EhCacheBasedIndexCache indexCache) throws TribbleException {
        return getFeatureReader(featureResource, null, codec, requireIndex, indexCache);
    }

    /**
     *
     * @param featureResource the feature file to create from
     * @param indexResource   the index for the feature file. If null, will auto-generate (if necessary)
     * @param codec
     * @param requireIndex    whether an index is required for this file
     * @return
     * @throws TribbleException
     */
    public static <F extends Feature, S> AbstractFeatureReader<F, S> getFeatureReader(
            final String featureResource, String indexResource, final FeatureCodec<F, S> codec,
            final boolean requireIndex, EhCacheBasedIndexCache indexCache) throws TribbleException {

        try {
            // Test for tabix index
            if (methods.isTabix(featureResource, indexResource)) {
                if (!(codec instanceof AsciiFeatureCodec)) {
                    throw new TribbleException(
                            "Tabix indexed files only work with ASCII codecs, but received non-Ascii codec "
                                    + codec.getClass().getSimpleName());
                }
                return new TabixFeatureReader<F, S>(featureResource, indexResource,
                        (AsciiFeatureCodec) codec, indexCache);
            }
            // Not tabix => tribble index file (might be gzipped, but not block gzipped)
            else {
                return new TribbleIndexedFeatureReader<F, S>(featureResource, indexResource, codec,
                        requireIndex, indexCache);
            }
        } catch (IOException e) {
            throw new TribbleException.MalformedFeatureFile(
                    "Unable to create BasicFeatureReader using feature file ", featureResource, e);
        } catch (TribbleException e) {
            e.setSource(featureResource);
            throw e;
        }
    }

    /**
     * Return a reader with a supplied index.
     *
     * @param featureResource the path to the source file containing the features
     * @param codec used to decode the features
     * @param index index of featureResource
     * @return a reader for this data
     * @throws TribbleException
     */
    public static <F extends Feature, S> AbstractFeatureReader<F, S> getFeatureReader(
            final String featureResource, final FeatureCodec<F, S>  codec, final Index index,
            EhCacheBasedIndexCache indexCache) throws TribbleException {
        try {
            return new TribbleIndexedFeatureReader<F, S>(featureResource, codec, index, indexCache);
        } catch (IOException e) {
            throw new TribbleException.MalformedFeatureFile(
                    "Unable to create AbstractFeatureReader using feature file ", featureResource, e);
        }

    }

    protected AbstractFeatureReader(final String path, final FeatureCodec<T, S> codec) {
        this.path = path;
        this.codec = codec;
    }

    /**
     * Whether the reader has an index or not
     * Default implementation returns false
     * @return
     */
    public boolean hasIndex(){
        return false;
    }

    public static void setComponentMethods(AbstractFeatureReader.ComponentMethods methods){
        AbstractFeatureReader.methods = methods;
    }

    /**
     * Whether a filename ends in one of the BLOCK_COMPRESSED_EXTENSIONS
     * @param fileName
     * @return
     */
    public static boolean hasBlockCompressedExtension (final String fileName) {
        for (final String extension : BLOCK_COMPRESSED_EXTENSIONS) {
            if (fileName.toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the name of a file ends in one of the BLOCK_COMPRESSED_EXTENSIONS
     * @param file
     * @return
     */
    public static boolean hasBlockCompressedExtension (final File file) {
        return hasBlockCompressedExtension(file.getName());
    }

    /**
     * Whether the path of a URI resource ends in one of the BLOCK_COMPRESSED_EXTENSIONS
     * @param uri a URI representing the resource to check
     * @return
     */
    public static boolean hasBlockCompressedExtension (final URI uri) {
        return hasBlockCompressedExtension(uri.getPath());
    }
    /**
     * get the header
     *
     * @return the header object we've read-in
     */
    public Object getHeader() {
        return header.getHeaderValue();
    }

    static class EmptyIterator<T extends Feature> implements CloseableTribbleIterator<T> {
        public Iterator iterator() { return this; }
        public boolean hasNext() { return false; }
        public T next() { return null; }
        public void remove() { }
        @Override public void close() { }
    }

    public static boolean isTabix(String resourcePath, String indexPath) throws IOException {
        if(indexPath == null){
            indexPath = ParsingUtils.appendToPath(resourcePath, TabixUtils.STANDARD_INDEX_EXTENSION);
        }
        return hasBlockCompressedExtension(resourcePath) && ParsingUtils.resourceExists(indexPath);
    }

    public static class ComponentMethods{

        public boolean isTabix(String resourcePath, String indexPath) throws IOException{
            return AbstractFeatureReader.isTabix(resourcePath, indexPath);
        }
    }
}
