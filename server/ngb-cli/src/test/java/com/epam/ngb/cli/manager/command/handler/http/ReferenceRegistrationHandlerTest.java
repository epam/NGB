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
import com.epam.ngb.cli.manager.command.ServerParameters;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ReferenceRegistrationHandlerTest extends AbstractCliTest {

    private static final String COMMAND = "register_reference";
    private static ServerParameters serverParameters;
    private static TestHttpServer server = new TestHttpServer();

    private static final Long REF_BIO_ID = 1L;
    private static final Long REF_ID = 50L;
    private static final String REFERENCE_NAME = "hg38";
    private static final String PATH_TO_REFERENCE = "path/ref.fa";

    @BeforeClass
    public static void setUp() {
        server.start();

        server.addAuthorization();
        server.addReferenceRegistration(REF_ID, REF_BIO_ID, PATH_TO_REFERENCE, REFERENCE_NAME);
        server.addReferenceRegistration(REF_ID, REF_BIO_ID, PATH_TO_REFERENCE, null);

        serverParameters = getDefaultServerOptions(server.getPort());
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }

    @Test
    public void testRegReferenceWithName() {
        ReferenceRegistrationHandler handler = getReferenceRegistrationHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setName(REFERENCE_NAME);
        applicationOptions.setPrintTable(true);
        handler.parseAndVerifyArguments(Collections.singletonList(PATH_TO_REFERENCE), applicationOptions);
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testRegReference() {
        ReferenceRegistrationHandler handler = getReferenceRegistrationHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        handler.parseAndVerifyArguments(Collections.singletonList(PATH_TO_REFERENCE), applicationOptions);
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }


    @Test(expected = IllegalArgumentException.class)
    public void testWrongArguments() {
        ReferenceRegistrationHandler handler = getReferenceRegistrationHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        handler.parseAndVerifyArguments(Arrays.asList(PATH_TO_REFERENCE, REFERENCE_NAME), applicationOptions);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoArguments() {
        ReferenceRegistrationHandler handler = getReferenceRegistrationHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        handler.parseAndVerifyArguments(Collections.emptyList(), applicationOptions);
    }

    private ReferenceRegistrationHandler getReferenceRegistrationHandler() {
        ReferenceRegistrationHandler handler = new ReferenceRegistrationHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        return handler;
    }
}
