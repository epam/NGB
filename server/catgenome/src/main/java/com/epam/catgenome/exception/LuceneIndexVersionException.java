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

package com.epam.catgenome.exception;

import java.io.IOException;

/**
 * Thrown when a Lucene index on disk was written by a Lucene too old (or too new) for the
 * version this server is linked against, instead of letting Lucene's own
 * {@code IndexFormatTooOldException} escape.
 *
 * <p>NGB 2.8 and earlier wrote Lucene 6 indexes; this release uses Lucene 9, whose
 * {@code Version.MIN_SUPPORTED_MAJOR} is 8. There is no in-place upgrade path
 * (see {@code docs/md/installation/lucene-reindex.md}) — every index has to be
 * rebuilt once. An operator who hits that deserves a sentence telling them which directory is
 * stale and what to run, not a stack trace, so the message carries both and
 * {@code ExceptionHandlerAdvice} passes it straight through to the client.
 *
 * <p>It extends {@link IOException} deliberately: every Lucene read path here already declares
 * {@code throws IOException}, so the guard needs no signature changes to propagate.
 */
public class LuceneIndexVersionException extends IOException {

    private static final long serialVersionUID = 1L;

    private final String indexDirectory;

    public LuceneIndexVersionException(final String message, final String indexDirectory,
                                       final Throwable cause) {
        super(message, cause);
        this.indexDirectory = indexDirectory;
    }

    /**
     * The directory that has to be rebuilt. Named separately from the message so callers that
     * aggregate several failures (the startup check does) can list the directories.
     */
    public String getIndexDirectory() {
        return indexDirectory;
    }
}
