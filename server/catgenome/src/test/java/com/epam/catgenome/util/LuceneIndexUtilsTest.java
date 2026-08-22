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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.exception.LuceneIndexVersionException;

/**
 * The two things every NGB index goes through {@link LuceneIndexUtils} for.
 *
 * <p>One is the lazy half of the Lucene upgrade guard: a per-file feature index is not checked at
 * startup - there can be thousands - so the first read of
 * a stale one has to name the file and the single call that rebuilds it, and the server has to keep
 * serving everything else.
 *
 * <p>The other has nothing to do with the upgrade being visible and everything to do with it being
 * correct: from Lucene 8 on, {@code IndexSearcher.search} stops counting matches at 1000, and NGB
 * puts {@code totalHits} straight into the result count of every paged search.
 *
 * @see StaleLuceneIndex for why the unreadable index is synthesised rather than checked in
 */
public class LuceneIndexUtilsTest {

    /** Comfortably past Lucene's {@code TOTAL_HITS_THRESHOLD} of 1000. */
    private static final int MORE_HITS_THAN_LUCENE_COUNTS = 2500;

    private static final String FIELD = "chr";
    private static final String VALUE = "X";

    @Rule
    public TemporaryFolder contents = new TemporaryFolder();

    @Before
    public void installMessageHelper() {
        final ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("catgenome-messages");
        messages.setDefaultEncoding("UTF-8");
        MessageHelper.singleton(messages);
    }

    @Test
    public void probeAcceptsAnAbsentAnEmptyAndACurrentIndex() throws IOException {
        Assert.assertNull(LuceneIndexUtils.probe(contents.getRoot().toPath().resolve("never-created")));
        Assert.assertNull(LuceneIndexUtils.probe(contents.newFolder("empty").toPath()));
        Assert.assertNull(LuceneIndexUtils.probe(
                StaleLuceneIndex.writeCurrent(contents.newFolder("current").toPath())));
    }

    @Test
    public void readingAStaleFeatureIndexNamesTheFileAndTheCallThatRebuildsIt() throws IOException {
        final Path index = featureIndex("VCF", 5);

        final LuceneIndexVersionException failure = assertRefused(index);

        Assert.assertEquals(index.toString(), failure.getIndexDirectory());
        assertMentions(failure, index.toString());
        assertMentions(failure, "was written by an older release of NGB");
        assertMentions(failure, "GET /restapi/vcf/5/index");
        assertMentions(failure, "delete nothing by hand");
        assertMentions(failure, "NGB CLI: ngb index_file 5");
        assertMentions(failure, "docs/md/installation/lucene-reindex.md");
    }

    @Test
    public void theRebuildCallIsDerivedFromTheIndexPathForEveryIndexedFileType() throws IOException {
        assertMentions(assertRefused(featureIndex("VCF", 1)), "GET /restapi/vcf/1/index");
        // FEATURE_COUNTS files share the genes directory, and the same endpoint rebuilds both.
        assertMentions(assertRefused(featureIndex("genes", 2)), "GET /restapi/gene/2/index?full=true");
        assertMentions(assertRefused(featureIndex("bed", 3)), "GET /restapi/bed/3/index");
    }

    @Test
    public void anIndexThatIsNotAFeatureIndexFallsBackToTheProcedure() throws IOException {
        final Path index = StaleLuceneIndex.writeStale(contents.newFolder("taxonomy").toPath());

        assertMentions(assertRefused(index), "re-run the import that populated it");
    }

    @Test
    public void aWriterOverAStaleIndexIsRefusedTheSameWayAsAReader() throws IOException {
        final Path index = featureIndex("VCF", 7);

        try (Directory directory = LuceneIndexUtils.openDirectory(index);
             IndexWriter writer = LuceneIndexUtils.openWriter(directory,
                     new IndexWriterConfig(new KeywordAnalyzer()))) {
            Assert.fail("a writer was opened over an index written by an earlier release: " + writer);
        } catch (LuceneIndexVersionException expected) {
            assertMentions(expected, "GET /restapi/vcf/7/index");
        }
    }

    /**
     * Which is what lets the reindex procedure say "you do not delete anything first" for a feature
     * index, and what lets a global re-import run over a directory the operator has not cleaned:
     * {@code IndexWriter}'s constructor reads the existing commit even with {@code OpenMode.CREATE},
     * so without this every rebuild would fail on the index it was about to replace.
     */
    @Test
    public void aRebuildWriterDiscardsAnUnreadableIndexAndLeavesEverythingElseAlone() throws IOException {
        final Path index = featureIndex("VCF", 9);
        final Path notLucenes = index.resolve("registration.log");
        Files.write(notLucenes, "kept".getBytes(StandardCharsets.UTF_8));

        try (Directory directory = LuceneIndexUtils.openDirectory(index);
             IndexWriter writer = LuceneIndexUtils.openWriterForRebuild(directory,
                     new IndexWriterConfig(new KeywordAnalyzer()))) {
            writer.deleteAll();
            writer.addDocument(document(VALUE));
            writer.commit();
        }

        Assert.assertNull("the rebuilt index is not readable", LuceneIndexUtils.probe(index));
        Assert.assertTrue("a file Lucene does not own was deleted", Files.exists(notLucenes));
        try (Directory directory = LuceneIndexUtils.openDirectory(index);
             DirectoryReader reader = LuceneIndexUtils.openReader(directory)) {
            Assert.assertEquals(1, reader.numDocs());
        }
    }

    @Test
    public void everyMatchIsCountedNotOnlyTheFirstThousand() throws IOException {
        final Path index = indexWith(MORE_HITS_THAN_LUCENE_COUNTS);

        try (Directory directory = LuceneIndexUtils.openDirectory(index);
             DirectoryReader reader = DirectoryReader.open(directory)) {
            final IndexSearcher searcher = new IndexSearcher(reader);
            final TermQuery query = new TermQuery(new Term(FIELD, VALUE));

            final TopDocs unsorted = LuceneIndexUtils.search(searcher, query, 10);
            Assert.assertEquals(MORE_HITS_THAN_LUCENE_COUNTS, LuceneIndexUtils.totalHits(unsorted));
            Assert.assertEquals(10, unsorted.scoreDocs.length);

            final Sort sort = new Sort(new SortField(FIELD, SortField.Type.STRING));
            final TopDocs sorted = LuceneIndexUtils.search(searcher, query, 10, sort);
            Assert.assertEquals(MORE_HITS_THAN_LUCENE_COUNTS, LuceneIndexUtils.totalHits(sorted));

            final TopDocs nextPage = LuceneIndexUtils.searchAfter(searcher, sorted.scoreDocs[9], query, 10, sort);
            Assert.assertEquals(MORE_HITS_THAN_LUCENE_COUNTS, LuceneIndexUtils.totalHits(nextPage));
            Assert.assertEquals(10, nextPage.scoreDocs.length);
        }
    }

    /**
     * An unreadable index at the path a feature index actually lives at:
     * {@code <base>/<reference id>/<TYPE>/<file id>/index.luc}, per
     * {@code FileManager.FilePathFormat}. The path is the only thing the rebuild hint is derived
     * from - there is no database lookup - so it has to be the real shape.
     */
    private Path featureIndex(final String type, final long fileId) throws IOException {
        final Path directory = contents.getRoot().toPath()
                .resolve("42").resolve(type).resolve(String.valueOf(fileId)).resolve("index.luc");
        Files.createDirectories(directory);
        return StaleLuceneIndex.writeStale(directory);
    }

    private Path indexWith(final int documents) throws IOException {
        final Path directory = contents.newFolder("many").toPath();
        try (Directory index = LuceneIndexUtils.openDirectory(directory);
             IndexWriter writer = new IndexWriter(index, new IndexWriterConfig(new KeywordAnalyzer()))) {
            for (int i = 0; i < documents; i++) {
                writer.addDocument(document(VALUE));
            }
            writer.commit();
        }
        return directory;
    }

    private static Document document(final String value) {
        final Document doc = new Document();
        doc.add(new StringField(FIELD, value, Field.Store.YES));
        doc.add(new SortedDocValuesField(FIELD, new BytesRef(value)));
        return doc;
    }

    private LuceneIndexVersionException assertRefused(final Path index) throws IOException {
        try (Directory directory = LuceneIndexUtils.openDirectory(index);
             DirectoryReader reader = LuceneIndexUtils.openReader(directory)) {
            Assert.fail("an index written by an earlier release was opened: " + index
                    + ", holding " + reader.numDocs() + " document(s)");
        } catch (LuceneIndexVersionException expected) {
            return expected;
        }
        return null;
    }

    private static void assertMentions(final LuceneIndexVersionException failure, final String expected) {
        Assert.assertTrue("the failure does not mention '" + expected + "', it reads:"
                + System.lineSeparator() + failure.getMessage(), failure.getMessage().contains(expected));
    }
}
