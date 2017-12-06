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
import com.epam.ngb.cli.entity.BiologicalDataItemFormat;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.ServerParameters;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class GeneAddingHandlerTest extends AbstractCliTest {

    private static final String COMMAND = "add_genes";
    private static ServerParameters serverParameters;
    private static TestHttpServer server = new TestHttpServer();

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
        server.addGeneAdding(
                TestDataProvider.getBioItem(REF_ID, REF_BIO_ID, BiologicalDataItemFormat.REFERENCE,
                        PATH_TO_REFERENCE, REFERENCE_NAME),
                GENE_ID);
        serverParameters = getDefaultServerOptions(server.getPort());
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }

    @Test()
    public void testAddByName() {
        GeneAddingHandler handler = getGeneAddingHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintJson(true);
        handler.parseAndVerifyArguments(Arrays.asList(REFERENCE_NAME, GENE_NAME), applicationOptions);
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test()
    public void testAddById() {
        GeneAddingHandler handler = getGeneAddingHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintTable(true);
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REF_BIO_ID), String.valueOf(GENE_BIO_ID)),
                applicationOptions);
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test(expected = ApplicationException.class)
    public void testWrongReference() {
        GeneAddingHandler handler = getGeneAddingHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        handler.parseAndVerifyArguments(Arrays.asList(REFERENCE_NAME + "/", GENE_NAME),
                applicationOptions);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongArguments() {
        GeneAddingHandler handler = getGeneAddingHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        handler.parseAndVerifyArguments(Collections.singletonList(REFERENCE_NAME),
                applicationOptions);
    }

    private GeneAddingHandler getGeneAddingHandler() {
        GeneAddingHandler handler = new GeneAddingHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        return handler;
    }
}
