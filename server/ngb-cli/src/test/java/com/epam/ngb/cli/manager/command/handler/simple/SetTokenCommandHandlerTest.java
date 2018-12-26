package com.epam.ngb.cli.manager.command.handler.simple;

import org.junit.Assert;
import org.junit.Test;

public class SetTokenCommandHandlerTest extends SetPropertyCommandHandlerTest {

    private static final String COMMAND = "set_token";

    @Test
    public void testSetToken() throws Exception {
        SetTokenCommandHandler handler = (SetTokenCommandHandler) initCommandHandler(new SetTokenCommandHandler(),
                COMMAND, TOKEN);
        Assert.assertEquals(TOKEN, handler.getProperties().getProperty(TOKEN_PROPERTY));
    }

    @Test
    public void testSetEmptyToken() {
        SetTokenCommandHandler handler = (SetTokenCommandHandler) initCommandHandler(new SetTokenCommandHandler(),
                COMMAND, "");
        Assert.assertTrue(handler.getProperties().getProperty(TOKEN_PROPERTY).isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoArguments() throws Exception {
        initNoArgsCommandHandler(new SetTokenCommandHandler(), COMMAND);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTooManyArguments() throws Exception {
        initTooManyArgsCommandHandler(new SetTokenCommandHandler(), COMMAND, TOKEN);
    }
}