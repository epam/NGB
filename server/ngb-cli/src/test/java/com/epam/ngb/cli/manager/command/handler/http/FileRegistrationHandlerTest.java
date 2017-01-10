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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import com.epam.ngb.cli.TestHttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.epam.ngb.cli.AbstractCliTest;
import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.entity.BiologicalDataItemFormat;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.ServerParameters;

public class FileRegistrationHandlerTest extends AbstractCliTest {

    private static final Long REF_BIO_ID = 1L;
    private static final Long REF_ID = 50L;
    private static final Long VCF_BIO_ID = 2L;
    private static final Long VCF_ID = 1L;
    private static final Long BAM_BIO_ID = 3L;
    private static final Long BAM_ID = 1L;
    private static final String PATH_TO_REFERENCE = "reference/50";
    private static final String REFERENCE_NAME = "hg38";
    private static final String PATH_TO_VCF = "path/test.vcf";
    private static final String PATH_TO_BAM = "path/test.bam";
    private static final String PATH_TO_BAI = "path/test.bam.bai";
    private static final String BAM_NAME = "MyBam";

    private static final String COMMAND = "register_file";
    private static ServerParameters serverParameters;
    private static TestHttpServer server = new TestHttpServer();

    @BeforeClass
    public static void setUp() {
        server.start();
        server.addReference(REF_BIO_ID, REF_ID, REFERENCE_NAME, PATH_TO_REFERENCE);

        //register vcf, no name, no index
        server.addFeatureIndexedFileRegistration(REF_ID, PATH_TO_VCF, null, null, VCF_ID,
                VCF_BIO_ID, BiologicalDataItemFormat.VCF, true);

        // add another without feature index
        server.addFeatureIndexedFileRegistration(REF_ID, PATH_TO_VCF, null, null, VCF_ID,
                                                 VCF_BIO_ID, BiologicalDataItemFormat.VCF, false);

        //register BAM with name
        server.addFileRegistration(REF_ID, PATH_TO_BAM, PATH_TO_BAI, BAM_NAME, BAM_ID, BAM_BIO_ID,
                BiologicalDataItemFormat.BAM);

        //register BAM without name
        server.addFileRegistration(REF_ID, PATH_TO_BAM, PATH_TO_BAI, null, BAM_ID, BAM_BIO_ID,
                BiologicalDataItemFormat.BAM);

        serverParameters = getDefaultServerOptions(server.getPort());
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }

    @Test
    public void testFileRegistration() {
        AbstractHTTPCommandHandler handler = createFileRegCommandHandler(serverParameters, COMMAND);
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REF_BIO_ID),
                PATH_TO_VCF), new ApplicationOptions());
        assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testFileRegistrationConstructor() {
        AbstractHTTPCommandHandler outerHandler =  createFileRegCommandHandler(serverParameters, COMMAND);
        FileRegistrationHandler handler = new FileRegistrationHandler(PATH_TO_VCF, REF_ID,
                outerHandler, new ApplicationOptions());
        assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testSeveralFilesRegistration() {
        AbstractHTTPCommandHandler handler =  createFileRegCommandHandler(serverParameters, COMMAND);
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintTable(true);
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REF_BIO_ID),
                PATH_TO_VCF, PATH_TO_BAM + "?" + PATH_TO_BAI), applicationOptions);
        handler.runCommand();
        assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testFileRegistrationWithReferenceName() {
        AbstractHTTPCommandHandler handler =  createFileRegCommandHandler(serverParameters, COMMAND);
        handler.parseAndVerifyArguments(Arrays.asList(REFERENCE_NAME,
                PATH_TO_VCF), new ApplicationOptions());
        assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testFileRegistrationWithIndex() {
        AbstractHTTPCommandHandler handler =  createFileRegCommandHandler(serverParameters, COMMAND);
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setName(BAM_NAME);
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REF_BIO_ID),
                PATH_TO_BAM + "?" + PATH_TO_BAI), applicationOptions);
        assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test(expected = ApplicationException.class)
    public void testFileRegistrationWithWrongReference() {
        AbstractHTTPCommandHandler handler =  createFileRegCommandHandler(serverParameters, COMMAND);
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REF_ID),
                PATH_TO_VCF), new ApplicationOptions());
        handler.runCommand();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileRegistrationWithoutArguments() {
        AbstractHTTPCommandHandler handler =  createFileRegCommandHandler(serverParameters, COMMAND);
        handler.parseAndVerifyArguments(Collections.emptyList(), new ApplicationOptions());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileRegistrationWithoutRequiredIndex() {
        AbstractHTTPCommandHandler handler =  createFileRegCommandHandler(serverParameters, COMMAND);
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REF_BIO_ID),
                PATH_TO_BAM), new ApplicationOptions());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileRegistrationUnsupportedFormat() {
        AbstractHTTPCommandHandler handler =  createFileRegCommandHandler(serverParameters, COMMAND);
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REF_BIO_ID),
                PATH_TO_BAM + "bz"), new ApplicationOptions());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileRegistrationWrongPath() {
        AbstractHTTPCommandHandler handler =  createFileRegCommandHandler(serverParameters, COMMAND);
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REF_BIO_ID),
                PATH_TO_BAM + "?" + PATH_TO_BAI + "?" + PATH_TO_BAI), new ApplicationOptions());
    }

    @Test
    public void testFileRegistrationNoIndex() {
        ApplicationOptions options = new ApplicationOptions();
        options.setDoIndex(false);

        AbstractHTTPCommandHandler handler = createFileRegCommandHandler(serverParameters, COMMAND);
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REF_BIO_ID),
                                                      PATH_TO_VCF), options);
        assertEquals(RUN_STATUS_OK, handler.runCommand());
    }
}
