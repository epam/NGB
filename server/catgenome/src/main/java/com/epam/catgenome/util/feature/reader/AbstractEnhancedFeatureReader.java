/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.catgenome.util.feature.reader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.Feature;
import htsjdk.tribble.FeatureCodec;
import htsjdk.tribble.TribbleException;
import htsjdk.tribble.index.Index;
import htsjdk.tribble.util.ParsingUtils;
import htsjdk.tribble.util.TabixUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Feature reader extended from HTSJDK library ro support signed S3 URLs
 * @param <T>
 * @param <S>
 */
public abstract class AbstractEnhancedFeatureReader<T extends Feature, S> extends AbstractFeatureReader<T, S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEnhancedFeatureReader.class);

    protected AbstractEnhancedFeatureReader(String path, FeatureCodec<T, S> codec) {
        super(path, codec);
    }

    private static ComponentMethods methods = new ComponentMethods();

    /**
     * Calls {@link #getFeatureReader(String, FeatureCodec, boolean, EhCacheBasedIndexCache)}
     * with {@code requireIndex} = true
     */
    public static <FEATURE extends Feature, SOURCE> AbstractFeatureReader<FEATURE, SOURCE> getFeatureReader(
            final String featureFile, final FeatureCodec<FEATURE, SOURCE> codec,
            EhCacheBasedIndexCache indexCache) throws TribbleException {
        return getFeatureReader(featureFile, codec, true, indexCache);
    }

    /**
     * {@link #getFeatureReader(String, String, FeatureCodec, boolean, EhCacheBasedIndexCache)}
     * with {@code null} for indexResource
     * @throws TribbleException
     */
    public static <FEATURE extends Feature, SOURCE> AbstractFeatureReader<FEATURE, SOURCE> getFeatureReader(
            final String featureResource, final FeatureCodec<FEATURE, SOURCE> codec,
            final boolean requireIndex, EhCacheBasedIndexCache indexCache)
            throws TribbleException {
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
    public static <FEATURE extends Feature, SOURCE> AbstractFeatureReader<FEATURE, SOURCE> getFeatureReader(
            final String featureResource, String indexResource,
            final FeatureCodec<FEATURE, SOURCE> codec, final boolean requireIndex,
            EhCacheBasedIndexCache indexCache) throws TribbleException {
        ParsingUtils.registerHelperClass(EnhancedUrlHelper.class);
        try {
            // Test for tabix index
            if (methods.isTabix(featureResource, indexResource)) {
                if (!(codec instanceof AsciiFeatureCodec)) {
                    throw new TribbleException("Tabix indexed files only work with ASCII codecs, "
                            + "but received non-Ascii codec " + codec.getClass().getSimpleName());
                }
                return new TabixFeatureReader<>(featureResource, indexResource,
                        (AsciiFeatureCodec) codec, indexCache);
            } else {
                // Not tabix => tribble index file (might be gzipped, but not block gzipped)
                return new TribbleIndexedFeatureReader<>(featureResource, indexResource,
                        codec, requireIndex, indexCache);
            }
        } catch (IOException e) {
            throw new TribbleException.MalformedFeatureFile("Unable to create"
                    + "BasicFeatureReader using feature file ", featureResource, e);
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
    public static <FEATURE extends Feature, SOURCE> AbstractFeatureReader<FEATURE, SOURCE> getFeatureReader(
            final String featureResource, final FeatureCodec<FEATURE, SOURCE>  codec, final Index index,
            EhCacheBasedIndexCache indexCache)
            throws TribbleException {
        try {
            return new TribbleIndexedFeatureReader<>(featureResource, codec, index, indexCache);
        } catch (IOException e) {
            throw new TribbleException.MalformedFeatureFile("Unable to create "
                    + "AbstractFeatureReader using feature file ", featureResource, e);
        }

    }
    /**
     * Whether a filename ends in one of the BLOCK_COMPRESSED_EXTENSIONS
     * @param fileName
     * @return
     */
    public static boolean hasBlockCompressedExtension(final String fileName) {
        if (isBlockCompressed(fileName)) {
            return true;
        }
        try {
            URL url = new URL(fileName);
            return isBlockCompressed(url.getPath());
        } catch (MalformedURLException e) {
            LOGGER.trace(e.getMessage(), e);
        }
        return false;
    }

    private static boolean isBlockCompressed(String path) {
        for (final String extension : BLOCK_COMPRESSED_EXTENSIONS) {
            if (path.toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    public static class ComponentMethods{

        public boolean isTabix(String resourcePath, String indexPath) throws IOException{
            if(indexPath == null){
                indexPath = ParsingUtils.appendToPath(resourcePath, TabixUtils.STANDARD_INDEX_EXTENSION);
            }
            return hasBlockCompressedExtension(resourcePath) && ParsingUtils.resourceExists(indexPath);
        }
    }
}
