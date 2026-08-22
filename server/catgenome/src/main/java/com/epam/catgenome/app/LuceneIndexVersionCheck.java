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

package com.epam.catgenome.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.exception.LuceneIndexVersionException;
import com.epam.catgenome.util.LuceneIndexUtils;

/**
 * Refuses to start NGB when one of the global Lucene index directories holds an index this
 * release cannot read, and says which directories they are and what rebuilds each one.
 *
 * <p>NGB 2.8 and earlier wrote Lucene 6 indexes; this release links Lucene 9, which cannot read
 * that format and has no in-place upgrade. Left unguarded, the first request that touched such a
 * directory would fail with an {@code IndexFormatTooOldException} from somewhere deep in a
 * manager - a support incident. Checked here, the same situation is a documented upgrade step.
 *
 * <p>Only the six configured <em>global</em> index roots are checked
 * ({@code *.index.directory}). Two of them - {@code targets.index.directory} and
 * {@code ncbi.index.directory} - are parents of several leaf indexes, so each root is walked
 * rather than probed directly, and a directory is taken to be an index when it contains a
 * {@code segments*} file. That way a leaf index added later is covered without touching this
 * class, and nothing is probed that is not actually an index.
 *
 * <p>Per-file feature indexes are deliberately <em>not</em> checked here. There is one per
 * registered VCF/GFF/BED file and an installation can have thousands; walking them all would turn
 * every start into a filesystem sweep. They are caught lazily instead, when the file is first
 * read, by the wrappers in {@link LuceneIndexUtils} - and unlike a global index, rebuilding one is
 * a single REST call that needs no manual deletion.
 *
 * <p>The check runs in {@link #afterPropertiesSet()} rather than off a context-refreshed event so
 * that it happens while the context is still coming up, and it fails startup rather than warning:
 * an installation whose indexes cannot be read is not serving anything useful from them, and a
 * warning at WARN is invisible under the shipped log4j2 profiles anyway.
 */
@Component
public class LuceneIndexVersionCheck implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneIndexVersionCheck.class);

    /**
     * How far below a configured root an index may sit. The four single-index roots are the index
     * itself (depth 0); {@code targets} and {@code ncbi} hold theirs one level down.
     */
    private static final int MAX_DEPTH = 2;

    private static final String SEGMENTS_PREFIX = "segments";

    private static final String TAXONOMY = "taxonomy.index.directory";
    private static final String HOMOLOGENE = "homologene.index.directory";
    private static final String PATHWAY = "pathway.index.directory";
    private static final String COVERAGE = "bam.coverage.index.directory";
    private static final String TARGETS = "targets.index.directory";
    private static final String NCBI = "ncbi.index.directory";

    /**
     * Configured root directories by the property they came from, in the order they are reported.
     * A property that is unset - which the {@code release} profile does for taxonomy and
     * homologene, and every profile does for some of the others - is simply skipped.
     */
    private final Map<String, String> roots = new LinkedHashMap<>();

    public LuceneIndexVersionCheck(@Value("${taxonomy.index.directory:}") final String taxonomy,
                                   @Value("${homologene.index.directory:}") final String homologene,
                                   @Value("${pathway.index.directory:}") final String pathway,
                                   @Value("${bam.coverage.index.directory:}") final String coverage,
                                   @Value("${targets.index.directory:}") final String targets,
                                   @Value("${ncbi.index.directory:}") final String ncbi,
                                   // Not read, but injecting it guarantees the static MessageHelper
                                   // singleton is in place before this check formats a message.
                                   final MessageHelper messageHelper) {
        roots.put(TAXONOMY, taxonomy);
        roots.put(HOMOLOGENE, homologene);
        roots.put(PATHWAY, pathway);
        roots.put(COVERAGE, coverage);
        roots.put(TARGETS, targets);
        roots.put(NCBI, ncbi);
    }

    @Override
    public void afterPropertiesSet() throws IOException {
        check();
    }

    void check() throws IOException {
        final List<LuceneIndexVersionException> stale = new ArrayList<>();
        final List<String> checkedRoots = new ArrayList<>();
        int checked = 0;

        for (final Map.Entry<String, String> root : roots.entrySet()) {
            final String configured = root.getValue();
            if (StringUtils.isBlank(configured)) {
                continue;
            }
            final Path rootPath = Paths.get(configured.trim());
            if (!Files.isDirectory(rootPath)) {
                continue;
            }
            checkedRoots.add(rootPath.toString());
            for (final Path index : indexDirectoriesUnder(rootPath)) {
                checked++;
                final LuceneIndexVersionException failure = probe(index);
                if (failure != null) {
                    stale.add(failure);
                }
            }
        }

        if (!stale.isEmpty()) {
            throw new LuceneIndexVersionException(
                    MessageHelper.getMessage(MessagesConstants.ERROR_LUCENE_INDEX_VERSION_STARTUP,
                            stale.size(), Version.LATEST, describe(stale)),
                    stale.get(0).getIndexDirectory(), stale.get(0));
        }
        if (checkedRoots.isEmpty()) {
            LOGGER.info(MessageHelper.getMessage(MessagesConstants.INFO_LUCENE_INDEX_CHECK_SKIPPED));
        } else if (checked == 0) {
            LOGGER.info(MessageHelper.getMessage(MessagesConstants.INFO_LUCENE_INDEX_CHECK_EMPTY,
                    String.join(", ", checkedRoots)));
        } else {
            LOGGER.info(MessageHelper.getMessage(MessagesConstants.INFO_LUCENE_INDEX_CHECK_PASSED,
                    checked, String.join(", ", checkedRoots), Version.LATEST));
        }
    }

    /**
     * A directory that cannot be read at all - permissions, a broken mount - is not what this
     * check is about, and refusing to start over it would be a new failure mode rather than a
     * guard. Those are logged and the walk continues; the manager that needs the index will
     * report the real problem when it gets there.
     */
    private LuceneIndexVersionException probe(final Path index) {
        try {
            return LuceneIndexUtils.probe(index);
        } catch (IOException e) {
            LOGGER.warn(MessageHelper.getMessage(MessagesConstants.WARN_LUCENE_INDEX_CHECK_FAILED,
                    index, e.getMessage()));
            return null;
        }
    }

    /**
     * Every directory at or below {@code root} that holds a Lucene commit point. An index
     * directory is recognised by its {@code segments_N} file rather than by name, so this covers
     * the leaf indexes of {@code targets}/{@code ncbi} without listing them, and skips the
     * BioPAX and data subdirectories that share those roots.
     */
    private List<Path> indexDirectoriesUnder(final Path root) throws IOException {
        try (Stream<Path> tree = Files.walk(root, MAX_DEPTH)) {
            return tree.filter(Files::isDirectory)
                    .filter(LuceneIndexVersionCheck::holdsSegmentsFile)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (UncheckedIOException e) {
            // Files.walk defers directory listing, so an unreadable subdirectory surfaces here.
            throw e.getCause();
        }
    }

    private static boolean holdsSegmentsFile(final Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return files.anyMatch(f -> f.getFileName().toString().startsWith(SEGMENTS_PREFIX));
        } catch (IOException e) {
            LOGGER.warn(MessageHelper.getMessage(MessagesConstants.WARN_LUCENE_INDEX_CHECK_FAILED,
                    directory, e.getMessage()));
            return false;
        }
    }

    /**
     * The block that goes into the startup message: one paragraph per unreadable index, naming the
     * directory and the call that rebuilds it.
     */
    private String describe(final List<LuceneIndexVersionException> stale) {
        return stale.stream()
                .map(failure -> "  " + failure.getIndexDirectory() + System.lineSeparator()
                        + "      to rebuild: " + rebuildHint(Paths.get(failure.getIndexDirectory())))
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
    }

    /**
     * What rebuilds the index in {@code directory}, matched first against the configured roots and
     * then, for the two roots that hold several leaf indexes, against the leaf directory name.
     *
     * <p>Kept in one place on purpose: this is the same mapping as the table in
     * {@code docs/md/installation/lucene-reindex.md}, and it is the whole value of the guard. A
     * leaf this method does not recognise still gets a usable answer from the fallback, which is
     * why the {@code default} is not an exception.
     */
    private String rebuildHint(final Path directory) {
        for (final Map.Entry<String, String> root : roots.entrySet()) {
            if (StringUtils.isBlank(root.getValue())) {
                continue;
            }
            final Path rootPath = realPath(Paths.get(root.getValue().trim()));
            final Path indexPath = realPath(directory);
            if (!indexPath.startsWith(rootPath)) {
                continue;
            }
            switch (root.getKey()) {
                case TAXONOMY:
                    return "PUT /restapi/taxonomy/upload?taxonomyFilePath=<NCBI names.dmp>";
                case HOMOLOGENE:
                    return "PUT /restapi/homologene/import?databasePath=<homologene.xml>";
                case PATHWAY:
                    return "PUT /restapi/pathway/index - rebuilds every registered pathway from the "
                            + "database and the pathway files, no arguments needed";
                case COVERAGE:
                    return "PUT /restapi/bam/coverage/index - recomputes every registered coverage "
                            + "track from its BAM file; long-running";
                case TARGETS:
                    return targetsHint(indexPath.getFileName().toString());
                case NCBI:
                    return ncbiHint(indexPath.getFileName().toString());
                default:
                    break;
            }
        }
        return MessageHelper.getMessage(MessagesConstants.INFO_LUCENE_INDEX_REBUILD_UNKNOWN);
    }

    /**
     * The directory an unreadable index is reported under comes from {@code FSDirectory}, which
     * resolves symlinks; the configured roots are whatever is in the properties file, commonly
     * relative. Both go through this so that "is this index under that root" compares like with
     * like.
     */
    private static Path realPath(final Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }

    private String targetsHint(final String leaf) {
        if (leaf.startsWith("opentargets.")) {
            return "PUT /restapi/target/import/opentargets?path=<Open Targets download>";
        }
        if (leaf.startsWith("dgidb.")) {
            return "PUT /restapi/target/import/dgidb?path=<DGIdb download>";
        }
        if (leaf.startsWith("pharmgkb.")) {
            return "PUT /restapi/target/import/pharmGKB?genePath=&drugPath=&drugAssociationPath="
                    + "&diseaseAssociationPath=<PharmGKB downloads>";
        }
        if (leaf.startsWith("ttd.")) {
            return "PUT /restapi/target/import/ttd?drugsPath=&targetsPath=&diseasesPath="
                    + "<TTD downloads>";
        }
        if ("genes".equals(leaf) || "gene.fields".equals(leaf)) {
            // The only index NGB cannot rebuild from anything it kept - see the reindex procedure.
            return "re-upload the gene spreadsheet of each target: POST "
                    + "/restapi/target/genes/import/{targetId}. NGB does not keep the uploaded "
                    + "file, so the original xlsx/csv/tsv is needed.";
        }
        return MessageHelper.getMessage(MessagesConstants.INFO_LUCENE_INDEX_REBUILD_UNKNOWN);
    }

    private String ncbiHint(final String leaf) {
        if ("gene.ids".equals(leaf)) {
            return "PUT /restapi/externaldb/ncbi/genes/import?path=<NCBI gene2ensembl>";
        }
        if ("gene.info".equals(leaf)) {
            return "PUT /restapi/externaldb/ncbi/genes/info/import?path=<NCBI gene_info>";
        }
        return MessageHelper.getMessage(MessagesConstants.INFO_LUCENE_INDEX_REBUILD_UNKNOWN);
    }
}
