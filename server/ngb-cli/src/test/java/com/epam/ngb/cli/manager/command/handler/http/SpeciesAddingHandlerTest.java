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

public class SpeciesAddingHandlerTest extends AbstractCliTest {

    private static final String COMMAND = "add_species";
    private static ServerParameters serverParameters;
    private static TestHttpServer server = new TestHttpServer();

    private static final String SPECIES_NAME = "human";
    private static final String SPECIES_VERSION = "hg38";

    private static final Long REF_BIO_ID = 1L;
    private static final Long REF_ID = 50L;
    private static final String REFERENCE_NAME = "hg38";
    private static final String PATH_TO_REFERENCE = "references/50";

    @BeforeClass
    public static void setUp() {
        server.start();
        server.addReference(REF_BIO_ID, REF_ID, REFERENCE_NAME, PATH_TO_REFERENCE);
        server.addSpeciesAdding(
                TestDataProvider.getBioItem(REF_ID, REF_BIO_ID, BiologicalDataItemFormat.REFERENCE,
                        PATH_TO_REFERENCE, REFERENCE_NAME),
                SPECIES_NAME,
                SPECIES_VERSION);
        serverParameters = getDefaultServerOptions(server.getPort());
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }

    @Test()
    public void testAdding() {
        SpeciesAddingHandler handler = getSpeciesAddingHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setPrintJson(true);
        handler.parseAndVerifyArguments(Arrays.asList(REFERENCE_NAME, SPECIES_VERSION), applicationOptions);
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test(expected = ApplicationException.class)
    public void testWrongReference() {
        SpeciesAddingHandler handler = getSpeciesAddingHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        handler.parseAndVerifyArguments(Arrays.asList(REFERENCE_NAME + "/", SPECIES_VERSION),
                applicationOptions);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongArguments() {
        SpeciesAddingHandler handler = getSpeciesAddingHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        handler.parseAndVerifyArguments(Collections.singletonList(REFERENCE_NAME),
                applicationOptions);
    }

    private SpeciesAddingHandler getSpeciesAddingHandler() {
        SpeciesAddingHandler handler = new SpeciesAddingHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        return handler;
    }
}
