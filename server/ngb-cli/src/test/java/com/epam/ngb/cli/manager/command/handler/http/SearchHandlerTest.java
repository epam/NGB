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

package com.epam.ngb.cli.manager.command.handler.http;

import com.epam.ngb.cli.AbstractCliTest;
import com.epam.ngb.cli.TestDataProvider;
import com.epam.ngb.cli.TestHttpServer;
import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.entity.BiologicalDataItem;
import com.epam.ngb.cli.entity.BiologicalDataItemFormat;
import com.epam.ngb.cli.manager.command.ServerParameters;
import org.junit.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchHandlerTest extends AbstractCliTest {

    private static final String COMMAND = "add_genes";
    private static ServerParameters serverParameters;
    private static TestHttpServer server = new TestHttpServer();

    private static final String SUBSTRING = "38";

    private static final Long REF_BIO_ID = 1L;
    private static final Long REF_ID = 50L;
    private static final String REFERENCE_NAME = "hg38";
    private static final String PATH_TO_REFERENCE = "references/50";

    private static final Long GENE_BIO_ID = 2L;
    private static final Long GENE_ID = 51L;
    private static final String GENE_NAME = "genes_38";
    private static final String PATH_TO_GENE = "path/genes.gtf";

    @BeforeClass
    public static void setUp() {
        server.start();
        server.addReference(REF_BIO_ID, REF_ID, REFERENCE_NAME, PATH_TO_REFERENCE);
        server.addFile(GENE_BIO_ID, GENE_ID, GENE_NAME, PATH_TO_GENE, BiologicalDataItemFormat.GENE);
        serverParameters = getDefaultServerOptions(server.getPort());
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }

    @After
    public void reset() {
        server.reset();
    }

    @Test
    public void testFindReference() {
        server.addReference(REF_BIO_ID, REF_ID, REFERENCE_NAME, PATH_TO_REFERENCE);
        SearchHandler handler = getSearchHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintTable(true);
        handler.parseAndVerifyArguments(Collections.singletonList(REFERENCE_NAME), applicationOptions);
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testFindFile() {
        server.addFile(GENE_BIO_ID, GENE_ID, GENE_NAME, PATH_TO_GENE, BiologicalDataItemFormat.GENE);
        SearchHandler handler = getSearchHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintJson(true);
        handler.parseAndVerifyArguments(Collections.singletonList(GENE_NAME), applicationOptions);
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testEmpty() {
        server.addEmptySearch(SUBSTRING);
        SearchHandler handler = getSearchHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintJson(true);
        handler.parseAndVerifyArguments(Collections.singletonList(SUBSTRING), applicationOptions);
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testFindSubstring() {
        List<BiologicalDataItem> items = new ArrayList<>();
        items.add(TestDataProvider.getBioItem(REF_ID, REF_BIO_ID,
                BiologicalDataItemFormat.REFERENCE, PATH_TO_REFERENCE, REFERENCE_NAME));
        items.add(TestDataProvider.getBioItem(GENE_ID, GENE_BIO_ID, BiologicalDataItemFormat.GENE,
                PATH_TO_GENE, GENE_NAME));
        server.addSearchQuery(SUBSTRING, items);
        SearchHandler handler = getSearchHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintTable(true);
        applicationOptions.setStrictSearch(false);
        handler.parseAndVerifyArguments(Collections.singletonList(SUBSTRING), applicationOptions);
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongArguments() {
        SearchHandler handler = getSearchHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintTable(true);
        handler.parseAndVerifyArguments(Collections.emptyList(), applicationOptions);
    }

    private SearchHandler getSearchHandler() {
        SearchHandler handler = new SearchHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        return handler;
    }
}
