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

public class FileDeletionHandlerTest extends AbstractCliTest {

    private static final String COMMAND = "delete_file";
    private static ServerParameters serverParameters;
    private static TestHttpServer server = new TestHttpServer();

    private static final Long VCF_BIO_ID = 2L;
    private static final Long VCF_ID = 1L;
    private static final String PATH_TO_VCF = "path/test.vcf";
    private static final String VCF_NAME = "test.vcf";

    private static final Long BAM_BIO_ID = 3L;
    private static final Long BAM_ID = 1L;
    private static final String PATH_TO_BAM = "path/test.bam";
    private static final String BAM_NAME = "test.bam";

    @BeforeClass
    public static void setUp() {
        server.start();

        server.addFile(VCF_BIO_ID, VCF_ID, VCF_NAME, PATH_TO_VCF, BiologicalDataItemFormat.VCF);
        server.addFile(BAM_BIO_ID, BAM_ID, BAM_NAME, PATH_TO_BAM, BiologicalDataItemFormat.BAM);

        server.addFileDeletion(VCF_BIO_ID, VCF_NAME);
        server.addFileDeletion(BAM_BIO_ID, BAM_NAME);

        serverParameters = getDefaultServerOptions(server.getPort());
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }

    @Test
    public void testDeleteByName() {
        FileDeletionHandler handler = getFileDeletionHandler();
        handler.parseAndVerifyArguments(Collections.singletonList(String.valueOf(VCF_BIO_ID)),
                new ApplicationOptions());
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testDeleteByID() {
        FileDeletionHandler handler = getFileDeletionHandler();
        handler.parseAndVerifyArguments(Collections.singletonList(BAM_NAME),
                new ApplicationOptions());
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoArguments() {
        FileDeletionHandler handler = getFileDeletionHandler();
        handler.parseAndVerifyArguments(Collections.emptyList(), new ApplicationOptions());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTooManyArguments() {
        FileDeletionHandler handler = getFileDeletionHandler();
        handler.parseAndVerifyArguments(Arrays.asList(BAM_NAME, VCF_NAME), new ApplicationOptions());
    }

    private FileDeletionHandler getFileDeletionHandler() {
        FileDeletionHandler handler = new FileDeletionHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        return handler;
    }
}
