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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.exception.LuceneIndexVersionException;
import com.epam.catgenome.util.StaleLuceneIndex;

/**
 * The startup half of the Lucene upgrade guard (decision D8 of the Java 21 migration): NGB refuses
 * to start when a global index directory holds an index this release cannot read, and the refusal
 * names every such directory together with what rebuilds it.
 *
 * <p>What is being protected here is the message, not the detection — an operator upgrading from
 * NGB 2.8 will see it, and it is the whole difference between a support incident and a documented
 * step. So the assertions are on the text: the directory, the count, and the rebuild call.
 *
 * @see StaleLuceneIndex for why the unreadable index is synthesised rather than checked in
 */
public class LuceneIndexVersionCheckTest {

    private static final String NOT_CONFIGURED = "";

    @Rule
    public TemporaryFolder contents = new TemporaryFolder();

    private MessageHelper messageHelper;

    @Before
    public void installMessageHelper() {
        final ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("catgenome-messages");
        messages.setDefaultEncoding("UTF-8");
        messageHelper = MessageHelper.singleton(messages);
    }

    @Test
    public void startsWhenNoIndexDirectoryIsConfigured() throws IOException {
        // Which is every property unset - the release profile leaves taxonomy and homologene so.
        taxonomy(NOT_CONFIGURED).check();
    }

    @Test
    public void startsWhenAConfiguredDirectoryDoesNotExistYet() throws IOException {
        taxonomy(contents.getRoot().toPath().resolve("taxonomy").toString()).check();
    }

    @Test
    public void startsWhenAConfiguredDirectoryHoldsNoIndex() throws IOException {
        // A fresh installation, and the state between deleting a stale index and rebuilding it.
        final Path index = contents.newFolder("taxonomy").toPath();
        Files.createDirectory(index.resolve("not-an-index"));
        Files.write(index.resolve("names.dmp"), "1|root|".getBytes(StandardCharsets.UTF_8));

        taxonomy(index.toString()).check();
    }

    @Test
    public void startsOnAnIndexThisReleaseWrote() throws IOException {
        final Path index = StaleLuceneIndex.writeCurrent(contents.newFolder("taxonomy").toPath());

        taxonomy(index.toString()).check();
    }

    @Test
    public void refusesToStartOnAnIndexAnEarlierReleaseWrote() throws IOException {
        final Path index = StaleLuceneIndex.writeStale(contents.newFolder("taxonomy").toPath());

        final String refusal = refusalOf(taxonomy(index.toString()));

        assertMentions(refusal, "NGB will not start: 1 Lucene index director(y/ies) cannot be read");
        assertMentions(refusal, index.toString());
        assertMentions(refusal, "to rebuild: PUT /restapi/taxonomy/upload?taxonomyFilePath=<NCBI names.dmp>");
        assertMentions(refusal, "To continue: stop NGB, delete or move the director(y/ies) listed above");
        assertMentions(refusal, "installation/lucene-reindex/");
    }

    /**
     * {@code targets.index.directory} and {@code ncbi.index.directory} are parents of several leaf
     * indexes rather than indexes themselves, so the check walks them - and has to report the leaf,
     * because the leaf is what determines the rebuild call. This is the shape of a real upgrade: on
     * the migration fixture it is 14 leaves under 6 roots.
     */
    @Test
    public void reportsEveryStaleLeafOfAMultiIndexRootWithItsOwnRebuildCall() throws IOException {
        final Path targets = contents.newFolder("targets").toPath();
        StaleLuceneIndex.writeStale(targets.resolve("opentargets.disease"));
        StaleLuceneIndex.writeStale(targets.resolve("dgidb.drug.association"));
        StaleLuceneIndex.writeStale(targets.resolve("pharmgkb.drug"));
        StaleLuceneIndex.writeStale(targets.resolve("ttd.drug"));
        StaleLuceneIndex.writeStale(targets.resolve("genes"));
        // A per-target subdirectory of the same root, holding no index: not ours to report.
        Files.createDirectory(targets.resolve("42"));
        Files.write(targets.resolve("42").resolve("genes.tsv"), "id\tname".getBytes(StandardCharsets.UTF_8));
        // And one leaf that is already in the new format.
        StaleLuceneIndex.writeCurrent(targets.resolve("gene.fields"));

        final Path ncbi = contents.newFolder("ncbi").toPath();
        StaleLuceneIndex.writeStale(ncbi.resolve("gene.ids"));
        StaleLuceneIndex.writeStale(ncbi.resolve("gene.info"));

        final String refusal = refusalOf(targetsAndNcbi(targets.toString(), ncbi.toString()));

        assertMentions(refusal, "NGB will not start: 7 Lucene index director(y/ies) cannot be read");
        assertMentions(refusal, "PUT /restapi/target/import/opentargets?path=<Open Targets download>");
        assertMentions(refusal, "PUT /restapi/target/import/dgidb?path=<DGIdb download>");
        assertMentions(refusal, "PUT /restapi/target/import/pharmGKB?genePath=");
        assertMentions(refusal, "PUT /restapi/target/import/ttd?drugsPath=");
        assertMentions(refusal, "re-upload the gene spreadsheet of each target");
        assertMentions(refusal, "PUT /restapi/externaldb/ncbi/genes/import?path=<NCBI gene2ensembl>");
        assertMentions(refusal, "PUT /restapi/externaldb/ncbi/genes/info/import?path=<NCBI gene_info>");
        Assert.assertFalse("a per-target data directory is not an index and must not be reported",
                refusal.contains(targets.resolve("42").toString()));
        Assert.assertFalse("a leaf already in the current format must not be reported",
                refusal.contains(targets.resolve("gene.fields").toString()));
    }

    /**
     * These two indexes cannot be rebuilt from a downloaded file, which is why this phase added an
     * endpoint for each; the hint is where an operator finds out they exist.
     */
    @Test
    public void namesTheArgumentlessRebuildEndpointsForPathwayAndCoverage() throws IOException {
        final Path pathway = StaleLuceneIndex.writeStale(contents.newFolder("pathway").toPath());
        final Path coverage = StaleLuceneIndex.writeStale(contents.newFolder("coverage").toPath());

        final String refusal = refusalOf(new LuceneIndexVersionCheck(NOT_CONFIGURED, NOT_CONFIGURED,
                pathway.toString(), coverage.toString(), NOT_CONFIGURED, NOT_CONFIGURED, messageHelper));

        assertMentions(refusal, "PUT /restapi/pathway/index - rebuilds every registered pathway");
        assertMentions(refusal, "PUT /restapi/bam/coverage/index - recomputes every registered coverage track");
    }

    private LuceneIndexVersionCheck taxonomy(final String directory) {
        return new LuceneIndexVersionCheck(directory, NOT_CONFIGURED, NOT_CONFIGURED, NOT_CONFIGURED,
                NOT_CONFIGURED, NOT_CONFIGURED, messageHelper);
    }

    private LuceneIndexVersionCheck targetsAndNcbi(final String targets, final String ncbi) {
        return new LuceneIndexVersionCheck(NOT_CONFIGURED, NOT_CONFIGURED, NOT_CONFIGURED,
                NOT_CONFIGURED, targets, ncbi, messageHelper);
    }

    private String refusalOf(final LuceneIndexVersionCheck check) throws IOException {
        try {
            check.check();
        } catch (LuceneIndexVersionException expected) {
            return expected.getMessage();
        }
        Assert.fail("the check passed over an index written by an earlier release");
        return null;
    }

    private static void assertMentions(final String refusal, final String expected) {
        Assert.assertTrue("the startup refusal does not mention '" + expected + "', it reads:"
                + System.lineSeparator() + refusal, refusal.contains(expected));
    }
}
