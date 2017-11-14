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
import com.epam.ngb.cli.TestHttpServer;
import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.manager.command.ServerParameters;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SpeciesDeletionHandlerTest extends AbstractCliTest {

    private static final String COMMAND = "delete_species";
    private static ServerParameters serverParameters;
    private static TestHttpServer server = new TestHttpServer();

    private static final String SPECIES_NAME = "human";
    private static final String SPECIES_VERSION = "hg19";

    @BeforeClass
    public static void setUp() {
        server.start();

        server.addAuthorization();
        server.addSpeciesDeletion(SPECIES_NAME, SPECIES_VERSION);
        serverParameters = getDefaultServerOptions(server.getPort());
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }

    @Test
    public void testDeletion() {
        SpeciesDeletionHandler handler = getSpeciesDeletionHandler();
        handler.parseAndVerifyArguments(Collections.singletonList(SPECIES_VERSION), new ApplicationOptions());
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongArguments() {
        SpeciesDeletionHandler handler = getSpeciesDeletionHandler();
        handler.parseAndVerifyArguments(Arrays.asList(SPECIES_NAME, SPECIES_VERSION), new ApplicationOptions());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoArguments() {
        SpeciesDeletionHandler handler = getSpeciesDeletionHandler();
        handler.parseAndVerifyArguments(Collections.emptyList(), new ApplicationOptions());
    }

    private SpeciesDeletionHandler getSpeciesDeletionHandler() {
        SpeciesDeletionHandler handler = new SpeciesDeletionHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        return handler;
    }
}
