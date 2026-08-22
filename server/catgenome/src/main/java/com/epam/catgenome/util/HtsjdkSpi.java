/*
 * MIT License
 *
 * Copyright (c) 2026 EPAM Systems
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

import java.io.IOException;

import com.epam.catgenome.util.feature.reader.EnhancedUrlHelper;
import htsjdk.samtools.seekablestream.SeekableStreamFactory;
import htsjdk.samtools.util.FileExtensions;
import htsjdk.samtools.util.IOUtil;
import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.util.ParsingUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Installs NGB's three htsjdk service-provider hooks. All three are process-global statics inside
 * htsjdk, so this happens once, at context refresh, before any track can be requested.
 *
 * <p>The same three concerns used to be spread over a fork of four htsjdk reader classes in
 * {@code util.feature.reader}. The fork is gone; what it did that stock htsjdk cannot is now
 * expressed through the SPIs htsjdk publishes for exactly this purpose:
 *
 * <ol>
 *   <li>{@link ParsingUtils#setURLHelperFactory} with {@link EnhancedUrlHelper} — so a pre-signed
 *       S3 URL, which answers {@code HEAD} with 403, is still treated as existing. The old code
 *       called {@code ParsingUtils.registerHelperClass(EnhancedUrlHelper.class)} on every reader
 *       open; that method was removed in htsjdk 4, and a factory registered once is what replaces
 *       it.</li>
 *   <li>{@link AbstractFeatureReader#setComponentMethods} with {@link NgbComponentMethods} — so
 *       tabix detection works for NGB's cloud schemes ({@code s3://}, {@code sws://}, {@code
 *       az://}). Stock {@code isTabix} asks {@code ParsingUtils.resourceExists}, which knows only
 *       local files, http/ftp and NIO filesystems.</li>
 *   <li>{@link SeekableStreamFactory#setInstance} with {@link NgbSeekableStreamFactory} — so
 *       ranged reads of cloud objects work. That factory installs itself from a static
 *       initialiser, which means it was previously installed as a side effect of {@code
 *       Application}'s {@code ngbSeekableStreamFactory()} bean being created — and therefore not
 *       at all in the XML-driven test contexts. Touching it here makes the installation explicit
 *       and flavour-independent; the bean is still declared, and doing it twice is harmless.</li>
 * </ol>
 */
@Component
public class HtsjdkSpi {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtsjdkSpi.class);

    private static boolean installed;

    /**
     * Registers the hooks. Idempotent, so unit tests that need them without a Spring context can
     * call it directly.
     */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        ParsingUtils.setURLHelperFactory(EnhancedUrlHelper::new);
        AbstractFeatureReader.setComponentMethods(new NgbComponentMethods());
        SeekableStreamFactory.setInstance(NgbSeekableStreamFactory.getInstance());
        installed = true;
        LOGGER.debug("htsjdk SPI hooks installed: URLHelperFactory, ComponentMethods, "
                + "ISeekableStreamFactory");
    }

    @PostConstruct
    public void registerHooks() {
        install();
    }

    /**
     * Stock {@link AbstractFeatureReader#isTabix} asked {@code ParsingUtils.resourceExists}, which
     * knows local files, http/ftp and NIO filesystems — not {@code s3://}, {@code sws://} or
     * {@code az://}. This asks {@link IOHelper#resourceExists} instead, and that is the only
     * difference: the block-compressed test is stock, because
     * {@link IOUtil#hasBlockCompressedExtension(String)} already strips the query string of an
     * http(s) URL, which is the reason NGB used to carry its own copy.
     */
    public static class NgbComponentMethods extends AbstractFeatureReader.ComponentMethods {

        @Override
        public boolean isTabix(final String resourcePath, final String indexPath) throws IOException {
            final String index = indexPath == null
                    ? ParsingUtils.appendToPath(resourcePath, FileExtensions.TABIX_INDEX)
                    : indexPath;
            return IOUtil.hasBlockCompressedExtension(resourcePath) && IOHelper.resourceExists(index);
        }
    }
}
