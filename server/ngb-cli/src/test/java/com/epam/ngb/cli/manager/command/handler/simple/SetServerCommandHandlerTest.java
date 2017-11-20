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

import org.junit.Assert;
import org.junit.Test;

public class SetServerCommandHandlerTest extends SetPropertyCommandHandlerTest {

    private static final String COMMAND = "set_server";

    @Test
    public void testSetServer() throws Exception {
        SetServerCommandHandler handler = (SetServerCommandHandler) initCommandHandler(new SetServerCommandHandler(),
                COMMAND, SERVER_URL);
        Assert.assertEquals(SERVER_URL, handler.getProperties().getProperty(SERVER_URL_PROPERTY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoArguments() throws Exception {
        initNoArgsCommandHandler(new SetServerCommandHandler(), COMMAND);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTooManyArguments() throws Exception {
        initTooManyArgsCommandHandler(new SetServerCommandHandler(), COMMAND, SERVER_URL);
    }
}
