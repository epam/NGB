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

import java.util.Collections;

public class SortHandlerTest extends AbstractCliTest {

    private static final String COMMAND = "sort";
    public static final int INCORRECT_MAX_MEMORY_VALUE = -100;
    private static ServerParameters serverParameters;
    private static TestHttpServer server = new TestHttpServer();

    private static final String CORRECT_UNSORTED = "unsorted.bed";
    private static final String UNSUPPORTED_FORMAT_FILE = "unsupported.bam";


    @BeforeClass
    public static void setUp() {
        server.start();
        server.enableSortService();
        serverParameters = getDefaultServerOptions(server.getPort());
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }

    @Test
    public void testCorrectSort() {
        SortHandler handler = getSortHandler();
        handler.parseAndVerifyArguments(Collections.singletonList(String.valueOf(CORRECT_UNSORTED)),
                new ApplicationOptions());
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }


    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfMemoryUsageIsIncorrect() {
        SortHandler handler = getSortHandler();
        final ApplicationOptions options = new ApplicationOptions();
        options.setMaxMemory(INCORRECT_MAX_MEMORY_VALUE);
        handler.parseAndVerifyArguments(Collections.singletonList(CORRECT_UNSORTED), options);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfNoArguments() {
        SortHandler handler = getSortHandler();
        final ApplicationOptions options = new ApplicationOptions();
        handler.parseAndVerifyArguments(Collections.emptyList(), options);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfFormatIsUnsupported() {
        SortHandler handler = getSortHandler();
        final ApplicationOptions options = new ApplicationOptions();
        handler.parseAndVerifyArguments(Collections.singletonList(UNSUPPORTED_FORMAT_FILE), options);
    }


    private SortHandler getSortHandler() {
        SortHandler handler = new SortHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        return handler;
    }
}
