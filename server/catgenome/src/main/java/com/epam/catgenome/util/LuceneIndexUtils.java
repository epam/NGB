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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexFormatTooNewException;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollectorManager;
import org.apache.lucene.search.TopScoreDocCollectorManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.exception.LuceneIndexVersionException;

/**
 * Every Lucene index directory in NGB is opened through here, so that the one failure an
 * operator is guaranteed to hit after upgrading — "this index was written by the old Lucene" —
 * comes out as a sentence they can act on rather than as an
 * {@code IndexFormatTooOldException} stack trace.
 *
 * <p>NGB 2.8 and earlier wrote Lucene 6 indexes. This release links Lucene 9, whose
 * {@link Version#MIN_SUPPORTED_MAJOR} is 8, and there is no in-place upgrade: the format
 * version is rejected by {@code SegmentInfos}, and it is rejected identically whether the
 * caller opens a {@link DirectoryReader}, opens an {@link IndexWriter}, or only reads the
 * commit point. All three are wrapped below.
 *
 * <p>{@link IndexNotFoundException} is deliberately <em>not</em> wrapped. An absent or empty
 * index directory is a normal state here — several managers catch it and return an empty result
 * — and it is not what the reindex message is about.
 *
 * <p>It also carries the {@code search} wrappers, for a reason that has nothing to do with the
 * guard: see {@link #search(IndexSearcher, Query, int)}.
 */
public final class LuceneIndexUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneIndexUtils.class);

    /**
     * Count every match, the way Lucene 6 did. See {@link #search(IndexSearcher, Query, int)}.
     */
    private static final int COUNT_ALL_HITS = Integer.MAX_VALUE;

    /**
     * Feature indexes live at {@code <base>/<root>/<TYPE>/<id>/index.luc}, per
     * {@code FileManager.FilePathFormat}. This is the last path element.
     */
    private static final String FEATURE_INDEX_DIR_NAME = "index.luc";

    private LuceneIndexUtils() {
        // utility class
    }

    /**
     * {@code searcher.search(query, n)}, but with {@code TopDocs.totalHits} guaranteed exact.
     *
     * <p>This is not cosmetic. Lucene 6 counted every matching document; from Lucene 8 on,
     * {@code IndexSearcher.search} stops counting once it has seen
     * {@code IndexSearcher.TOTAL_HITS_THRESHOLD} (1000) of them and reports the rest as a lower
     * bound. NGB feeds {@code totalHits} straight into {@code totalCount} on every paged result,
     * so left alone the upgrade would silently cap every "N variations found" and every page
     * count in the client at 1000. Asking the collector manager for an unlimited threshold
     * restores the Lucene 6 behaviour — and its Lucene 6 cost, which is what the paging
     * expects.
     *
     * <p>Only used where the total is actually read. Call sites that want a page and nothing
     * else still go straight to {@code IndexSearcher}, and keep the faster early termination.
     */
    public static TopDocs search(final IndexSearcher searcher, final Query query, final int n) throws IOException {
        return searcher.search(query, new TopScoreDocCollectorManager(hitsToCollect(n), null, COUNT_ALL_HITS));
    }

    /**
     * @see #search(IndexSearcher, Query, int)
     */
    public static TopDocs search(final IndexSearcher searcher, final Query query, final int n, final Sort sort)
            throws IOException {
        return sort == null ? search(searcher, query, n)
                : searcher.search(query, new TopFieldCollectorManager(sort, hitsToCollect(n), null, COUNT_ALL_HITS));
    }

    /**
     * @see #search(IndexSearcher, Query, int)
     */
    public static TopDocs searchAfter(final IndexSearcher searcher, final ScoreDoc after, final Query query,
                                      final int n, final Sort sort) throws IOException {
        if (sort == null) {
            return searcher.search(query, new TopScoreDocCollectorManager(hitsToCollect(n), after, COUNT_ALL_HITS));
        }
        Assert.isTrue(after == null || after instanceof FieldDoc,
                "A sorted search has to be continued from a FieldDoc, not a plain ScoreDoc");
        return searcher.search(query,
                new TopFieldCollectorManager(sort, hitsToCollect(n), (FieldDoc) after, COUNT_ALL_HITS));
    }

    /**
     * {@code TopDocs.totalHits} became a {@code TotalHits} holding a {@code long} in Lucene 8.
     * Every NGB caller wants the {@code int} it used to be; saturating rather than throwing
     * keeps the absurd case (more than {@code Integer.MAX_VALUE} matches) behaving as it did
     * when the field was an {@code int}.
     */
    public static int totalHits(final TopDocs docs) {
        return (int) Math.min(docs.totalHits.value, Integer.MAX_VALUE);
    }

    /**
     * The collector managers reject a non-positive size, and callers derive it from
     * {@code reader.numDocs()}, which is zero for an empty index.
     */
    private static int hitsToCollect(final int n) {
        return Math.max(1, n);
    }

    /**
     * Opens an index directory, creating it if absent (which is what the old
     * {@code SimpleFSDirectory} constructor did too). {@code FSDirectory.open} picks
     * {@code MMapDirectory} on a 64-bit JVM, which is Lucene's recommended default; the
     * removal of {@code SimpleFSDirectory} in Lucene 9 is what forced the change.
     */
    public static FSDirectory openDirectory(final String directory) throws IOException {
        return openDirectory(Paths.get(directory));
    }

    public static FSDirectory openDirectory(final Path directory) throws IOException {
        return FSDirectory.open(directory);
    }

    /**
     * {@code DirectoryReader.open}, with the version failure translated.
     */
    public static DirectoryReader openReader(final Directory directory) throws IOException {
        try {
            return DirectoryReader.open(directory);
        } catch (IndexFormatTooOldException | IndexFormatTooNewException e) {
            throw versionException(directory, e);
        }
    }

    /**
     * {@code new IndexWriter(...)}, with the version failure translated. Worth wrapping as well
     * as the reader: an operator re-running an import over a stale directory hits the writer
     * first, and that is exactly the moment the message is useful.
     */
    public static IndexWriter openWriter(final Directory directory, final IndexWriterConfig config)
            throws IOException {
        try {
            return new IndexWriter(directory, config);
        } catch (IndexFormatTooOldException | IndexFormatTooNewException e) {
            throw versionException(directory, e);
        }
    }

    /**
     * As {@link #openWriter}, for the call sites whose next statement is
     * {@code writer.deleteAll()} — a full rebuild. If the directory holds an index this Lucene
     * cannot read, the unreadable files are discarded before the writer is opened.
     *
     * <p>That is not a licence to delete data, and it is why this is a separate method rather
     * than behaviour of {@link #openWriter}. A caller that opens {@code CREATE_OR_APPEND} and
     * immediately calls {@code deleteAll()} has already declared everything in the directory
     * disposable; discarding the files throws away precisely what {@code deleteAll()} was about
     * to. Without it every global re-import would fail on a pre-upgrade index, because
     * {@code IndexWriter}'s constructor reads the existing commit — even with
     * {@code OpenMode.CREATE} — and so throws before {@code deleteAll()} is ever reached.
     *
     * <p>The state of the directory is settled with {@link #probe} first rather than by opening a
     * writer and recovering from the failure, because an {@link IndexWriterConfig} cannot be used
     * twice: {@code IndexWriter}'s constructor claims it on its first statement, so a second
     * attempt with the same config fails with "do not share IndexWriterConfig instances across
     * IndexWriters" whatever the directory now looks like. Probing also means the writer and the
     * startup check decide "is this index readable" through the same code.
     *
     * <p>Only files matching Lucene's own naming are removed, only from this directory, never
     * recursively and never the directory itself.
     */
    public static IndexWriter openWriterForRebuild(final Directory directory, final IndexWriterConfig config)
            throws IOException {
        if (directory instanceof FSDirectory && probe(directory) != null) {
            final Path path = ((FSDirectory) directory).getDirectory();
            final int discarded = discardIndexFiles(directory);
            LOGGER.warn(MessageHelper.getMessage(MessagesConstants.WARN_LUCENE_INDEX_DISCARDED,
                    path, discarded, Version.LATEST));
        }
        return openWriter(directory, config);
    }

    /**
     * Reads only the commit point, to answer "would this index be readable?" without building a
     * reader over it. Used by the startup check.
     *
     * @return the failure to report, or {@code null} if the directory is readable, absent or
     *         empty
     * @throws IOException if the directory cannot be read at all
     */
    public static LuceneIndexVersionException probe(final Path directory) throws IOException {
        try (Directory index = openDirectory(directory)) {
            return probe(index);
        }
    }

    /**
     * @see #probe(Path)
     */
    public static LuceneIndexVersionException probe(final Directory directory) throws IOException {
        try {
            SegmentInfos.readLatestCommit(directory);
            return null;
        } catch (IndexNotFoundException e) {
            return null;
        } catch (IndexFormatTooOldException | IndexFormatTooNewException e) {
            return versionException(directory, e);
        }
    }

    /**
     * The rebuild instruction for a feature index directory, derived from the path — the
     * directory name two levels up is the file type and the one above is the
     * {@code BiologicalDataItem} id, so the exact REST call can be named without a database
     * lookup. Returns {@code null} for anything that is not a feature index.
     */
    public static String featureIndexRebuildHint(final Path directory) {
        if (directory == null || directory.getNameCount() < 3
                || !FEATURE_INDEX_DIR_NAME.equals(directory.getFileName().toString())) {
            return null;
        }
        final String id = directory.getParent().getFileName().toString();
        final String type = directory.getParent().getParent().getFileName().toString();
        final String endpoint;
        switch (type) {
            case "VCF":
                endpoint = "GET /restapi/vcf/" + id + "/index";
                break;
            case "genes":
                endpoint = "GET /restapi/gene/" + id + "/index?full=true";
                break;
            case "bed":
                endpoint = "GET /restapi/bed/" + id + "/index";
                break;
            default:
                return null;
        }
        return MessageHelper.getMessage(MessagesConstants.INFO_LUCENE_INDEX_REBUILD_FILE, endpoint, id);
    }

    /**
     * Deletes the Lucene-owned files in an index directory, leaving anything else — including
     * {@code write.lock}, which Lucene recreates and tolerates — alone.
     *
     * @return how many files were removed, for the log line
     */
    private static int discardIndexFiles(final Directory directory) throws IOException {
        int discarded = 0;
        for (final String name : directory.listAll()) {
            if (IndexWriter.WRITE_LOCK_NAME.equals(name) || !isLuceneFile(name)) {
                continue;
            }
            try {
                directory.deleteFile(name);
                discarded++;
            } catch (NoSuchFileException | FileNotFoundException alreadyGone) {
                LOGGER.debug("{} vanished while discarding an unreadable index", name);
            }
        }
        return discarded;
    }

    private static boolean isLuceneFile(final String name) {
        return name.startsWith("segments") || name.startsWith("pending_segments") || name.startsWith("_");
    }

    private static LuceneIndexVersionException versionException(final Directory directory, final Throwable cause) {
        return versionException(directory instanceof FSDirectory
                ? ((FSDirectory) directory).getDirectory().toString()
                : directory.toString(), cause);
    }

    private static LuceneIndexVersionException versionException(final String directory, final Throwable cause) {
        final Path path = Paths.get(directory);
        final String hint = featureIndexRebuildHint(path);
        return new LuceneIndexVersionException(
                MessageHelper.getMessage(MessagesConstants.ERROR_LUCENE_INDEX_VERSION,
                        directory,
                        Version.LATEST,
                        hint != null ? hint
                                : MessageHelper.getMessage(MessagesConstants.INFO_LUCENE_INDEX_REBUILD_UNKNOWN)),
                directory, cause);
    }
}
