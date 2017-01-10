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
import com.epam.ngb.cli.manager.command.ServerParameters;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static net.jadler.Jadler.*;

/**
 * Basic functionality of HttpCommandHandlers is tested on a {code {@link FileRegistrationHandler}}
 * instance
 */
public class CommonHTTPCommandHandlerTest extends AbstractCliTest {

    private static final String COMMAND = "register_file";
    private static final Long REF_ID = 10L;
    private static ServerParameters serverParameters;

    @BeforeClass
    public static void setUp() {
        initJadler();
        serverParameters = getDefaultServerOptions(port());
    }

    @AfterClass
    public static void tearDown() {
        closeJadler();
    }

    @Test
    public void getRequestType() throws Exception {
        AbstractHTTPCommandHandler handler = createFileRegCommandHandler(serverParameters, COMMAND);
        String requestUrl = String.format(handler.getRequestUrl(), REF_ID);
        HttpRequestBase post = handler.getRequest(requestUrl);
        Assert.assertTrue(post instanceof HttpPost);
        Assert.assertEquals(serverParameters.getServerUrl() + requestUrl,
                post.getURI().toString());
    }
}
