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

import java.io.IOException;
import java.nio.file.Path;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;

/**
 * Builds an index directory that this release's Lucene refuses to open, so that the upgrade guard
 * (decision D8 of the Java 21 migration) can be tested without a checked-in binary fixture.
 *
 * <p>A real Lucene 6 index cannot be committed to the test tree: this very phase is what would
 * invalidate it, and the next Lucene upgrade would leave a fixture nobody can regenerate. It also
 * does not need to be real. Every caller of the guard reaches the failure through
 * {@code SegmentInfos}, which reads the commit point before anything else and rejects it on the
 * format version in its header — so a {@code segments_1} holding nothing but that header fails in
 * exactly the same place, with exactly the same exception, as the 21 MB of Lucene 6 index in
 * {@code .devenv/fixtures/pre-migration/lucene6/} does.
 *
 * <p>{@link #writeStale} writes format version 6, which is the value Lucene 6.6.0 wrote
 * ({@code SegmentInfos.VERSION_53}, its {@code VERSION_CURRENT}); Lucene 9 accepts 7 to 10.
 */
public final class StaleLuceneIndex {

    /**
     * {@code SegmentInfos.VERSION_53} of Lucene 6 — the format version NGB 2.8 and earlier wrote.
     * Below Lucene 9's oldest supported {@code VERSION_70}, which is 7.
     */
    private static final int LUCENE_6_SEGMENTS_FORMAT = 6;

    private StaleLuceneIndex() {
        // test fixture factory
    }

    /**
     * Creates {@code directory} if needed and puts an unreadable commit point in it.
     *
     * @return the same directory, for chaining
     */
    public static Path writeStale(final Path directory) throws IOException {
        try (Directory index = LuceneIndexUtils.openDirectory(directory);
             IndexOutput out = index.createOutput("segments_1", IOContext.DEFAULT)) {
            CodecUtil.writeHeader(out, "segments", LUCENE_6_SEGMENTS_FORMAT);
        }
        return directory;
    }

    /**
     * Creates {@code directory} if needed and puts a readable one-document index in it, for the
     * cases where the guard has to stay quiet.
     *
     * @return the same directory, for chaining
     */
    public static Path writeCurrent(final Path directory) throws IOException {
        try (Directory index = LuceneIndexUtils.openDirectory(directory);
             IndexWriter writer = new IndexWriter(index, new IndexWriterConfig(new KeywordAnalyzer()))) {
            final Document doc = new Document();
            doc.add(new StringField("id", "1", Field.Store.YES));
            writer.addDocument(doc);
            writer.commit();
        }
        return directory;
    }
}
