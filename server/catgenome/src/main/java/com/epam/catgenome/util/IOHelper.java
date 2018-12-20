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


import com.amazonaws.services.s3.AmazonS3URI;
import com.epam.catgenome.util.aws.S3Client;
import htsjdk.tribble.util.ParsingUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Source:      IOHelper.java
 * Created:     11/25/15, 12:40 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code IOHelper} provides miscellaneous util methods concerned with
 * different I/O activities.
 * </p>
 */
public final class IOHelper {
    /**
     * {@code List} specifies collection of file extensions that indicates that a file
     * with one of these extensions can be considered as compressed resource.
     */
    private static final List<String> GZIP_EXTENSIONS = Collections.unmodifiableList(
        Collections.singletonList(".gz"));

    private IOHelper() {
        // no operations by default
    }

    /**
     * Opens the given {@code File} to read it. If file name indicates that it's compressed,
     * it's decompressed automatically.
     *
     * @param file {@code File} represents a file that should be opened to read
     * @return {@code InputStream}
     * @throws IOException will be thrown if any I/O errors occur
     */
    public static InputStream openStream(final File file) throws IOException {
        InputStream is = new FileInputStream(file);
        if (isGZIPFile(file.getName())) {
            return new GZIPInputStream(is);
        }
        return is;
    }

    /**
     * Returns <tt>true</tt> if and only if the given file name has en extension that indicates that
     * it's a GZIP resource, otherwise returns <tt>false</tt>.
     *
     * @param fn {@code String} represents a file name that should be tested
     * @return boolean
     */
    public static boolean isGZIPFile(final String fn) {
        for (String ext : GZIP_EXTENSIONS) {
            if (fn.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static boolean resourceExists(String resource) throws IOException {
        if(Utils.isS3Source(resource)) {
            return S3Client.getInstance().isFileExisting(resource);
        } else {
            return ParsingUtils.resourceExists(resource);
        }
    }

    public static InputStream openStream(String path) throws IOException {
        if (Utils.isS3Source(path)) {
            return S3Client.getInstance().loadFully(new AmazonS3URI(path));
        } else {
            return ParsingUtils.openInputStream(path);
        }
    }
}
