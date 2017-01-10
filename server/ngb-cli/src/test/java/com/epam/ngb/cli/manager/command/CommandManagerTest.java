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

package com.epam.ngb.cli.manager.command;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.epam.ngb.cli.AbstractCliTest;
import com.epam.ngb.cli.TestHttpServer;
import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.http.SearchHandler;
import com.epam.ngb.cli.manager.command.handler.simple.SetServerCommandHandler;

public class CommandManagerTest extends AbstractCliTest{

    private static final int PORT = 15666;
    private static final String SEARCH_COMMAND = "search";
    private static final String SET_SERVER_COMMAND = "set_server";
    private static final String UNKNOWN_COMMAND = "unknown";

    private static final Long REF_BIO_ID = 1L;
    private static final Long REF_ID = 50L;
    private static final String PATH_TO_REFERENCE = "reference/50";
    private static final String REFERENCE_NAME = "hg38";

    @Test
    public void testCreateHTTPCommandHandler() throws Exception {
        CommandManager manager = new CommandManager(SEARCH_COMMAND,
                getCommandConfiguration(SEARCH_COMMAND), loadServerProperties(PORT));
        Assert.assertTrue(manager.getCommandHandler() instanceof SearchHandler);
    }

    @Test
    public void testCreateSimpleCommandHandler() throws Exception {
        CommandManager manager = new CommandManager(SET_SERVER_COMMAND,
                getCommandConfiguration(SET_SERVER_COMMAND), loadServerProperties(PORT));
        Assert.assertTrue(manager.getCommandHandler() instanceof SetServerCommandHandler);
    }

    @Test(expected = ApplicationException.class)
    public void testCreateUnknownCommandHandler() throws Exception {
        CommandManager manager = new CommandManager(UNKNOWN_COMMAND,
                getCommandConfiguration(UNKNOWN_COMMAND), loadServerProperties(PORT));
        Assert.assertTrue(manager.getCommandHandler() instanceof SetServerCommandHandler);
    }

    @Test
    public void testRunCommand() {
        TestHttpServer server = new TestHttpServer();
        server.start();
        server.addReference(REF_BIO_ID, REF_ID, REFERENCE_NAME, PATH_TO_REFERENCE);
        CommandManager manager = new CommandManager(SEARCH_COMMAND,
                getCommandConfiguration(SEARCH_COMMAND), loadServerProperties(server.getPort()));
        int result = manager.run(Collections.singletonList(REFERENCE_NAME), new ApplicationOptions());
        Assert.assertEquals(RUN_STATUS_OK, result);
        server.stop();
    }

    @Test(expected = ApplicationException.class)
    public void testRunCommandServerUnavailable() {
        CommandManager manager = new CommandManager(SEARCH_COMMAND,
                getCommandConfiguration(SEARCH_COMMAND), loadServerProperties(PORT));
        manager.run(Collections.singletonList(REFERENCE_NAME), new ApplicationOptions());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunCommandWrongArguments() {
        CommandManager manager = new CommandManager(SEARCH_COMMAND,
                getCommandConfiguration(SEARCH_COMMAND), loadServerProperties(PORT));
        manager.run(Arrays.asList(REFERENCE_NAME, UNKNOWN_COMMAND), new ApplicationOptions());
    }
}
