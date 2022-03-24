/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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
import com.epam.ngb.cli.manager.command.handler.http.reference.AnnotationReferenceAddingHandler;
import com.epam.ngb.cli.manager.command.handler.http.reference.AnnotationReferenceRemovingHandler;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class AnnotationReferenceHandlersTest extends AbstractCliTest {

    private static final String COMMAND = "add_annotation";
    private static ServerParameters serverParameters;
    private static TestHttpServer server = new TestHttpServer();

    private static final Long REF_BIO_ID = 1L;
    private static final Long REF_ID = 50L;
    private static final String REFERENCE_NAME = "hg38";
    private static final String PATH_TO_REFERENCE = "references/50";

    private static final Long BED_BIO_ID = 2L;
    private static final Long BED_ID = 51L;
    private static final String BED_NAME = "example";
    private static final String PATH_TO_BED = "path/example.bed";

    private static final Long BED2_BIO_ID = 3L;
    private static final Long BED2_ID = 52L;
    private static final String BED2_NAME = "example";
    private static final String PATH_TO_BED2 = "path/example.bed";

    @BeforeClass
    public static void setUp() {
        server.start();
        server.addReference(REF_BIO_ID, REF_ID, REFERENCE_NAME, PATH_TO_REFERENCE);
        server.addFile(BED_BIO_ID, BED_ID, BED_NAME, PATH_TO_BED, BiologicalDataItemFormat.BED);
        server.addFile(BED2_BIO_ID, BED2_ID, BED2_NAME, PATH_TO_BED2, BiologicalDataItemFormat.BED);

        //adding requests
        server.addAnnotationReferenceUpdating(
                TestDataProvider.getBioItem(REF_ID, REF_BIO_ID, BiologicalDataItemFormat.REFERENCE,
                        PATH_TO_REFERENCE, REFERENCE_NAME),
                BED_BIO_ID, true);
        server.addAnnotationReferenceUpdating(
                TestDataProvider.getBioItem(REF_ID, REF_BIO_ID, BiologicalDataItemFormat.REFERENCE,
                        PATH_TO_REFERENCE, REFERENCE_NAME),
                BED2_BIO_ID, true);

        //removing requests
        server.addAnnotationReferenceUpdating(
                TestDataProvider.getBioItem(REF_ID, REF_BIO_ID, BiologicalDataItemFormat.REFERENCE,
                        PATH_TO_REFERENCE, REFERENCE_NAME),
                BED_BIO_ID, false);
        server.addAnnotationReferenceUpdating(
                TestDataProvider.getBioItem(REF_ID, REF_BIO_ID, BiologicalDataItemFormat.REFERENCE,
                        PATH_TO_REFERENCE, REFERENCE_NAME),
                BED2_BIO_ID, false);
        serverParameters = getDefaultServerOptions(server.getPort());
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }

    @Test
    public void testAddByName() {
        AnnotationReferenceAddingHandler addingHandler = getAnnotationAddingHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintJson(true);
        addingHandler.parseAndVerifyArguments(Arrays.asList(REFERENCE_NAME, BED_NAME), applicationOptions);
        Assert.assertEquals(RUN_STATUS_OK, addingHandler.runCommand());

        AnnotationReferenceRemovingHandler removingHandler = getAnnotationRemovingHandler();
        applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintJson(true);
        removingHandler.parseAndVerifyArguments(Arrays.asList(REFERENCE_NAME, BED_NAME), applicationOptions);
        Assert.assertEquals(RUN_STATUS_OK, removingHandler.runCommand());
    }

    @Test()
    public void testAddById() {
        AnnotationReferenceAddingHandler addingHandler = getAnnotationAddingHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintJson(true);
        addingHandler.parseAndVerifyArguments(
                Arrays.asList(REFERENCE_NAME, String.valueOf(BED_BIO_ID)),
                applicationOptions
        );
        Assert.assertEquals(RUN_STATUS_OK, addingHandler.runCommand());

        AnnotationReferenceRemovingHandler removingHandler = getAnnotationRemovingHandler();
        applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintJson(true);
        removingHandler.parseAndVerifyArguments(
                Arrays.asList(REFERENCE_NAME, String.valueOf(BED_BIO_ID)),
                applicationOptions
        );
        Assert.assertEquals(RUN_STATUS_OK, removingHandler.runCommand());
    }

    @Test()
    public void testAddMultipleFiles() {
        AnnotationReferenceAddingHandler addingHandler = getAnnotationAddingHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintJson(true);
        addingHandler.parseAndVerifyArguments(
                Arrays.asList(REFERENCE_NAME, String.valueOf(BED_BIO_ID), String.valueOf(BED2_BIO_ID)),
                applicationOptions
        );
        Assert.assertEquals(RUN_STATUS_OK, addingHandler.runCommand());

        AnnotationReferenceRemovingHandler removingHandler = getAnnotationRemovingHandler();
        applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintJson(true);
        removingHandler.parseAndVerifyArguments(
                Arrays.asList(REFERENCE_NAME, String.valueOf(BED_BIO_ID), String.valueOf(BED2_BIO_ID)),
                applicationOptions
        );
        Assert.assertEquals(RUN_STATUS_OK, removingHandler.runCommand());
    }

    @Test(expected = ApplicationException.class)
    public void testWrongReference() {
        try {
            //catch exception here and move on catch block for the next exception
            AnnotationReferenceAddingHandler addingHandler = getAnnotationAddingHandler();
            ApplicationOptions applicationOptions = new ApplicationOptions();
            addingHandler.parseAndVerifyArguments(Arrays.asList(REFERENCE_NAME + "/", BED_NAME),
                    applicationOptions);
        } catch (ApplicationException e) {
            AnnotationReferenceAddingHandler removingHandler = getAnnotationAddingHandler();
            ApplicationOptions applicationOptions = new ApplicationOptions();
            removingHandler.parseAndVerifyArguments(Arrays.asList(REFERENCE_NAME + "/", BED_NAME),
                    applicationOptions);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongArguments() {
        try {
            //catch exception here and move on catch block for the next exception
            AnnotationReferenceAddingHandler addingHandler = getAnnotationAddingHandler();
            ApplicationOptions applicationOptions = new ApplicationOptions();
            addingHandler.parseAndVerifyArguments(Collections.singletonList(REFERENCE_NAME), applicationOptions);
        } catch (IllegalArgumentException e) {
            AnnotationReferenceAddingHandler removingHandler = getAnnotationAddingHandler();
            ApplicationOptions applicationOptions = new ApplicationOptions();
            removingHandler.parseAndVerifyArguments(Collections.singletonList(REFERENCE_NAME), applicationOptions);
        }
    }

    private AnnotationReferenceAddingHandler getAnnotationAddingHandler() {
        AnnotationReferenceAddingHandler handler = new AnnotationReferenceAddingHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        return handler;
    }

    private AnnotationReferenceRemovingHandler getAnnotationRemovingHandler() {
        AnnotationReferenceRemovingHandler handler = new AnnotationReferenceRemovingHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        return handler;
    }
}