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
 *
 */

package com.epam.ngb.cli.manager.command.handler.http;

import com.epam.ngb.cli.AbstractCliTest;
import com.epam.ngb.cli.TestHttpServer;
import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.entity.BiologicalDataItemFormat;
import com.epam.ngb.cli.manager.command.ServerParameters;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Source:      FileIndexingHandlerTest
 * Created:     16.12.16, 17:20
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0.3, JDK 1.8
 */
public class FileIndexingHandlerTest extends AbstractCliTest {
    private static final String COMMAND = "index_file";
    private static ServerParameters serverParameters;
    private static TestHttpServer server = new TestHttpServer();

    private static final Long VCF_BIO_ID = 2L;
    private static final Long VCF_ID = 1L;
    private static final String PATH_TO_VCF = "path/test.vcf";
    private static final String VCF_NAME = "test.vcf";

    @BeforeClass
    public static void setUp() {
        server.start();

        server.addFile(VCF_BIO_ID, VCF_ID, VCF_NAME, PATH_TO_VCF, BiologicalDataItemFormat.VCF);
        server.addFileIndexing(VCF_ID, VCF_NAME, BiologicalDataItemFormat.VCF);

        serverParameters = getDefaultServerOptions(server.getPort());
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }

    @Test
    public void testReindexByID() {
        FileIndexingHandler handler = getFileIndexationHandler();
        handler.parseAndVerifyArguments(Collections.singletonList(VCF_BIO_ID.toString()), new ApplicationOptions());
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testReindexByName() {
        FileIndexingHandler handler = getFileIndexationHandler();
        handler.parseAndVerifyArguments(Collections.singletonList(VCF_NAME), new ApplicationOptions());
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongArguments() {
        FileIndexingHandler handler = getFileIndexationHandler();
        handler.parseAndVerifyArguments(Arrays.asList(VCF_BIO_ID.toString(), VCF_NAME), new ApplicationOptions());
    }

    private FileIndexingHandler getFileIndexationHandler() {
        FileIndexingHandler handler = new FileIndexingHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        return handler;
    }
}
