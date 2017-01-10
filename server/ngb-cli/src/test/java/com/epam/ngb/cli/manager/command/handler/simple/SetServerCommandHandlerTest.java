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

package com.epam.ngb.cli.manager.command.handler.simple;

import com.epam.ngb.cli.AbstractCliTest;
import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.exception.ApplicationException;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SetServerCommandHandlerTest extends AbstractCliTest{

    private static final String COMMAND = "set_server";
    private static final String URL = "localhost:15666";

    //this will always fail, as we cannot save configuration to an external file in test
    //as this command works only in distribution
    @Test(expected = ApplicationException.class)
    public void testSetServer() throws Exception {
        SetServerCommandHandler handler = new SetServerCommandHandler();
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        handler.parseAndVerifyArguments(Collections.singletonList(URL), new ApplicationOptions());
        handler.runCommand();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoArguments() throws Exception {
        SetServerCommandHandler handler = new SetServerCommandHandler();
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        handler.parseAndVerifyArguments(Collections.emptyList(), new ApplicationOptions());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTooManyArguments() throws Exception {
        SetServerCommandHandler handler = new SetServerCommandHandler();
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        handler.parseAndVerifyArguments(Arrays.asList(URL, URL), new ApplicationOptions());
    }
}
